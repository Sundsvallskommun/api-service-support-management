package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

/**
 * Removes the delivered rows of the outbox that nothing needs any more, and leaves the undelivered ones alone.
 * <p>
 * An undelivered row is never removed here. It stays until it has been delivered or has aged out, and only the relay
 * decides either.
 */
@Component
public class ProcessEventCleanup {

	private static final Duration SHORTEST_RETENTION = Duration.ofDays(1);
	private static final int WINDOWS_KEPT = 6;

	private final ProcessEventOutboxRepository outboxRepository;
	private final ProcessEngineProperties processEngineProperties;
	private final Clock clock;

	/**
	 * How many rows one delete removes. Each batch is a transaction of its own, so a large backlog is never one large
	 * transaction.
	 */
	@Value("${scheduler.process-cleanup.batch-size:1000}")
	private int batchSize = 1000;

	public ProcessEventCleanup(final ProcessEventOutboxRepository outboxRepository, final ProcessEngineProperties processEngineProperties, final Clock clock) {
		this.outboxRepository = outboxRepository;
		this.processEngineProperties = processEngineProperties;
		this.clock = clock;
	}

	/**
	 * Removes the rows delivered longer ago than the retention.
	 *
	 * @return how many rows were removed.
	 */
	public int removeDelivered() {
		final var deliveredBefore = OffsetDateTime.now(clock).minus(retention());
		var removed = 0;
		List<String> batch;

		do {
			batch = outboxRepository.findByDeliveredAtBefore(deliveredBefore, PageRequest.of(0, batchSize)).stream()
				.map(ProcessEventOutboxEntity::getId)
				.toList();
			outboxRepository.deleteAllByIdInBatch(batch);
			removed += batch.size();
		} while (batch.size() == batchSize);

		return removed;
	}

	/**
	 * Six windows of the emergency brake, and a day at the least. The brake counts delivered rows inside its window, so a
	 * row has to outlive the window, and the day leaves room to see what was delivered when something has gone wrong.
	 */
	Duration retention() {
		final var windows = processEngineProperties.loopGuard().window().multipliedBy(WINDOWS_KEPT);

		return windows.compareTo(SHORTEST_RETENTION) > 0 ? windows : SHORTEST_RETENTION;
	}
}
