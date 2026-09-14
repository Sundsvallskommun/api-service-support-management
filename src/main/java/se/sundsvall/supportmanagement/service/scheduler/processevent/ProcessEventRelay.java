package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.pwalkt.PwAlktUnavailableException;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.pwalkt.configuration.PwAlktConfiguration.CLIENT_ID;

/**
 * Takes rows out of the outbox and hands them to pw-alkt.
 * <p>
 * Started two ways that do the same thing. The scheduled run is the truth of the system and takes whatever is waiting;
 * the direct run takes the rows of one errand as soon as the transaction that wrote them is committed. The direct run
 * only brings a delivery forward: dropped, the scheduled run delivers the row within a minute, and when both reach for
 * the same row the one that comes second finds it delivered.
 * <p>
 * Rows are delivered per errand, one transaction per group, which keeps the order within an errand and lets an errand
 * whose delivery fails hold back no one but itself.
 */
@Component
public class ProcessEventRelay {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessEventRelay.class);

	private static final Sort OLDEST_FIRST = Sort.by("created", "id");

	private static final String FOREIGN_ROWS = "undelivered process events are addressed to a process consumer other than '%s', and nothing delivers them";
	private static final String STALE_BACKLOG = "the oldest undelivered process event was written at %s, which is more than %s ago";

	private final ProcessEventOutboxRepository outboxRepository;
	private final ProcessEventDelivery delivery;
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
	 * How old an undelivered row may get before it is dropped. See {@link ProcessEventDelivery#dropAgedOut}.
	 */
	@Value("${scheduler.process-event.max-age:P30D}")
	private Duration maxAge = Duration.ofDays(30);

	/**
	 * How old the oldest undelivered row may get before the relay reports itself unhealthy.
	 */
	@Value("${scheduler.process-event.unhealthy-after:PT15M}")
	private Duration unhealthyAfter = Duration.ofMinutes(15);

	public ProcessEventRelay(final ProcessEventOutboxRepository outboxRepository, final ProcessEventDelivery delivery, final Clock clock) {
		this.outboxRepository = outboxRepository;
		this.delivery = delivery;
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
		dropAgedOut();

		final var groups = outboxRepository.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(CLIENT_ID, OffsetDateTime.now(clock).minus(transactionBuffer), PageRequest.of(0, batchSize, OLDEST_FIRST)).stream()
			.collect(groupingBy(ProcessEventOutboxEntity::getErrandId, LinkedHashMap::new, mapping(ProcessEventOutboxEntity::getId, toList())));

		for (final var group : groups.entrySet()) {
			if (!deliverGroup(group.getKey(), group.getValue())) {
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
			delivery.deliverGroup(rows.stream().map(ProcessEventOutboxEntity::getId).toList());
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

		return outboxRepository.findByDeliveredAtIsNullOrderByCreatedAsc(PageRequest.of(0, 1)).stream()
			.map(ProcessEventOutboxEntity::getCreated)
			.filter(created -> created.isBefore(limit))
			.findFirst()
			.map(created -> STALE_BACKLOG.formatted(created, unhealthyAfter));
	}

	/**
	 * @return false when the circuit breaker of pw-alkt is open, which every group after this one would meet as well.
	 */
	private boolean deliverGroup(final String errandId, final List<String> rowIds) {
		try {
			delivery.deliverGroup(rowIds);
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

	private void dropAgedOut() {
		final var createdBefore = OffsetDateTime.now(clock).minus(maxAge);
		int dropped;

		do {
			dropped = delivery.dropAgedOut(createdBefore, batchSize);
		} while (dropped == batchSize);
	}
}
