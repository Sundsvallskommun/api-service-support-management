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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
	private static final String STARTED_BY = "joe01doe";
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
			runner = new LabelMoveRunner(errandsRepositoryMock, errandServiceMock, metadataServiceMock, jobServiceMock, eventServiceMock,
				new LabelMoveProperties(BATCH_SIZE, 2, PROGRESS_INTERVAL, PROGRESS_INTERVAL));
		}
		return runner;
	}

	@Test
	void run_happyPath_reparentsLabelRestowsErrandsAndCompletesJob() {
		var errand = errandWithAccessLabels(LABEL_ID).withId("errand-1");

		when(errandsRepositoryMock.findAllById(List.of("errand-1"))).thenReturn(List.of(errand));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));

		runner().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, NEW_PARENT_ID, List.of("errand-1"), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		// Re-validation and the re-parent write itself are MetadataService's job, not the runner's - see its own doc
		// for why that must happen inside the worker's own transaction rather than trusting the request thread's.
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, NEW_PARENT_ID);
		verify(errandsRepositoryMock).findAllById(List.of("errand-1"));
		// The label rebuild itself is ErrandService's job (persistLabelMigrationBatch), not the runner's - it hands
		// over the chunk exactly as read.
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand));
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		// Once from the one page's own stop check, once more from move()'s own check before completing.
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
		// No page for an empty errand-id list to check from inside, so the one call is move()'s own, made regardless.
		verify(jobServiceMock).statusOf(JOB_ID);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(LABEL_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
		verify(errandsRepositoryMock, never()).findAllById(any());
		verify(errandServiceMock, never()).persistLabelMigrationBatch(any());
	}

	@Test
	@DisplayName("Verification that a run stops restowing once the job is no longer running, rather than carrying on to restow pages - and complete or audit a job - a second run's reclaimed lease may already have taken over")
	void run_jobNoLongerRunning_stopsWithoutRestowingFurtherPagesOrCompletingOrAuditing() {
		var errand1 = errandWithAccessLabels(LABEL_ID).withId("errand-1");
		var pagedRunner = new LabelMoveRunner(errandsRepositoryMock, errandServiceMock, metadataServiceMock, jobServiceMock, eventServiceMock,
			new LabelMoveProperties(1, 2, PROGRESS_INTERVAL, PROGRESS_INTERVAL));

		when(errandsRepositoryMock.findAllById(List.of("errand-1"))).thenReturn(List.of(errand1));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(STOPPED));

		pagedRunner.run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of("errand-1", "errand-2"), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		// The first page is restowed and reported before the job is ever asked about - only the second page, which
		// would restow errands a second run's own reclaimed lease may already be walking, is skipped.
		verify(errandsRepositoryMock).findAllById(List.of("errand-1"));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand1));
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		// Once from restowErrands' own page check, which is what stops it from ever reading a second page, and once
		// more from move()'s own check afterward, which is what skips the audit event and complete() below.
		verify(jobServiceMock, times(2)).statusOf(JOB_ID);
		verifyNoInteractions(eventServiceMock);
		verify(jobServiceMock, never()).complete(any(), any());
		verify(jobServiceMock, never()).fail(any(), any());
	}

	@Test
	@DisplayName("Verification that a frozen errand-id list longer than the batch size is walked chunk by chunk, each persisted and reported on its own")
	void run_multipleChunks_persistsEachChunkInItsOwnBatchAndReportsProgressPerChunk() {
		var errand1 = errandWithAccessLabels(LABEL_ID).withId("errand-1");
		var errand2 = errandWithAccessLabels(LABEL_ID).withId("errand-2");
		var pagedRunner = new LabelMoveRunner(errandsRepositoryMock, errandServiceMock, metadataServiceMock, jobServiceMock, eventServiceMock,
			new LabelMoveProperties(1, 2, PROGRESS_INTERVAL, PROGRESS_INTERVAL));

		when(errandsRepositoryMock.findAllById(List.of("errand-1"))).thenReturn(List.of(errand1));
		when(errandsRepositoryMock.findAllById(List.of("errand-2"))).thenReturn(List.of(errand2));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));

		pagedRunner.run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of("errand-1", "errand-2"), STARTED_BY));

		verify(errandsRepositoryMock).findAllById(List.of("errand-1"));
		verify(errandsRepositoryMock).findAllById(List.of("errand-2"));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand1));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand2));
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock).updateProgress(JOB_ID, 2);
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		// Once from each of the two pages' own stop check, once more from move()'s own check before completing.
		verify(jobServiceMock, times(3)).statusOf(JOB_ID);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(LABEL_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that progress is reported from inside a page, not only at its boundary, once the progress interval has elapsed - mirrors ErrandPurgeRunner's own heartbeat, and is what keeps a page that is merely slow from being taken for abandoned mid-flight")
	void run_progressIntervalElapsed_reportsFromInsideAPage() {
		var errand1 = errandWithAccessLabels(LABEL_ID).withId("errand-1");
		var errand2 = errandWithAccessLabels(LABEL_ID).withId("errand-2");
		// Zero interval: the heartbeat fires after every single errand, not only once a page's persists are all done.
		var heartbeatRunner = new LabelMoveRunner(errandsRepositoryMock, errandServiceMock, metadataServiceMock, jobServiceMock, eventServiceMock,
			new LabelMoveProperties(BATCH_SIZE, 2, Duration.ZERO, PROGRESS_INTERVAL));

		when(errandsRepositoryMock.findAllById(List.of("errand-1", "errand-2"))).thenReturn(List.of(errand1, errand2));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));

		heartbeatRunner.run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of("errand-1", "errand-2"), STARTED_BY));

		// Once from inside the page after errand-1, once from inside after errand-2, and once more at the page boundary.
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock, times(2)).updateProgress(JOB_ID, 2);
		verify(errandsRepositoryMock).findAllById(List.of("errand-1", "errand-2"));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand1));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand2));
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		// Both ids land in one page here (batchSize covers both), so the stop check fires once from that page, once more
		// from move()'s own check before completing.
		verify(jobServiceMock, times(2)).statusOf(JOB_ID);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(LABEL_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that an errand which loses the optimistic-lock race against a concurrent edit is retried against a fresh read rather than failing the whole job")
	void run_optimisticLockConflictOnChunk_retriesAgainstFreshReadAndSucceeds() {
		// Same id (a retry re-reads the same errand); errandNumber differs only so the two invocations below can be
		// told apart in the verifications - ErrandEntity.equals() does not compare version.
		var staleErrand = errandWithAccessLabels(LABEL_ID).withId("errand-1").withErrandNumber("stale");
		var freshErrand = errandWithAccessLabels(LABEL_ID).withId("errand-1").withErrandNumber("fresh");

		when(errandsRepositoryMock.findAllById(List.of("errand-1")))
			.thenReturn(List.of(staleErrand), List.of(freshErrand));
		doThrow(new ObjectOptimisticLockingFailureException(ErrandEntity.class, "errand-1"))
			.doNothing()
			.when(errandServiceMock).persistLabelMigrationBatch(any());
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));

		runner().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of("errand-1"), STARTED_BY));

		verify(errandsRepositoryMock, times(2)).findAllById(List.of("errand-1"));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(staleErrand));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(freshErrand));
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		verify(jobServiceMock, times(2)).statusOf(JOB_ID);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(LABEL_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that an errand which keeps losing the optimistic-lock race on every attempt is left unrestowed and logged rather than failing the whole job - one stubborn errand must not strand every other one in the same run")
	void run_optimisticLockConflictOnEveryAttempt_leavesErrandUnrestowedButCompletesJob() {
		var errand = errandWithAccessLabels(LABEL_ID).withId("errand-1");

		when(errandsRepositoryMock.findAllById(List.of("errand-1"))).thenReturn(List.of(errand));
		doThrow(new ObjectOptimisticLockingFailureException(ErrandEntity.class, "errand-1"))
			.when(errandServiceMock).persistLabelMigrationBatch(any());
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));

		runner().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of("errand-1"), STARTED_BY));

		verify(errandsRepositoryMock, times(3)).findAllById(List.of("errand-1"));
		verify(errandServiceMock, times(3)).persistLabelMigrationBatch(List.of(errand));
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock, times(2)).statusOf(JOB_ID);
		// The job still completes - the one unrestowed errand is reported as 0 of 1 restowed, not a job failure - and
		// is picked up again the next time the same move is started, restowing being idempotent per errand.
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(LABEL_ID), eq(STARTED_BY), argThat(message -> message.contains("0 of 1")));
		verify(jobServiceMock).complete(eq(JOB_ID), argThat(message -> message.contains("0 of 1")));
		verify(jobServiceMock, never()).fail(any(), any());
	}

	@Test
	void run_labelNoLongerExists_failsJobWithoutTouchingErrandsOrAuditing() {
		doThrow(new IllegalStateException("Label gone no longer exists"))
			.when(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);

		runner().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null, List.of(), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataServiceMock).revalidateAndReparent(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, null);
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.contains("no longer exists")));
		verifyNoInteractions(errandServiceMock, eventServiceMock);
		verify(errandsRepositoryMock, never()).findAllById(any());
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandsRepositoryMock, errandServiceMock, metadataServiceMock, jobServiceMock, eventServiceMock);
	}

	private static ErrandEntity errandWithAccessLabels(final String... leafIds) {
		var accessLabels = java.util.Arrays.stream(leafIds)
			.map(id -> AccessLabelEmbeddable.create().withMetadataLabelId(id))
			.toList();
		return ErrandEntity.create().withAccessLabels(accessLabels);
	}
}
