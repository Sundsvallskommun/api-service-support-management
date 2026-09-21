package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktIntegration;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktUnavailableException;
import se.sundsvall.supportmanagement.service.ProcessErrorLog;

import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.pwalkt.configuration.PwAlktConfiguration.CLIENT_ID;
import static se.sundsvall.supportmanagement.service.mapper.ProcessEventMapper.toErrandEvent;

/**
 * Takes rows out of the outbox, hands them to pw-alkt, and acknowledges them in the same transaction.
 * <p>
 * Started two ways that do the same thing. The scheduled run takes whatever is waiting; the direct run takes the rows
 * of one errand as soon as the transaction that wrote them is committed. A row the direct run does not deliver is
 * delivered by the scheduled run, and when both reach for the same row the one that comes second finds it delivered.
 * <p>
 * Rows are delivered per errand, oldest first, one transaction per group, so an errand whose delivery fails holds back
 * no other errand. A group is delivered in full or not at all: a delivery that does not go through rolls its
 * transaction back, the rows stay exactly as they were, and the next run tries again. There is no retry counter, no
 * backoff and no dead letter.
 * <p>
 * pw-alkt can be given the same event more than once, since a transaction rolled back after pw-alkt took one row gives
 * it that row again.
 * <p>
 * Every group gets a transaction of its own with read committed isolation, which keeps the locking read of a group
 * from also locking the gaps new outbox rows are written into.
 */
@Component
public class ProcessEventRelay {

	static final String REJECTION_ACTIVITY_TYPE = "DELIVERY";
	static final String REJECTION_ERROR_CODE = "PROCESS_KEY_NOT_DEPLOYED";

	private static final Logger LOG = LoggerFactory.getLogger(ProcessEventRelay.class);

	private static final Sort OLDEST_FIRST = Sort.by("created", "id");

	private static final String FOREIGN_ROWS = "undelivered process events are addressed to a process consumer other than '%s', and nothing delivers them";
	private static final String STALE_BACKLOG = "the oldest undelivered process event was written at %s, which is more than %s ago";
	private static final String REJECTED = """
		%s refused an event on this errand for good, since it has no process deployed under the key '%s'. The event is \
		not delivered again, and every later event on the errand is refused the same way until the key is one the \
		process engine knows. Deploy the process under that key, or correct the processKey attribute of the label""";

	private final ProcessEventOutboxRepository outboxRepository;
	private final ErrandsRepository errandsRepository;
	private final ErrandProcessRepository processRepository;
	private final PwAlktIntegration pwAlktIntegration;
	private final ProcessErrorLog errorLog;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	/**
	 * How many rows one run takes at most.
	 */
	@Value("${scheduler.process-event.batch-size:200}")
	private int batchSize = 200;

	/**
	 * How old a row must be before the scheduled run takes it. Gives transactions still being committed time to finish,
	 * so that the rows of an errand are delivered in order.
	 */
	@Value("${scheduler.process-event.transaction-buffer:PT5S}")
	private Duration transactionBuffer = Duration.ofSeconds(5);

	/**
	 * How old an undelivered row may get before it is dropped. See {@link #dropAgedOut(OffsetDateTime, int)}.
	 */
	@Value("${scheduler.process-event.max-age:P30D}")
	private Duration maxAge = Duration.ofDays(30);

	/**
	 * How old the oldest undelivered row may get before the relay reports itself unhealthy.
	 */
	@Value("${scheduler.process-event.unhealthy-after:PT15M}")
	private Duration unhealthyAfter = Duration.ofMinutes(15);

	public ProcessEventRelay(
		final ProcessEventOutboxRepository outboxRepository,
		final ErrandsRepository errandsRepository,
		final ErrandProcessRepository processRepository,
		final PwAlktIntegration pwAlktIntegration,
		final ProcessErrorLog errorLog,
		final PlatformTransactionManager transactionManager,
		final Clock clock) {

		this.outboxRepository = outboxRepository;
		this.errandsRepository = errandsRepository;
		this.processRepository = processRepository;
		this.pwAlktIntegration = pwAlktIntegration;
		this.errorLog = errorLog;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.transactionTemplate.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
		this.transactionTemplate.setIsolationLevel(ISOLATION_READ_COMMITTED);
		this.clock = clock;
	}

