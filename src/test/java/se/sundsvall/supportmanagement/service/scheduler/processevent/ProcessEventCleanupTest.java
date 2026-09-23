package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.DirectRun;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.LoopGuard;
import se.sundsvall.supportmanagement.config.ProcessEventCleanupProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessEventCleanupTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T02:30:00Z"), ZoneId.of("UTC"));
	private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);
	private static final int BATCH_SIZE = 2;
	private static final Duration ACTIVITY_RETENTION = Duration.ofDays(90);

	@Mock
	private ProcessEventOutboxRepository outboxRepositoryMock;

	@Mock
	private ErrandProcessActivityRepository activityRepositoryMock;

	@Test
	@DisplayName("Verification that delivered rows are removed a batch at a time, until a batch comes back short")
	void deliveredRowsAreRemovedBatchByBatch() {
		final var first = List.of(row("row-1"), row("row-2"));
		final var last = List.of(row("row-3"));
		when(outboxRepositoryMock.findByDeliveredAtBefore(NOW.minus(Duration.ofDays(1)), PageRequest.of(0, BATCH_SIZE))).thenReturn(first, last);

		assertThat(cleanup(Duration.ofMinutes(10)).removeDelivered()).isEqualTo(3);

		verify(outboxRepositoryMock).deleteAllByIdInBatch(List.of("row-1", "row-2"));
		verify(outboxRepositoryMock).deleteAllByIdInBatch(List.of("row-3"));
		verifyNoInteractions(activityRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a cleanup with nothing to remove asks once and removes nothing")
	void nothingToRemove() {
		when(outboxRepositoryMock.findByDeliveredAtBefore(NOW.minus(Duration.ofDays(1)), PageRequest.of(0, BATCH_SIZE))).thenReturn(List.of());

		assertThat(cleanup(Duration.ofMinutes(10)).removeDelivered()).isZero();

		verify(outboxRepositoryMock).deleteAllByIdInBatch(List.of());
	}

	@Test
	@DisplayName("Verification that entries of the activity log are removed once they have outlived their retention, a batch at a time")
	void expiredActivitiesAreRemovedBatchByBatch() {
		final var first = List.of(activity("entry-1"), activity("entry-2"));
		final var last = List.of(activity("entry-3"));
		when(activityRepositoryMock.findByCreatedBeforeOrderByCreatedAsc(NOW.minus(ACTIVITY_RETENTION), PageRequest.of(0, BATCH_SIZE))).thenReturn(first, last);

		assertThat(cleanup(Duration.ofMinutes(10)).removeExpiredActivities()).isEqualTo(3);

		verify(activityRepositoryMock).deleteAllByIdInBatch(List.of("entry-1", "entry-2"));
		verify(activityRepositoryMock).deleteAllByIdInBatch(List.of("entry-3"));
		verifyNoInteractions(outboxRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a delivered row is kept for a day at the least, however short the window of the emergency brake")
	void theRetentionIsADayAtTheLeast() {
		assertThat(cleanup(Duration.ofMinutes(10)).retention()).isEqualTo(Duration.ofDays(1));
		assertThat(cleanup(Duration.ofHours(4)).retention()).isEqualTo(Duration.ofDays(1));
	}

	@Test
	@DisplayName("Verification that a delivered row outlives six windows of the emergency brake, which counts delivered rows inside its window")
	void theRetentionIsSixWindowsWhenThatIsLonger() {
		assertThat(cleanup(Duration.ofHours(5)).retention()).isEqualTo(Duration.ofHours(30));
	}

	private ProcessEventCleanup cleanup(final Duration window) {
		final var properties = new ProcessEngineProperties(new LoopGuard(20, window), new DirectRun(true, 2, 4, 500));
		return new ProcessEventCleanup(outboxRepositoryMock, activityRepositoryMock, properties, new ProcessEventCleanupProperties(BATCH_SIZE, ACTIVITY_RETENTION), CLOCK);
	}

	private static ProcessEventOutboxEntity row(final String id) {
		return ProcessEventOutboxEntity.create().withId(id);
	}

	private static ErrandProcessActivityEntity activity(final String id) {
		return ErrandProcessActivityEntity.create().withId(id);
	}
}
