package se.sundsvall.supportmanagement.service.job;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.MetadataService;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;

@ExtendWith(MockitoExtension.class)
class LabelMoveRunnerTest {

	private static final String JOB_ID = randomUUID().toString();
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String STARTED_BY = "joe01doe; type=adAccount";
	private static final String LABEL_ID = "moved";
	private static final String NEW_PARENT_ID = "new-parent";
	private static final int BATCH_SIZE = 2;
	// Long enough that it never fires within a fast-running unit test - tests that want the heartbeat to fire use
	// Duration.ZERO explicitly instead.
	private static final Duration PROGRESS_INTERVAL = Duration.ofMinutes(1);

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private MetadataService metadataServiceMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private EventService eventServiceMock;

	private LabelMoveRunner runner;

	private LabelMoveRunner runner() {
		if (runner == null) {
			runner = newRunner(BATCH_SIZE, PROGRESS_INTERVAL);
		}
		return runner;
	}

	/**
	 * {@code self} is wired to the instance itself after construction, exactly the way {@code JobServiceTest} wires
	 * {@code JobService}'s own {@code self} field - a plain Mockito test builds no Spring proxy, so {@code self} would
	 * otherwise stay {@code null} and {@link LabelMoveRunner#executeMove} would never be reached.
	 */
	private LabelMoveRunner newRunner(final int batchSize, final Duration progressInterval) {
		final var created = new LabelMoveRunner(errandsRepositoryMock, errandServiceMock, metadataServiceMock, jobServiceMock, eventServiceMock,
			new LabelMoveProperties(batchSize, 2, progressInterval, PROGRESS_INTERVAL, 10_000), null);
		ReflectionTestUtils.setField(created, "self", created);
		return created;
	}

	@Test
	void run_happyPath_reparentsLabelRestowsErrandsAndCompletesJob() {
		var errand = errandWithAccessLabels(LABEL_ID).withId("errand-1");

		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1"))).thenReturn(List.of(errand));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));

		runner().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, NEW_PARENT_ID, List.of("errand-1"), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		// Re-validation and the re-parent write itself are MetadataService's job, not the runner's - both now run
		// inside executeMove's own transaction, ahead of the lock-and-restow walk.
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, NEW_PARENT_ID);
		verify(errandsRepositoryMock).findAllByIdForUpdate(List.of("errand-1"));
		// The label rebuild itself is ErrandService's job (persistLabelMigration), not the runner's - it hands over
		// the locked errand exactly as read.
		verify(errandServiceMock).persistLabelMigration(errand);
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		// Once from the one chunk's own stop check, once more from executeMove's trailing check after the walk ends.
		verify(jobServiceMock, times(2)).statusOf(JOB_ID);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(LABEL_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	void run_moveToRoot_setsParentNullAndSkipsRestowWhenNoErrandsAffected() {
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));

		runner().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of(), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		// No chunk for an empty errand-id list to check from inside, so the one call is executeMove's own trailing
		// check, made regardless - otherwise an empty move could never be stopped at all.
		verify(jobServiceMock).statusOf(JOB_ID);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(LABEL_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
		verify(errandsRepositoryMock, never()).findAllByIdForUpdate(any());
		verify(errandServiceMock, never()).persistLabelMigration(any());
	}

