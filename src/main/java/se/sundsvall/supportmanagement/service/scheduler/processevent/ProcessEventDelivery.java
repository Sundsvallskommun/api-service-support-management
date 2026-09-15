package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
import static org.springframework.transaction.annotation.Isolation.READ_COMMITTED;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.service.mapper.ProcessEventMapper.toErrandEvent;

/**
 * Delivers rows of the outbox, and acknowledges them in the same transaction.
 * <p>
 * The pattern {@code V1_48__simplify_notification_dispatch} gave the notification dispatch: no retry counter, no
 * backoff and no dead letter to keep in step. A delivery that does not go through takes its transaction down, the rows
 * stay exactly as they were, and the next run tries again - an undelivered row is its own receipt that the work
 * remains.
 * <p>
 * Two things follow. pw-alkt has to take an event it has been given before, since a transaction rolled back after
 * pw-alkt took one row gives it that row again. And a group is delivered in full or not at all, which is what keeps
 * the order within an errand when something fails.
 */
@Component
public class ProcessEventDelivery {

	static final String REJECTION_ACTIVITY_TYPE = "DELIVERY";
	static final String REJECTION_ERROR_CODE = "PROCESS_KEY_NOT_DEPLOYED";

	private static final Logger LOG = LoggerFactory.getLogger(ProcessEventDelivery.class);

	private static final String REJECTED = """
		%s refused an event on this errand for good, since it has no process deployed under the key '%s'. The event is \
		not delivered again, and every later event on the errand is refused the same way until the key is one the \
		process engine knows. Deploy the process under that key, or correct the processKey attribute of the label""";

	private final ProcessEventOutboxRepository outboxRepository;
	private final ErrandsRepository errandsRepository;
	private final ErrandProcessRepository processRepository;
	private final PwAlktIntegration pwAlktIntegration;
	private final ProcessErrorLog errorLog;
	private final Clock clock;

	public ProcessEventDelivery(
		final ProcessEventOutboxRepository outboxRepository,
		final ErrandsRepository errandsRepository,
		final ErrandProcessRepository processRepository,
		final PwAlktIntegration pwAlktIntegration,
		final ProcessErrorLog errorLog,
		final Clock clock) {

		this.outboxRepository = outboxRepository;
		this.errandsRepository = errandsRepository;
		this.processRepository = processRepository;
		this.pwAlktIntegration = pwAlktIntegration;
		this.errorLog = errorLog;
		this.clock = clock;
	}

	/**
	 * Delivers one group of rows, all for the same errand, oldest first.
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
	 * @throws PwAlktUnavailableException when a row does not go through, which leaves every row of the group as it was.
	 */
	@Transactional(propagation = REQUIRES_NEW, isolation = READ_COMMITTED)
	public void deliverGroup(final Collection<String> rowIds) {
		final var deliveredAt = OffsetDateTime.now(clock).truncatedTo(MILLIS);
		final var refused = new ArrayList<ProcessEventOutboxEntity>();

		for (final var row : oldestFirst(outboxRepository.findByIdInAndDeliveredAtIsNull(rowIds))) {
			if (!pwAlktIntegration.sendErrandEvent(row.getMunicipalityId(), row.getNamespace(), toErrandEvent(row))) {
				refused.add(row);
			}
			row.setDeliveredAt(deliveredAt);
		}

		refused.forEach(this::recordRejection);
	}

	/**
	 * Drops undelivered rows that have aged past the last resort, oldest first and at most a batch at a time.
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
	@Transactional(propagation = REQUIRES_NEW, isolation = READ_COMMITTED)
	public int dropAgedOut(final OffsetDateTime createdBefore, final int limit) {
		final var agedOut = outboxRepository.findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(createdBefore, PageRequest.of(0, limit));

		if (agedOut.isEmpty()) {
			return 0;
		}

		final var rows = outboxRepository.findByIdInAndDeliveredAtIsNull(agedOut.stream().map(ProcessEventOutboxEntity::getId).toList());

		rows.forEach(row -> LOG.error("Dropping process event {} for errand {} undelivered, written at {}: {} was never told about this {} {}",
			row.getId(), row.getErrandId(), row.getCreated(), row.getProcessService(), row.getEventType(), row.getEventSubType()));

		outboxRepository.deleteAllByIdInBatch(rows.stream().map(ProcessEventOutboxEntity::getId).toList());

		return rows.size();
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
