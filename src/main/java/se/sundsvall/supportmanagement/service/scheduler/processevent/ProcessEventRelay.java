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
 * Started two ways that do the same thing. The scheduled run is the truth of the system and takes whatever is waiting;
 * the direct run takes the rows of one errand as soon as the transaction that wrote them is committed. The direct run
 * only brings a delivery forward: dropped, the scheduled run delivers the row within a minute, and when both reach for
 * the same row the one that comes second finds it delivered.
 * <p>
 * Rows are delivered per errand, one transaction per group, which keeps the order within an errand and lets an errand
 * whose delivery fails hold back no one but itself.
 * <p>
 * The pattern is the one {@code V1_48__simplify_notification_dispatch} gave the notification dispatch: no retry
 * counter, no backoff and no dead letter to keep in step. A delivery that does not go through takes its transaction
 * down, the rows stay exactly as they were, and the next run tries again - an undelivered row is its own receipt that
 * the work remains.
 * <p>
 * Two things follow. pw-alkt has to take an event it has been given before, since a transaction rolled back after
 * pw-alkt took one row gives it that row again. And a group is delivered in full or not at all, which is what keeps
 * the order within an errand when something fails.
 * <p>
 * The transactions are opened through a {@link TransactionTemplate} rather than {@code @Transactional}, since they are
 * opened from inside the class, where no proxy would see the call. The template has to be given both settings itself:
 * a transaction of its own for every group, and read committed isolation, which keeps the locking read of a group from
 * also locking the gaps a publication writes its new rows into. Left out, neither fails anything - the isolation of
 * the database takes over, and errand writes wait for pw-alkt to answer.
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
	 * How many rows one run takes at most. The fetch has a limit so that a backlog cannot make a single run unbounded.
	 */
	@Value("${scheduler.process-event.batch-size:200}")
	private int batchSize = 200;

	/**
	 * How old a row must be before the scheduled run takes it. Without it the run could reach past a transaction that is
	 * still being committed and deliver a later row of an errand before an earlier one.
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
	 * Drops what has aged out first, so that nothing the relay has given up on is delivered after all. Then takes the
	 * oldest rows, at most a batch of them, and delivers them errand by errand. An errand that fails is left for the next
	 * run and the others go on.
	 * <p>
	 * An open circuit breaker ends the run, since every errand after it would meet the same answer. That holds because the
	 * breaker counts only calls that never got an answer. Were a refusal of a single event counted as well, the few events
	 * pw-alkt never takes - which are read first again every run - would keep the breaker open for every other errand.
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
	 * Not held to the transaction buffer, since it starts only once its own transaction is committed. It is held to the
	 * age limit: a row that has aged out is the scheduled run's to drop.
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
	 * Measured on the age of the oldest undelivered row, not on whether there is one. Every publication leaves a row
	 * behind until the next run takes it, so a condition on existence would report the relay unhealthy during normal
	 * operation - and an indicator that is always red is one nobody looks at.
	 * <p>
	 * A row addressed to anything but pw-alkt is reported at once, since no run will ever take it.
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
	 * @return false when the circuit breaker of pw-alkt is open, which every group after this one would meet as well.
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
	 * The rows are read again under a write lock, keeping only those still undelivered. That is what makes the direct
	 * run and the scheduled run harmless to each other: whichever reaches a row second waits for the first and then
	 * finds it delivered. Waiting is on purpose - skipping the locked rows instead would let a later row of the errand
	 * past an earlier one still being delivered.
	 * <p>
	 * A refusal for good is recorded once every call in the group has been made, so that the lock it takes on the errand
	 * is held for the bookkeeping and not across calls to pw-alkt.
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
	 * Drops undelivered rows that have aged past the last resort, oldest first and at most a batch at a time, in a
	 * transaction of its own.
	 * <p>
	 * A dropped row means a process never got to know something, so each one is logged as an error naming what it takes
	 * to find the errand and the event again. It should never happen.
	 * <p>
	 * The rows are found without a lock and then locked by id, the same way a delivery locks them, so that asking for aged
	 * rows never waits for a delivery of rows that have not aged at all.
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
	 * Sorted here rather than by the locking query, which then has nothing to gain from walking the index of undelivered
	 * rows instead of the primary key. Walked that way, a locking read would also lock the gap a publication writes its new
	 * row into, and hold every errand write back until pw-alkt had answered.
	 */
	private static List<ProcessEventOutboxEntity> oldestFirst(final List<ProcessEventOutboxEntity> rows) {
		return rows.stream()
			.sorted(comparing(ProcessEventOutboxEntity::getCreated).thenComparing(ProcessEventOutboxEntity::getId))
			.toList();
	}

	/**
	 * Puts a refusal for good in the history of the errand rather than in a flag on a row. The refusal itself is logged
	 * where the answer of pw-alkt is read.
	 * <p>
	 * The live instance of the errand is failed, if it has one. Usually it has none - a key that was never deployed never
	 * started anything - and then only the entry is written, without an instance. A mistyped key refuses every event of
	 * the errand, which is why the entry is written once per errand and window.
	 * <p>
	 * The errand is locked first, which is how the reports of a process are serialised against each other, since the
	 * instance would otherwise be failed underneath a report being written. A deletion, or an errand that is gone,
	 * leaves nothing to write on, and the log is all there is.
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