	@Test
	@DisplayName("Verification that a run which finds the job no longer running rolls back rather than restowing further chunks, reports the exact progress reached, and fails the job (a no-op once the real JobService sees the job is already STOPPED) rather than completing or auditing it")
	void run_jobNoLongerRunning_rollsBackReportsProgressAndFailsWithoutCompletingOrAuditing() {
		var errand1 = errandWithAccessLabels(LABEL_ID).withId("errand-1");
		var pagedRunner = newRunner(1, PROGRESS_INTERVAL);

		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1"))).thenReturn(List.of(errand1));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(STOPPED));

		pagedRunner.run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of("errand-1", "errand-2"), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		// The first chunk is restowed before the job is ever asked about - only the second chunk, which would restow
		// errands a second run's own reclaimed lease may already be walking, is skipped: the stop is found at the
		// first chunk's own boundary check, which throws immediately rather than reading a second chunk.
		verify(errandsRepositoryMock).findAllByIdForUpdate(List.of("errand-1"));
		verify(errandServiceMock).persistLabelMigration(errand1);
		// Once at the chunk boundary, once more from the catch block that reports the exact count reached before the
		// rollback - both report the same value, since nothing changed in between.
		verify(jobServiceMock, times(2)).updateProgress(JOB_ID, 1);
		// The one call that discovers the job stopped and throws, short-circuiting out of the loop before any later
		// check could run.
		verify(jobServiceMock).statusOf(JOB_ID);
		verifyNoInteractions(eventServiceMock);
		verify(jobServiceMock, never()).complete(any(), any());
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.contains("Reached 1 of 2") && message.contains("stopped by request")));
	}

	@Test
	@DisplayName("Verification that a frozen errand-id list longer than the batch size is walked chunk by chunk, each locked and restowed, and reported on its own")
	void run_multipleChunks_locksAndRestowsEachChunkOnItsOwnAndReportsProgressPerChunk() {
		var errand1 = errandWithAccessLabels(LABEL_ID).withId("errand-1");
		var errand2 = errandWithAccessLabels(LABEL_ID).withId("errand-2");
		var pagedRunner = newRunner(1, PROGRESS_INTERVAL);

		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1"))).thenReturn(List.of(errand1));
		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-2"))).thenReturn(List.of(errand2));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));

		pagedRunner.run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of("errand-1", "errand-2"), STARTED_BY));

		verify(errandsRepositoryMock).findAllByIdForUpdate(List.of("errand-1"));
		verify(errandsRepositoryMock).findAllByIdForUpdate(List.of("errand-2"));
		verify(errandServiceMock).persistLabelMigration(errand1);
		verify(errandServiceMock).persistLabelMigration(errand2);
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock).updateProgress(JOB_ID, 2);
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		// Once from each of the two chunks' own stop check, once more from executeMove's trailing check before
		// completing.
		verify(jobServiceMock, times(3)).statusOf(JOB_ID);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(LABEL_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that progress is reported from inside a chunk, not only at its boundary, once the progress interval has elapsed - mirrors ErrandPurgeRunner's own heartbeat, and is what keeps a chunk that is merely slow from being taken for abandoned mid-flight")
	void run_progressIntervalElapsed_reportsFromInsideAChunk() {
		var errand1 = errandWithAccessLabels(LABEL_ID).withId("errand-1");
		var errand2 = errandWithAccessLabels(LABEL_ID).withId("errand-2");
		// Zero interval: the heartbeat fires after every single errand, not only once a chunk's persists are all done.
		var heartbeatRunner = newRunner(BATCH_SIZE, Duration.ZERO);

		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1", "errand-2"))).thenReturn(List.of(errand1, errand2));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));

		heartbeatRunner.run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of("errand-1", "errand-2"), STARTED_BY));

		// Once from inside the chunk after errand-1, once from inside after errand-2, and once more at the chunk
		// boundary.
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock, times(2)).updateProgress(JOB_ID, 2);
		verify(errandsRepositoryMock).findAllByIdForUpdate(List.of("errand-1", "errand-2"));
		verify(errandServiceMock).persistLabelMigration(errand1);
		verify(errandServiceMock).persistLabelMigration(errand2);
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		// Both ids land in one chunk here (batchSize covers both), so the stop check fires once from that chunk, once
		// more from executeMove's trailing check before completing.
		verify(jobServiceMock, times(2)).statusOf(JOB_ID);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(LABEL_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that a lock-acquisition timeout is failed with a friendly, non-alarming message - the namespace was merely busy, nothing is actually broken - rather than completing or auditing the move")
	void run_lockAcquisitionTimesOut_failsJobWithFriendlyMessageWithoutCompletingOrAuditing() {
		when(errandsRepositoryMock.findAllByIdForUpdate(List.of("errand-1")))
			.thenThrow(new CannotAcquireLockException("could not execute statement"));

		runner().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, NEW_PARENT_ID, List.of("errand-1"), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, NEW_PARENT_ID);
		verify(errandsRepositoryMock).findAllByIdForUpdate(List.of("errand-1"));
		// Reported with the count reached before the failing chunk - none, since the very first chunk is the one that
		// timed out.
		verify(jobServiceMock).updateProgress(JOB_ID, 0);
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.contains("could not acquire locks") && message.contains("retry when the namespace is idle")));
		verifyNoInteractions(errandServiceMock, eventServiceMock);
	}

	@Test
	void run_labelNoLongerExists_failsJobWithoutTouchingErrandsOrAuditing() {
		doThrow(new IllegalStateException("Label gone no longer exists"))
			.when(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);

		runner().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of(), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		// Reported before the rollback, with nothing yet restowed - the failure happened before the walk ever began.
		verify(jobServiceMock).updateProgress(JOB_ID, 0);
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.contains("no longer exists")));
		verifyNoInteractions(errandServiceMock, eventServiceMock);
		verify(errandsRepositoryMock, never()).findAllByIdForUpdate(any());
	}

	private static ErrandEntity errandWithAccessLabels(final String... leafIds) {
		var accessLabels = java.util.Arrays.stream(leafIds)
			.map(id -> AccessLabelEmbeddable.create().withMetadataLabelId(id))
			.toList();
		return ErrandEntity.create().withAccessLabels(accessLabels);
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandsRepositoryMock, errandServiceMock, metadataServiceMock, jobServiceMock, eventServiceMock);
	}
}
