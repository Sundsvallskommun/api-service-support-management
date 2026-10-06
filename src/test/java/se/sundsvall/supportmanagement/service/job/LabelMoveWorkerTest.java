package se.sundsvall.supportmanagement.service.job;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
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

	private static final String JOB_ID = "job-id";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String STARTED_BY = "joe01doe";
	private static final int BATCH_SIZE = 2;
	private static final Duration PROGRESS_INTERVAL = Duration.ofMinutes(1);
	private static final Duration SHUTDOWN_GRACE_PERIOD = Duration.ofSeconds(20);

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private EventService eventServiceMock;

	private LabelMoveWorker worker;

	private LabelMoveWorker worker() {
		if (worker == null) {
			worker = new LabelMoveWorker(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock, eventServiceMock,
				new LabelMoveProperties(BATCH_SIZE, 2, PROGRESS_INTERVAL, SHUTDOWN_GRACE_PERIOD, 10_000));
		}
		return worker;
	}

	@Test
	void moveAndRestow_happyPath_reparentsLabelAndRestowsErrands() {
		var movedId = "moved";
		var newParentId = "new-parent";
		var newParent = labelEntity(newParentId, null, "TARGET");
		var moved = labelEntity(movedId, labelEntity("old-parent", null, "ROOT"), "ROOT/MOVED");
		var errand = errandWithAccessLabels(movedId).withId("errand-1");
		var pageable = PageRequest.ofSize(BATCH_SIZE);
		var progress = new int[] {
			-1
		};

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(metadataLabelRepositoryMock.findById(newParentId)).thenReturn(Optional.of(newParent));
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable))
			.thenReturn(List.of(errand));

		var restowed = worker().moveAndRestow(JOB_ID, MUNICIPALITY_ID, new LabelMoveStep(movedId, newParentId, null, null), STARTED_BY, processed -> progress[0] = processed);

		assertThat(restowed).isEqualTo(1);
		assertThat(progress[0]).isEqualTo(1);
		assertThat(moved.getParent()).isSameAs(newParent);
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).findById(newParentId);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable);
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand));
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), eq(STARTED_BY), any());
	}

	@Test
	@DisplayName("Verification that a combined move+rename sets the new resourceName and displayName in the same update, as a label-tree restructure's MOVE step may ask for")
	void moveAndRestow_withRename_setsResourceNameAndDisplayNameToo() {
		var movedId = "moved";
		var newParentId = "new-parent";
		var newParent = labelEntity(newParentId, null, "TARGET");
		var moved = labelEntity(movedId, null, "ROOT/MOVED");
		var pageable = PageRequest.ofSize(BATCH_SIZE);

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(metadataLabelRepositoryMock.findById(newParentId)).thenReturn(Optional.of(newParent));
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable))
			.thenReturn(List.of());

		worker().moveAndRestow(JOB_ID, MUNICIPALITY_ID, new LabelMoveStep(movedId, newParentId, "NEW_NAME", "New display"), STARTED_BY, processed -> {
		});

		assertThat(moved.getResourceName()).isEqualTo("NEW_NAME");
		assertThat(moved.getDisplayName()).isEqualTo("New display");
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).findById(newParentId);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), eq(STARTED_BY), any());
	}

	@Test
	void moveAndRestow_moveToRoot_setsParentNullAndSkipsNewParentLookup() {
		var movedId = "moved";
		var moved = labelEntity(movedId, labelEntity("old-parent", null, "ROOT"), "ROOT/MOVED");
		var pageable = PageRequest.ofSize(BATCH_SIZE);

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable))
			.thenReturn(List.of());

		worker().moveAndRestow(JOB_ID, MUNICIPALITY_ID, new LabelMoveStep(movedId, null, null, null), STARTED_BY, processed -> {
		});

		assertThat(moved.getParent()).isNull();
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), eq(STARTED_BY), any());
	}

	@Test
	@DisplayName("Verification that paging keeps walking by keyset (id > lastSeenId) until a page comes back shorter than the batch size")
	void moveAndRestow_multiplePages_persistsEachPageInItsOwnBatchAndReportsCumulativeProgress() {
		var movedId = "moved";
		var moved = labelEntity(movedId, null, "ROOT");
		var errand1 = errandWithAccessLabels(movedId).withId("errand-1");
		var errand2 = errandWithAccessLabels(movedId).withId("errand-2");
		var pageable = PageRequest.ofSize(1);
		var pagedWorker = new LabelMoveWorker(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock, eventServiceMock,
			new LabelMoveProperties(1, 2, PROGRESS_INTERVAL, SHUTDOWN_GRACE_PERIOD, 10_000));
		var progressUpdates = new java.util.ArrayList<Integer>();

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable))
			.thenReturn(List.of(errand1));
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "errand-1", pageable))
			.thenReturn(List.of(errand2));
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "errand-2", pageable))
			.thenReturn(List.of());

		var restowed = pagedWorker.moveAndRestow(JOB_ID, MUNICIPALITY_ID, new LabelMoveStep(movedId, null, null, null), STARTED_BY, progressUpdates::add);

		assertThat(restowed).isEqualTo(2);
		assertThat(progressUpdates).containsExactly(1, 2);
		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable);
		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "errand-1", pageable);
		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "errand-2", pageable);
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand1));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(errand2));
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), eq(STARTED_BY), any());
	}

	@Test
	@DisplayName("Verification that a page which loses the optimistic-lock race against a concurrent edit is retried against a fresh read rather than failing the whole move")
	void moveAndRestow_optimisticLockConflictOnPage_retriesAgainstFreshReadAndSucceeds() {
		var movedId = "moved";
		var moved = labelEntity(movedId, null, "ROOT");
		var staleErrand = errandWithAccessLabels(movedId).withId("errand-1").withErrandNumber("stale");
		var freshErrand = errandWithAccessLabels(movedId).withId("errand-1").withErrandNumber("fresh");
		var pageable = PageRequest.ofSize(BATCH_SIZE);

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable))
			.thenReturn(List.of(staleErrand), List.of(freshErrand));
		doThrow(new ObjectOptimisticLockingFailureException(ErrandEntity.class, "errand-1"))
			.doNothing()
			.when(errandServiceMock).persistLabelMigrationBatch(any());

		var restowed = worker().moveAndRestow(JOB_ID, MUNICIPALITY_ID, new LabelMoveStep(movedId, null, null, null), STARTED_BY, processed -> {
		});

		assertThat(restowed).isEqualTo(1);
		verify(errandsRepositoryMock, times(2)).findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable);
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(staleErrand));
		verify(errandServiceMock).persistLabelMigrationBatch(List.of(freshErrand));
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verify(eventServiceMock).createLabelMoveEvent(eq(MUNICIPALITY_ID), eq(movedId), eq(STARTED_BY), any());
	}

	@Test
	@DisplayName("Verification that a page which keeps losing the optimistic-lock race on every attempt lets the exception propagate rather than retrying forever")
	void moveAndRestow_optimisticLockConflictOnEveryAttempt_propagatesException() {
		var movedId = "moved";
		var moved = labelEntity(movedId, null, "ROOT");
		var errand = errandWithAccessLabels(movedId).withId("errand-1");
		var pageable = PageRequest.ofSize(BATCH_SIZE);

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable))
			.thenReturn(List.of(errand));
		doThrow(new ObjectOptimisticLockingFailureException(ErrandEntity.class, "errand-1"))
			.when(errandServiceMock).persistLabelMigrationBatch(any());

		var progressReporter = (IntConsumer) processed -> {
		};
		var sut = worker();
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> sut.moveAndRestow(JOB_ID, MUNICIPALITY_ID, new LabelMoveStep(movedId, null, null, null), STARTED_BY, progressReporter))
			.isInstanceOf(ObjectOptimisticLockingFailureException.class);

		verify(errandsRepositoryMock, times(3)).findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(movedId, "", pageable);
		verify(errandServiceMock, times(3)).persistLabelMigrationBatch(List.of(errand));
		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).saveAndFlush(moved);
		verifyNoInteractions(eventServiceMock);
	}

	@Test
	void moveAndRestow_labelNoLongerExists_throwsWithoutTouchingErrandsOrAuditing() {
		var movedId = "gone";
		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.empty());

		var progressReporter = (IntConsumer) processed -> {
		};
		assertThatIllegalStateException()
			.isThrownBy(() -> worker().moveAndRestow(JOB_ID, MUNICIPALITY_ID, new LabelMoveStep(movedId, null, null, null), STARTED_BY, progressReporter))
			.withMessageContaining("no longer exists");

		verify(metadataLabelRepositoryMock).findById(movedId);
		verifyNoInteractions(errandServiceMock, eventServiceMock);
		verify(errandsRepositoryMock, never()).findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(any(), any(), any());
	}

	@Test
	void moveAndRestow_newParentNoLongerExists_throwsWithoutTouchingErrandsOrAuditing() {
		var movedId = "moved";
		var newParentId = "gone";
		var moved = labelEntity(movedId, null, "ROOT/MOVED");

		when(metadataLabelRepositoryMock.findById(movedId)).thenReturn(Optional.of(moved));
		when(metadataLabelRepositoryMock.findById(newParentId)).thenReturn(Optional.empty());

		var progressReporter = (IntConsumer) processed -> {
		};
		assertThatIllegalStateException()
			.isThrownBy(() -> worker().moveAndRestow(JOB_ID, MUNICIPALITY_ID, new LabelMoveStep(movedId, newParentId, null, null), STARTED_BY, progressReporter))
			.withMessageContaining("no longer exists");

		verify(metadataLabelRepositoryMock).findById(movedId);
		verify(metadataLabelRepositoryMock).findById(newParentId);
		verifyNoInteractions(errandServiceMock, eventServiceMock);
		verify(errandsRepositoryMock, never()).findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(any(), any(), any());
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandsRepositoryMock, metadataLabelRepositoryMock, errandServiceMock, eventServiceMock);
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
