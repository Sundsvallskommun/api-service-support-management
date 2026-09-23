package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.supportmanagement.config.ProcessEventRelayProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktIntegration;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktUnavailableException;
import se.sundsvall.supportmanagement.service.ProcessActivityLog;

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
import static se.sundsvall.supportmanagement.service.ProcessActivityLog.DELIVERY_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.mapper.ProcessEventMapper.toErrandEvent;

/**
 * Takes rows out of the outbox, hands them to pw-alkt, and acknowledges them in the same transaction.
 * <p>
 * Started two ways that do the same thing. The scheduled run takes whatever is waiting; the direct run takes the rows
 * of one errand as soon as the transaction that wrote them is committed. A row the direct run does not deliver is
 * delivered by the scheduled run, and when both reach for the same row the one that comes second finds it delivered.
 * <p>
 * Rows are delivered per errand, oldest first, one transaction per group. A group is delivered up to the first row that
 * does not go through: the rows before it are acknowledged, and that row and every later one of the errand stay
 * exactly as they were for the next run to try again. The scheduled run then leaves the errand for the rest of the run
 * and goes on with rows of other errands, so an errand whose delivery fails holds back no other errand. There is no
 * retry counter, no backoff and no dead letter.
 * <p>
 * pw-alkt can be given the same event more than once, since a transaction that fails to commit after pw-alkt took a row
 * gives it that row again.
 * <p>
 * Every group gets a transaction of its own with read committed isolation, which keeps the locking read of a group
 * from also locking the gaps new outbox rows are written into.
 */
@Component
public class ProcessEventRelay {

	static final String REJECTION_ERROR_CODE = "PROCESS_KEY_NOT_DEPLOYED";

	private static final Logger LOG = LoggerFactory.getLogger(ProcessEventRelay.class);

	private static final Sort OLDEST_FIRST = Sort.by("created", "id");

	private static final String FOREIGN_ROWS = "undelivered process events are addressed to a process consumer other than '%s', and nothing delivers them";
	private static final String STALE_BACKLOG = "the oldest undelivered process event was written at %s, which is more than %s ago";
	private static final String REJECTED = """
		%s refused an event on this errand for good, since it has no process deployed under the key '%s'. The event is \
		not delivered again, and every later event on the errand is refused the same way until the key is one the \
		process engine knows. Deploy the process under that key, or correct the processKey attribute of the label""";
	private static final String REJECTED_FOR_ITS_PROCESS = """
		%s refused an event on this errand for good, since it has no process deployed under the key '%s'. The event is \
		not delivered again, and every later event on the errand is refused the same way until the key is one the \
		process engine knows. The errand runs that process for the whole of its life, so correcting the label does not \
		help: deploy the process under that key""";

	private final ProcessEventOutboxRepository outboxRepository;
	private final ErrandsRepository errandsRepository;
	private final ErrandProcessRepository processRepository;
	private final PwAlktIntegration pwAlktIntegration;
	private final ProcessActivityLog activityLog;
	private final ProcessEventRelayProperties properties;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public ProcessEventRelay(
		final ProcessEventOutboxRepository outboxRepository,
		final ErrandsRepository errandsRepository,
		final ErrandProcessRepository processRepository,
		final PwAlktIntegration pwAlktIntegration,
		final ProcessActivityLog activityLog,
		final ProcessEventRelayProperties properties,
		final PlatformTransactionManager transactionManager,
		final Clock clock) {

		this.outboxRepository = outboxRepository;
		this.errandsRepository = errandsRepository;
		this.processRepository = processRepository;
		this.pwAlktIntegration = pwAlktIntegration;
		this.activityLog = activityLog;
		this.properties = properties;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.transactionTemplate.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
		this.transactionTemplate.setIsolationLevel(ISOLATION_READ_COMMITTED);
		this.clock = clock;
	}