	/**
	 * The scheduled run.
	 * <p>
	 * Drops what has aged out first. Then takes the oldest rows, at most a batch of them, and delivers them errand by
	 * errand. An errand that fails is left for the next run and the others go on. An open circuit breaker of pw-alkt ends
	 * the run.
	 */
	public void relay() {
		dropAllAgedOut();

		final var groups = outboxRepository.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(CLIENT_ID, OffsetDateTime.now(clock).minus(transactionBuffer), PageRequest.of(0, batchSize, OLDEST_FIRST)).stream()
			.collect(groupingBy(ProcessEventOutboxEntity::getErrandId, LinkedHashMap::new, mapping(ProcessEventOutboxEntity::getId, toList())));

		for (final var group : groups.entrySet()) {
			if (!deliverOrLeaveForNextRun(group.getKey(), group.getValue())) {
				LOG.warn("Leaving the rest of the run to the next one, since the circuit breaker of pw-alkt is open");
				return;
			}
		}
	}

	/**
	 * The direct run, for the rows of one errand.
	 * <p>
	 * Called once the transaction that wrote the rows is committed, and takes them without waiting for the transaction
	 * buffer. Only rows within the age limit are taken; a row that has aged out is left for the scheduled run to drop.
	 *
	 * @param  errandId                   the errand whose rows were just written.
	 * @throws PwAlktUnavailableException when the rows did not reach pw-alkt, which leaves them for the scheduled run.
	 */
	public void relayErrand(final String errandId) {
		final var rows = outboxRepository.findByProcessServiceAndErrandIdAndDeliveredAtIsNullAndCreatedAfterOrderByCreatedAscIdAsc(CLIENT_ID, errandId, OffsetDateTime.now(clock).minus(maxAge), PageRequest.of(0, batchSize));

		if (!rows.isEmpty()) {
			deliverGroup(rows.stream().map(ProcessEventOutboxEntity::getId).toList());
		}
	}

	/**
	 * What is wrong with the relay, if anything.
	 * <p>
	 * Measured on the age of the oldest undelivered row: the relay is unhealthy once that row is older than the
	 * configured limit. An undelivered row addressed to anything but pw-alkt, which no run takes, is reported at once.
	 *
	 * @return the fault, or empty when the relay is healthy.
	 */
	public Optional<String> findHealthFault() {
		if (outboxRepository.existsByDeliveredAtIsNullAndProcessServiceNot(CLIENT_ID)) {
			return Optional.of(FOREIGN_ROWS.formatted(CLIENT_ID));
		}

		final var limit = OffsetDateTime.now(clock).minus(unhealthyAfter);

		return outboxRepository.findFirstByDeliveredAtIsNullOrderByCreatedAsc()
			.map(ProcessEventOutboxEntity::getCreated)
			.filter(created -> created.isBefore(limit))
			.map(created -> STALE_BACKLOG.formatted(created, unhealthyAfter));
	}

	/**
	 * Delivers a group for the scheduled run, which leaves a group that fails for the next run and goes on with the rest.
	 *
	 * @return false when the circuit breaker of pw-alkt is open, true otherwise.
	 */
	private boolean deliverOrLeaveForNextRun(final String errandId, final List<String> rowIds) {
		try {
			deliverGroup(rowIds);
		} catch (final PwAlktUnavailableException e) {
			if (e.isCircuitOpen()) {
				return false;
			}
			LOG.warn("{} events for errand {} did not reach pw-alkt, and are left for the next run: {}", rowIds.size(), sanitizeForLogging(errandId), e.getMessage());
		} catch (final Exception e) {
			LOG.error("Failed to deliver {} events for errand {}, and they are left for the next run", rowIds.size(), sanitizeForLogging(errandId), e);
		}

		return true;
	}

