package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.config.ProcessEventCleanupProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

/**
 * Removes what the process integration no longer has to keep: the delivered rows of the outbox, and the entries of the
 * activity log that have outlived their retention.
 * <p>
 * An undelivered row is never removed here. It stays until it has been delivered or has aged out, and only the relay
 * decides either.
 */
@Component
public class ProcessEventCleanup {

	private static final Duration SHORTEST_RETENTION = Duration.ofDays(1);
	private static final int WINDOWS_KEPT = 6;

	private final ProcessEventOutboxRepository outboxRepository;
	private final ErrandProcessActivityRepository activityRepository;
	private final ProcessEngineProperties processEngineProperties;
	private final ProcessEventCleanupProperties properties;
	private final Clock clock;

	public ProcessEventCleanup(final ProcessEventOutboxRepository outboxRepository, final ErrandProcessActivityRepository activityRepository, final ProcessEngineProperties processEngineProperties,
		final ProcessEventCleanupProperties properties, final Clock clock) {
		this.outboxRepository = outboxRepository;
		this.activityRepository = activityRepository;
		this.processEngineProperties = processEngineProperties;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Removes the rows delivered longer ago than the retention.
	 *
	 * @return how many rows were removed.
	 */
	public int removeDelivered() {
		final var deliveredBefore = OffsetDateTime.now(clock).minus(retention());

		return removeInBatches(page -> outboxRepository.findByDeliveredAtBefore(deliveredBefore, page).stream()
			.map(ProcessEventOutboxEntity::getId)
			.toList(), outboxRepository::deleteAllByIdInBatch);
	}

	/**
	 * Removes the entries of the activity log written longer ago than their retention.
	 *
	 * @return how many entries were removed.
	 */
	public int removeExpiredActivities() {
		final var createdBefore = OffsetDateTime.now(clock).minus(properties.activityRetention());

		return removeInBatches(page -> activityRepository.findByCreatedBeforeOrderByCreatedAsc(createdBefore, page).stream()
			.map(ErrandProcessActivityEntity::getId)
			.toList(), activityRepository::deleteAllByIdInBatch);
	}

	/**
	 * How long a delivered row is kept: six windows of the emergency brake, and a day at the least. A row thereby outlives
	 * the window in which the brake counts it.
	 */
	Duration retention() {
		final var windows = processEngineProperties.loopGuard().window().multipliedBy(WINDOWS_KEPT);

		return windows.compareTo(SHORTEST_RETENTION) > 0 ? windows : SHORTEST_RETENTION;
	}

	private int removeInBatches(final Function<Pageable, List<String>> nextBatch, final Consumer<List<String>> remove) {
		final var page = PageRequest.of(0, properties.batchSize());
		var removed = 0;
		List<String> batch;

		do {
			batch = nextBatch.apply(page);
			remove.accept(batch);
			removed += batch.size();
		} while (batch.size() == properties.batchSize());

		return removed;
	}
}
