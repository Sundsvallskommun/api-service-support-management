package se.sundsvall.supportmanagement.service;

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
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
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

@ExtendWith(MockitoExtension.class)
class LabelMoveWorkerTest {

	private static final String JOB_ID = randomUUID().toString();
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String STARTED_BY = "joe01doe";
	private static final int BATCH_SIZE = 2;
	// Long enough that it never fires within a fast-running unit test - tests that want the heartbeat to fire use
	// Duration.ZERO explicitly instead.
	private static final Duration PROGRESS_INTERVAL = Duration.ofMinutes(1);

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private EventService eventServiceMock;

	private LabelMoveWorker worker;

	private LabelMoveWorker worker() {
		if (worker == null) {
			worker = new LabelMoveWorker(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock, jobServiceMock, eventServiceMock,
				new LabelMoveProperties(BATCH_SIZE, 2, PROGRESS_INTERVAL));
		}
		return worker;
	}

	@Test
	void run_happyPath_reparentsLabelRestowsErrandsAndCompletesJob() {
		var movedId = "moved";
		var newParentId = "new-parent";

		var newParent = labelEntity(newParentId, null, "TARGET");
		var moved = labelEntity(movedId, labelEntity("old-parent", null, "ROOT"), "ROOT/MOVED");
		var errand = errandWithAccessLabels(movedId).withId("errand-1");

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(metadataLabelRepositoryMock.findById(newParentId)).thenReturn(Optional.of(newParent));
		when(errandsRepositoryMock.findAllById(List.of("errand-1"))).thenReturn(List.of(errand));

		worker().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, newParentId, List.of("errand-1"), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).findById(newParentId);
		assertThat(moved.getParent()).isSameAs(newParent);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		// The whole subtree's resourcePath is refreshed by the @PreUpdate cascade inside saveAndFlush above - the worker
		// itself does no separate re-parent-and-refresh pass, and issues no further calls to metadataLabelRepository.
		verify(errandsRepositoryMock).findAllById(List.of("errand-1"));
		// The label rebuild itself is ErrandService's job (persistLabelMigrationBatch), not the worker's - it hands
		// over the chunk exactly as read.
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand));
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	void run_moveToRoot_setsParentNullAndSkipsRestowWhenNoErrandsAffected() {
		var movedId = "moved";
		var moved = labelEntity(movedId, labelEntity("old-parent", null, "ROOT"), "ROOT/MOVED");

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));

		worker().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, null, List.of(), STARTED_BY));

		assertThat(moved.getParent()).isNull();
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
		verify(errandsRepositoryMock, never()).findAllById(any());
		verify(errandServiceMock, never()).persistLabelMigrationBatch(any());
	}

	@Test
	@DisplayName("Verification that a frozen errand-id list longer than the batch size is walked chunk by chunk, each persisted and reported on its own")
	void run_multipleChunks_persistsEachChunkInItsOwnBatchAndReportsProgressPerChunk() {
		var movedId = "moved";
		var moved = labelEntity(movedId, null, "ROOT");
		var errand1 = errandWithAccessLabels(movedId).withId("errand-1");
		var errand2 = errandWithAccessLabels(movedId).withId("errand-2");
		var pagedWorker = new LabelMoveWorker(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock, jobServiceMock, eventServiceMock,
			new LabelMoveProperties(1, 2, PROGRESS_INTERVAL));

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(errandsRepositoryMock.findAllById(List.of("errand-1"))).thenReturn(List.of(errand1));
		when(errandsRepositoryMock.findAllById(List.of("errand-2"))).thenReturn(List.of(errand2));

		pagedWorker.run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, null, List.of("errand-1", "errand-2"), STARTED_BY));

		verify(errandsRepositoryMock).findAllById(List.of("errand-1"));
		verify(errandsRepositoryMock).findAllById(List.of("errand-2"));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand1));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand2));
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock).updateProgress(JOB_ID, 2);
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that progress is reported from inside a page, not only at its boundary, once the progress interval has elapsed - mirrors ErrandPurgeWorker's own heartbeat, and is what keeps a page that is merely slow from being taken for abandoned mid-flight")
	void run_progressIntervalElapsed_reportsFromInsideAPage() {
		var movedId = "moved";
		var moved = labelEntity(movedId, null, "ROOT");
		var errand1 = errandWithAccessLabels(movedId).withId("errand-1");
		var errand2 = errandWithAccessLabels(movedId).withId("errand-2");
		// Zero interval: the heartbeat fires after every single errand, not only once a page's persists are all done.
		var heartbeatWorker = new LabelMoveWorker(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock, jobServiceMock, eventServiceMock,
			new LabelMoveProperties(BATCH_SIZE, 2, Duration.ZERO));

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(errandsRepositoryMock.findAllById(List.of("errand-1", "errand-2"))).thenReturn(List.of(errand1, errand2));

		heartbeatWorker.run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, null, List.of("errand-1", "errand-2"), STARTED_BY));

		// Once from inside the page after errand-1, once from inside after errand-2, and once more at the page boundary.
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock, times(2)).updateProgress(JOB_ID, 2);
		verify(errandsRepositoryMock).findAllById(List.of("errand-1", "errand-2"));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand1));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand2));
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that an errand which loses the optimistic-lock race against a concurrent edit is retried against a fresh read rather than failing the whole job")
	void run_optimisticLockConflictOnChunk_retriesAgainstFreshReadAndSucceeds() {
		var movedId = "moved";
		var moved = labelEntity(movedId, null, "ROOT");
		// Same id (a retry re-reads the same errand); errandNumber differs only so the two invocations below can be
		// told apart in the verifications - ErrandEntity.equals() does not compare version.
		var staleErrand = errandWithAccessLabels(movedId).withId("errand-1").withErrandNumber("stale");
		var freshErrand = errandWithAccessLabels(movedId).withId("errand-1").withErrandNumber("fresh");

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(errandsRepositoryMock.findAllById(List.of("errand-1")))
			.thenReturn(List.of(staleErrand), List.of(freshErrand));
		doThrow(new ObjectOptimisticLockingFailureException(ErrandEntity.class, "errand-1"))
			.doNothing()
			.when(errandServiceMock).persistLabelMigrationBatch(any());

		worker().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, null, List.of("errand-1"), STARTED_BY));

		verify(errandsRepositoryMock, times(2)).findAllById(List.of("errand-1"));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(staleErrand));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(freshErrand));
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that an errand which keeps losing the optimistic-lock race on every attempt fails the job instead of retrying forever")
	void run_optimisticLockConflictOnEveryAttempt_failsJob() {
		var movedId = "moved";
		var moved = labelEntity(movedId, null, "ROOT");
		var errand = errandWithAccessLabels(movedId).withId("errand-1");

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(errandsRepositoryMock.findAllById(List.of("errand-1"))).thenReturn(List.of(errand));
		doThrow(new ObjectOptimisticLockingFailureException(ErrandEntity.class, "errand-1"))
			.when(errandServiceMock).persistLabelMigrationBatch(any());

		worker().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, null, List.of("errand-1"), STARTED_BY));

		verify(errandsRepositoryMock, times(3)).findAllById(List.of("errand-1"));
		verify(errandServiceMock, times(3)).persistLabelMigrationBatch(List.of(errand));
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.startsWith("Label move aborted:")));
		verifyNoInteractions(eventServiceMock);
	}

	@Test
	void run_labelNoLongerExists_failsJobWithoutTouchingErrandsOrAuditing() {
		var movedId = "gone";
		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.empty());

		worker().run(new LabelMoveRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, movedId, null, List.of(), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.contains("no longer exists")));
		verifyNoInteractions(errandServiceMock, eventServiceMock);
		verify(errandsRepositoryMock, never()).findAllById(any());
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock, jobServiceMock, eventServiceMock);
	}

	private static ErrandEntity errandWithAccessLabels(final String... leafIds) {
		var accessLabels = java.util.Arrays.stream(leafIds)
			.map(id -> AccessLabelEmbeddable.create().withMetadataLabelId(id))
			.toList();
		return ErrandEntity.create().withAccessLabels(accessLabels);
	}

	private static MetadataLabelEntity labelEntity(final String id, final MetadataLabelEntity parent, final String resourcePath) {
		return MetadataLabelEntity.create().withId(id).withParent(parent).withResourcePath(resourcePath);
	}
}
