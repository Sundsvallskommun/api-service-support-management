package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ActionConfigRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelMergeWorkerTest {

	private static final String JOB_ID = randomUUID().toString();
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String STARTED_BY = "joe01doe";
	private static final String TARGET_ID = "target";
	private static final int BATCH_SIZE = 2;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private ActionConfigRepository actionConfigRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private EventService eventServiceMock;

	@Mock
	private PlatformTransactionManager transactionManagerMock;

	@Mock
	private TransactionStatus transactionStatusMock;

	private LabelMergeWorker worker;

	private LabelMergeWorker worker() {
		if (worker == null) {
			worker = newWorker(BATCH_SIZE);
		}
		return worker;
	}

	@Test
	@DisplayName("Verification that a merge restows the errands, takes each source label out of its parent's children before deleting it, and completes the job")
	void run_happyPath_restowsErrandsDeletesSourcesAndCompletesJob() {
		var sourceIds = Set.of("source-1", "source-2");
		var errand = errandWithAccessLabels("source-1").withId("errand-1");
		var pageable = PageRequest.ofSize(BATCH_SIZE);
		var parent = MetadataLabelEntity.create().withId("parent");
		var source1 = MetadataLabelEntity.create().withId("source-1").withParent(parent);
		var source2 = MetadataLabelEntity.create().withId("source-2").withParent(parent);
		var target = MetadataLabelEntity.create().withId(TARGET_ID).withParent(parent);
		parent.setMetadataLabels(new ArrayList<>(List.of(source1, source2, target)));
		when(metadataLabelRepositoryMock.findAllById(sourceIds)).thenReturn(List.of(source1, source2));

		when(metadataLabelRepositoryMock.existsById(TARGET_ID)).thenReturn(true);
		when(metadataLabelRepositoryMock.existsById("source-1")).thenReturn(true);
		when(metadataLabelRepositoryMock.existsById("source-2")).thenReturn(true);
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "", pageable))
			.thenReturn(List.of(errand));
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());

		worker().run(new LabelMergeRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, TARGET_ID, sourceIds, STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).existsById(TARGET_ID);
		verify(metadataLabelRepositoryMock).existsById("source-1");
		verify(metadataLabelRepositoryMock).existsById("source-2");
		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "", pageable);
		verify(errandServiceMock).persistLabelMergeBatch(List.of(errand), sourceIds, TARGET_ID);
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).findAllById(sourceIds);
		verify(metadataLabelRepositoryMock).delete(source1);
		verify(metadataLabelRepositoryMock).delete(source2);
		verify(transactionManagerMock).commit(transactionStatusMock);
		assertThat(parent.getMetadataLabels()).containsExactly(target);
		verify(eventServiceMock).createLabelMergeEvent(eq(MUNICIPALITY_ID), eq(TARGET_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that an action's hasLabel condition naming a source label is retargeted to the destination label rather than left pointing at a row about to be deleted")
	void run_actionConditionReferencesSource_isRetargetedToDestination() {
		var sourceIds = Set.of("source-1");
		var errand = errandWithAccessLabels("source-1").withId("errand-1");
		var pageable = PageRequest.ofSize(BATCH_SIZE);
		var condition = ActionConfigConditionEntity.create().withKey("hasLabel").withValues(List.of("source-1", "unrelated"));
		var config = ActionConfigEntity.create().withConditions(List.of(condition));
		var unrelatedCondition = ActionConfigConditionEntity.create().withKey("status").withValues(List.of("OPEN"));
		var unrelatedConfig = ActionConfigEntity.create().withConditions(List.of(unrelatedCondition));

		when(metadataLabelRepositoryMock.existsById(TARGET_ID)).thenReturn(true);
		when(metadataLabelRepositoryMock.existsById("source-1")).thenReturn(true);
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "", pageable))
			.thenReturn(List.of(errand));
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(config, unrelatedConfig));

		worker().run(new LabelMergeRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, TARGET_ID, sourceIds, STARTED_BY));

		assertThat(condition.getValues()).containsExactlyInAnyOrder(TARGET_ID, "unrelated");
		assertThat(unrelatedCondition.getValues()).containsExactly("OPEN");
		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(actionConfigRepositoryMock).saveAll(List.of(config));
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).existsById(TARGET_ID);
		verify(metadataLabelRepositoryMock).existsById("source-1");
		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "", pageable);
		verify(errandServiceMock).persistLabelMergeBatch(List.of(errand), sourceIds, TARGET_ID);
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(metadataLabelRepositoryMock).findAllById(sourceIds);
		verify(eventServiceMock).createLabelMergeEvent(eq(MUNICIPALITY_ID), eq(TARGET_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that paging keeps walking by keyset (id > lastSeenId) until a page comes back shorter than the batch size")
	void run_multiplePages_persistsEachPageInItsOwnBatchAndReportsProgressPerPage() {
		var sourceIds = Set.of("source-1");
		var errand1 = errandWithAccessLabels("source-1").withId("errand-1");
		var errand2 = errandWithAccessLabels("source-1").withId("errand-2");
		var pageable = PageRequest.ofSize(1);
		var pagedWorker = newWorker(1);

		when(metadataLabelRepositoryMock.existsById(TARGET_ID)).thenReturn(true);
		when(metadataLabelRepositoryMock.existsById("source-1")).thenReturn(true);
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "", pageable))
			.thenReturn(List.of(errand1));
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "errand-1", pageable))
			.thenReturn(List.of(errand2));
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "errand-2", pageable))
			.thenReturn(List.of());
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());

		pagedWorker.run(new LabelMergeRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, TARGET_ID, sourceIds, STARTED_BY));

		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "", pageable);
		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "errand-1", pageable);
		verify(errandsRepositoryMock).findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "errand-2", pageable);
		verify(errandServiceMock).persistLabelMergeBatch(List.of(errand1), sourceIds, TARGET_ID);
		verify(errandServiceMock).persistLabelMergeBatch(List.of(errand2), sourceIds, TARGET_ID);
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock).updateProgress(JOB_ID, 2);
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).existsById(TARGET_ID);
		verify(metadataLabelRepositoryMock).existsById("source-1");
		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).findAllById(sourceIds);
		verify(eventServiceMock).createLabelMergeEvent(eq(MUNICIPALITY_ID), eq(TARGET_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that a page which loses the optimistic-lock race against a concurrent edit is retried against a fresh read rather than failing the whole job")
	void run_optimisticLockConflictOnPage_retriesAgainstFreshReadAndSucceeds() {
		var sourceIds = Set.of("source-1");
		var staleErrand = errandWithAccessLabels("source-1").withId("errand-1").withErrandNumber("stale");
		var freshErrand = errandWithAccessLabels("source-1").withId("errand-1").withErrandNumber("fresh");
		var pageable = PageRequest.ofSize(BATCH_SIZE);

		when(metadataLabelRepositoryMock.existsById(TARGET_ID)).thenReturn(true);
		when(metadataLabelRepositoryMock.existsById("source-1")).thenReturn(true);
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "", pageable))
			.thenReturn(List.of(staleErrand), List.of(freshErrand));
		doThrow(new ObjectOptimisticLockingFailureException(ErrandEntity.class, "errand-1"))
			.doNothing()
			.when(errandServiceMock).persistLabelMergeBatch(any(), eq(sourceIds), eq(TARGET_ID));
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());

		worker().run(new LabelMergeRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, TARGET_ID, sourceIds, STARTED_BY));

		verify(errandsRepositoryMock, times(2)).findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "", pageable);
		verify(errandServiceMock).persistLabelMergeBatch(List.of(staleErrand), sourceIds, TARGET_ID);
		verify(errandServiceMock).persistLabelMergeBatch(List.of(freshErrand), sourceIds, TARGET_ID);
		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).existsById(TARGET_ID);
		verify(metadataLabelRepositoryMock).existsById("source-1");
		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).findAllById(sourceIds);
		verify(eventServiceMock).createLabelMergeEvent(eq(MUNICIPALITY_ID), eq(TARGET_ID), eq(STARTED_BY), any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@Test
	@DisplayName("Verification that a page which keeps losing the optimistic-lock race on every attempt fails the job instead of retrying forever, and never deletes the source labels")
	void run_optimisticLockConflictOnEveryAttempt_failsJob() {
		var sourceIds = Set.of("source-1");
		var errand = errandWithAccessLabels("source-1").withId("errand-1");
		var pageable = PageRequest.ofSize(BATCH_SIZE);

		when(metadataLabelRepositoryMock.existsById(TARGET_ID)).thenReturn(true);
		when(metadataLabelRepositoryMock.existsById("source-1")).thenReturn(true);
		when(errandsRepositoryMock.findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "", pageable))
			.thenReturn(List.of(errand));
		doThrow(new ObjectOptimisticLockingFailureException(ErrandEntity.class, "errand-1"))
			.when(errandServiceMock).persistLabelMergeBatch(any(), eq(sourceIds), eq(TARGET_ID));

		worker().run(new LabelMergeRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, TARGET_ID, sourceIds, STARTED_BY));

		verify(errandsRepositoryMock, times(3)).findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceIds, "", pageable);
		verify(errandServiceMock, times(3)).persistLabelMergeBatch(List.of(errand), sourceIds, TARGET_ID);
		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).existsById(TARGET_ID);
		verify(metadataLabelRepositoryMock).existsById("source-1");
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.startsWith("Label merge aborted:")));
		verify(metadataLabelRepositoryMock, never()).findAllById(any());
		verify(metadataLabelRepositoryMock, never()).delete(any());
		verifyNoInteractions(eventServiceMock);
	}

	@Test
	void run_targetNoLongerExists_failsJobWithoutTouchingErrandsOrAuditing() {
		when(metadataLabelRepositoryMock.existsById(TARGET_ID)).thenReturn(false);

		worker().run(new LabelMergeRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, TARGET_ID, Set.of("source-1"), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).existsById(TARGET_ID);
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.contains("no longer exists")));
		verifyNoInteractions(errandServiceMock, eventServiceMock);
		verify(errandsRepositoryMock, never()).findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(any(), any(), any());
	}

	@Test
	void run_sourceNoLongerExists_failsJobWithoutTouchingErrandsOrAuditing() {
		when(metadataLabelRepositoryMock.existsById(TARGET_ID)).thenReturn(true);
		when(metadataLabelRepositoryMock.existsById("source-1")).thenReturn(false);

		worker().run(new LabelMergeRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, TARGET_ID, Set.of("source-1"), STARTED_BY));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).existsById(TARGET_ID);
		verify(metadataLabelRepositoryMock).existsById("source-1");
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.contains("no longer exists")));
		verifyNoInteractions(errandServiceMock, eventServiceMock);
		verify(errandsRepositoryMock, never()).findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(any(), any(), any());
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandsRepositoryMock, metadataLabelRepositoryMock, actionConfigRepositoryMock, errandServiceMock, jobServiceMock, eventServiceMock);
	}

	private LabelMergeWorker newWorker(final int batchSize) {
		// Only a merge that gets as far as deleting its sources opens a transaction - lenient so the tests that fail
		// before that are not flagged for an unused stub.
		lenient().when(transactionManagerMock.getTransaction(any())).thenReturn(transactionStatusMock);
		return new LabelMergeWorker(errandsRepositoryMock, metadataLabelRepositoryMock, actionConfigRepositoryMock, errandServiceMock, jobServiceMock, eventServiceMock,
			new LabelMoveProperties(batchSize, 2), transactionManagerMock);
	}

	private static ErrandEntity errandWithAccessLabels(final String... leafIds) {
		var accessLabels = java.util.Arrays.stream(leafIds)
			.map(id -> AccessLabelEmbeddable.create().withMetadataLabelId(id))
			.toList();
		return ErrandEntity.create().withAccessLabels(accessLabels);
	}
}