	/**
	 * The scheduled run.
	 * <p>
	 * Drops what has aged out first. Then takes the oldest rows and delivers them errand by errand, until a batch of rows
	 * has been tried or none is left. A group delivered counts its rows, and a group that fails counts as one row: it
	 * stopped at its first row that did not go through. An errand that fails is left for the next run, and the rows
	 * fetched after it belong to the other errands, so rows that never go through cannot fill the batch. An open circuit
	 * breaker of pw-alkt ends the run.
	 */
	public void relay() {
		dropAllAgedOut();

		final var createdBefore = OffsetDateTime.now(clock).minus(properties.transactionBuffer());
		final var failed = new HashSet<String>();
		var tried = 0;

		while (tried < properties.batchSize()) {
			final var limit = properties.batchSize() - tried;
			final var rows = oldestUndelivered(createdBefore, failed, limit);

			final var groups = rows.stream()
				.collect(groupingBy(ProcessEventOutboxEntity::getErrandId, LinkedHashMap::new, mapping(ProcessEventOutboxEntity::getId, toList())));

			for (final var group : groups.entrySet()) {
				switch (deliverOrLeaveForNextRun(group.getKey(), group.getValue())) {
					case CIRCUIT_OPEN -> {
						LOG.warn("Leaving the rest of the run to the next one, since the circuit breaker of pw-alkt is open");
						return;
					}
					case LEFT_FOR_NEXT_RUN -> {
						failed.add(group.getKey());
						tried++;
					}
					case DELIVERED -> tried += group.getValue().size();
				}
			}

			if (rows.size() < limit) {
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
	 * @throws PwAlktUnavailableException when a row did not reach pw-alkt, which leaves it and the later rows of the
	 *                                    errand for the scheduled run.
	 */
	public void relayErrand(final String errandId) {
		final var rows = outboxRepository.findByProcessServiceAndErrandIdAndDeliveredAtIsNullAndCreatedAfterOrderByCreatedAscIdAsc(CLIENT_ID, errandId,
			OffsetDateTime.now(clock).minus(properties.maxAge()), PageRequest.of(0, properties.batchSize()));

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

		final var limit = OffsetDateTime.now(clock).minus(properties.unhealthyAfter());

		return outboxRepository.findFirstByDeliveredAtIsNullOrderByCreatedAsc()
			.map(ProcessEventOutboxEntity::getCreated)
			.filter(created -> created.isBefore(limit))
			.map(created -> STALE_BACKLOG.formatted(created, properties.unhealthyAfter()));
	}

	/**
	 * The oldest undelivered rows for pw-alkt, leaving out the errands that failed earlier in the run.
	 */
	private List<ProcessEventOutboxEntity> oldestUndelivered(final OffsetDateTime createdBefore, final Set<String> failed, final int limit) {
		final var page = PageRequest.of(0, limit, OLDEST_FIRST);

		return failed.isEmpty()
			? outboxRepository.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(CLIENT_ID, createdBefore, page)
			: outboxRepository.findByProcessServiceAndDeliveredAtIsNullAndCreatedBeforeAndErrandIdNotIn(CLIENT_ID, createdBefore, failed, page);
	}

	/**
	 * Delivers a group for the scheduled run, which leaves a group that fails for the next run and goes on with the rest.
	 *
	 * @return how the delivery went.
	 */
	private Delivery deliverOrLeaveForNextRun(final String errandId, final List<String> rowIds) {
		try {
			deliverGroup(rowIds);
			return Delivery.DELIVERED;
		} catch (final PwAlktUnavailableException e) {
			if (e.isCircuitOpen()) {
				return Delivery.CIRCUIT_OPEN;
			}
			LOG.warn("Events for errand {} did not reach pw-alkt, and are left for the next run: {}", sanitizeForLogging(errandId), e.getMessage());
		} catch (final Exception e) {
			LOG.error("Failed to deliver {} events for errand {}, and they are left for the next run", rowIds.size(), sanitizeForLogging(errandId), e);
		}

		return Delivery.LEFT_FOR_NEXT_RUN;
	}

	/**
	 * Delivers one group of rows, all for the same errand, oldest first, in a transaction of its own.
	 * <p>
	 * The rows are read again under a write lock, keeping only those still undelivered: whichever run reaches a row
	 * second waits for the first and then finds it delivered.
	 * <p>
	 * A row pw-alkt refuses for good is acknowledged as well. The refusals are recorded once every call in the group has
	 * been made, so the lock that recording takes on the errand is not held across calls to pw-alkt.
	 * <p>
	 * The first row that does not go through ends the group. The rows before it are acknowledged when the transaction is
	 * committed, and the failure is handed on once it has been.
	 *
	 * @param  rowIds                     the rows to deliver.
	 * @throws PwAlktUnavailableException when a row does not go through, which leaves it and the later rows of the group
	 *                                    as they were.
	 */
	private void deliverGroup(final Collection<String> rowIds) {
		final var failure = transactionTemplate.execute(_ -> {
			final var deliveredAt = OffsetDateTime.now(clock).truncatedTo(MILLIS);
			final var refused = new ArrayList<ProcessEventOutboxEntity>();
			PwAlktUnavailableException notDelivered = null;

			for (final var row : oldestFirst(outboxRepository.findByIdInAndDeliveredAtIsNull(rowIds))) {
				try {
					if (!pwAlktIntegration.sendErrandEvent(row.getMunicipalityId(), row.getNamespace(), toErrandEvent(row))) {
						refused.add(row);
					}
				} catch (final PwAlktUnavailableException e) {
					notDelivered = e;
					break;
				}
				row.setDeliveredAt(deliveredAt);
			}

			refused.forEach(this::recordRejection);
			return notDelivered;
		});

		if (nonNull(failure)) {
			throw failure;
		}
	}

	private void dropAllAgedOut() {
		final var createdBefore = OffsetDateTime.now(clock).minus(properties.maxAge());
		int dropped;

		do {
			dropped = dropAgedOut(createdBefore, properties.batchSize());
		} while (dropped == properties.batchSize());
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
	 * only the history entry is written. The message advises correcting the label only for an errand that has no process
	 * row, since the key of a process row is what every later event of the errand carries.
	 * <p>
	 * The errand is locked first, which serialises this against the other reports of the process. Nothing is written
	 * for a deletion or for an errand that no longer exists.
	 */
	private void recordRejection(final ProcessEventOutboxEntity row) {
		if (DELETE.getValue().equals(row.getEventType())
			|| !errandsRepository.existsWithLockingByIdAndNamespaceAndMunicipalityId(row.getErrandId(), row.getNamespace(), row.getMunicipalityId())) {
			return;
		}

		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(row.getErrandId());
		final var message = (instances.isEmpty() ? REJECTED : REJECTED_FOR_ITS_PROCESS).formatted(row.getProcessService(), row.getProcessKey());
		final var instance = instances.stream().filter(ErrandProcessEntity::isLive).findFirst().orElse(null);

		if (nonNull(instance)) {
			instance.applyStatus(FAILED, clock);
			instance.setErrorCode(REJECTION_ERROR_CODE);
			instance.setErrorMessage(message);
		}

		activityLog.writeOncePerWindow(row.getErrandId(), ofNullable(instance).map(ErrandProcessEntity::getId).orElse(null), DELIVERY_ACTIVITY_TYPE, REJECTION_ERROR_CODE, message);
	}

	/**
	 * How the delivery of a group went, as the scheduled run needs to know it.
	 */
	private enum Delivery {
		DELIVERED,
		LEFT_FOR_NEXT_RUN,
		CIRCUIT_OPEN
	}
}