	/**
	 * Delivers one group of rows, all for the same errand, oldest first, in a transaction of its own.
	 * <p>
	 * The rows are read again under a write lock, keeping only those still undelivered: whichever run reaches a row
	 * second waits for the first and then finds it delivered.
	 * <p>
	 * A row pw-alkt refuses for good is acknowledged as well. The refusals are recorded once every call in the group has
	 * been made, so the lock that recording takes on the errand is not held across calls to pw-alkt.
	 *
	 * @param  rowIds                     the rows to deliver.
	 * @throws PwAlktUnavailableException when a row does not go through, which rolls the transaction back and leaves every
	 *                                    row of the group as it was.
	 */
	private void deliverGroup(final Collection<String> rowIds) {
		transactionTemplate.executeWithoutResult(_ -> {
			final var deliveredAt = OffsetDateTime.now(clock).truncatedTo(MILLIS);
			final var refused = new ArrayList<ProcessEventOutboxEntity>();

			for (final var row : oldestFirst(outboxRepository.findByIdInAndDeliveredAtIsNull(rowIds))) {
				if (!pwAlktIntegration.sendErrandEvent(row.getMunicipalityId(), row.getNamespace(), toErrandEvent(row))) {
					refused.add(row);
				}
				row.setDeliveredAt(deliveredAt);
			}

			refused.forEach(this::recordRejection);
		});
	}

	private void dropAllAgedOut() {
		final var createdBefore = OffsetDateTime.now(clock).minus(maxAge);
		int dropped;

		do {
			dropped = dropAgedOut(createdBefore, batchSize);
		} while (dropped == batchSize);
	}

	/**
	 * Drops undelivered rows that have aged out, oldest first and at most a batch at a time, in a transaction of its own.
	 * <p>
	 * Each dropped row is logged as an error naming the errand and the event.
	 * <p>
	 * The rows are found without a lock and then locked by id, the same way a delivery locks them, so that asking for aged
	 * rows never waits for a delivery of rows that have not aged.
	 *
	 * @param  createdBefore the moment a row has to predate to be dropped.
	 * @param  limit         the most rows to drop in one go.
	 * @return               how many rows were dropped.
	 */
	private int dropAgedOut(final OffsetDateTime createdBefore, final int limit) {
		return ofNullable(transactionTemplate.execute(_ -> {
			final var agedOut = outboxRepository.findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(createdBefore, PageRequest.of(0, limit));

			if (agedOut.isEmpty()) {
				return 0;
			}

			final var rows = outboxRepository.findByIdInAndDeliveredAtIsNull(agedOut.stream().map(ProcessEventOutboxEntity::getId).toList());

			rows.forEach(row -> LOG.error("Dropping process event {} for errand {} undelivered, written at {}: {} was never told about this {} {}",
				row.getId(), row.getErrandId(), row.getCreated(), row.getProcessService(), row.getEventType(), row.getEventSubType()));

			outboxRepository.deleteAllByIdInBatch(rows.stream().map(ProcessEventOutboxEntity::getId).toList());

			return rows.size();
		})).orElse(0);
	}

	/**
	 * Sorts the rows oldest first, by creation time and then by id.
	 */
	private static List<ProcessEventOutboxEntity> oldestFirst(final List<ProcessEventOutboxEntity> rows) {
		// Not in the locking query: ordered by the index of undelivered rows it would also lock the gap new rows go into.
		return rows.stream()
			.sorted(comparing(ProcessEventOutboxEntity::getCreated).thenComparing(ProcessEventOutboxEntity::getId))
			.toList();
	}

	/**
	 * Records a refusal for good in the process history of the errand, at most once per errand and window.
	 * <p>
	 * The live process instance of the errand, if it has one, is failed with the error code and message; without one,
	 * only the history entry is written.
	 * <p>
	 * The errand is locked first, which serialises this against the other reports of the process. Nothing is written
	 * for a deletion or for an errand that no longer exists.
	 */
	private void recordRejection(final ProcessEventOutboxEntity row) {
		if (DELETE.getValue().equals(row.getEventType())
			|| !errandsRepository.existsWithLockingByIdAndNamespaceAndMunicipalityId(row.getErrandId(), row.getNamespace(), row.getMunicipalityId())) {
			return;
		}

		final var message = REJECTED.formatted(row.getProcessService(), row.getProcessKey());
		final var instance = processRepository.findByErrandIdAndActiveMarkerIsNotNull(row.getErrandId()).orElse(null);

		if (nonNull(instance)) {
			instance.applyStatus(FAILED, clock);
			instance.setErrorCode(REJECTION_ERROR_CODE);
			instance.setErrorMessage(message);
		}

		errorLog.writeOncePerWindow(row.getErrandId(), ofNullable(instance).map(ErrandProcessEntity::getId).orElse(null), REJECTION_ACTIVITY_TYPE, REJECTION_ERROR_CODE, message);
	}
}
