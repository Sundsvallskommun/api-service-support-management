package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.api.model.metadata.LabelMergeRequest;
import se.sundsvall.supportmanagement.integration.db.ActionConfigRepository;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.PhaseRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.MERGE_LABELS;

@ExtendWith(MockitoExtension.class)
class MetadataServiceMergeLabelsTest {

	private static final String NAMESPACE = "NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String TARGET_ID = "target-id";
	private static final String SOURCE_ID = "source-id";

	@Mock
	private ActionConfigRepository actionConfigRepositoryMock;

	@Mock
	private CategoryRepository categoryRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ExternalIdTypeRepository externalIdTypeRepositoryMock;

	@Mock
	private MeasureTypeRepository measureTypeRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private PhaseRepository phaseRepositoryMock;

	@Mock
	private RoleRepository roleRepositoryMock;

	@Mock
	private StatusRepository statusRepositoryMock;

	@Mock
	private ValidationRepository validationRepositoryMock;

	@Mock
	private ContactReasonRepository contactReasonRepositoryMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private LabelMoveWorker labelMoveWorkerMock;

	@Mock
	private LabelMergeWorker labelMergeWorkerMock;

	@Mock
	private AsyncTaskExecutor labelMoveTaskExecutorMock;

	@Mock
	private PlatformTransactionManager transactionManagerMock;

	@Mock
	private TransactionStatus transactionStatusMock;

	@InjectMocks
	private MetadataService service;

	@BeforeEach
	void setUpTransactionManager() {
		// Only startLabelMerge goes through readOnlyTransactionTemplate - lenient so mergeLabels-only tests, which never
		// touch it, are not flagged for an unused stub.
		lenient().when(transactionManagerMock.getTransaction(any())).thenReturn(transactionStatusMock);
	}

	@Test
	void mergeLabels_targetNotFound_throws404() {
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.mergeLabels(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(SOURCE_ID)).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(NOT_FOUND.value()));

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void mergeLabels_targetHasChildren_throws400() {
		var target = leafLabel(TARGET_ID, "TARGET");

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/"))
			.thenReturn(true);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.mergeLabels(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(SOURCE_ID)).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("children");

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/");
	}

	@Test
	@DisplayName("Verification that a source id resolved to the same row as the target is still caught as a self-merge - each source is looked up and canonicalized before the self-merge check runs, which is why the target's own id and leaf check are each hit twice here: once resolving the target itself, once resolving it again as the (self-referencing) source")
	void mergeLabels_targetAmongSources_throws400() {
		var target = leafLabel(TARGET_ID, "TARGET");

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/"))
			.thenReturn(false);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.mergeLabels(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(TARGET_ID)).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("merged into itself");

		verify(metadataLabelRepositoryMock, times(2)).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock, times(2)).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/");
	}

	@Test
	@DisplayName("Verification that a source id differing from the target's only in case is still caught as a self-merge, rather than slipping through to be deleted along with the real sources once the merge restows everything onto what is, underneath the casing, the very same label")
	void mergeLabels_targetAmongSources_differentCase_throws400() {
		var target = leafLabel(TARGET_ID, "TARGET");
		var differentlyCasedTargetId = TARGET_ID.toUpperCase();

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/"))
			.thenReturn(false);
		// A case-insensitive id lookup/collation resolves the differently-cased source id to the very same row.
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(differentlyCasedTargetId, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.mergeLabels(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(differentlyCasedTargetId)).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("merged into itself");

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(differentlyCasedTargetId, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock, times(2)).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/");
	}

	@Test
	void mergeLabels_sourceNotFound_throws400() {
		var target = leafLabel(TARGET_ID, "TARGET");

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/"))
			.thenReturn(false);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.mergeLabels(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(SOURCE_ID)).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()));

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/");
		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void mergeLabels_sourceHasChildren_throws400() {
		var target = leafLabel(TARGET_ID, "TARGET");
		var source = leafLabel(SOURCE_ID, "SOURCE");

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/"))
			.thenReturn(false);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(source));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/"))
			.thenReturn(true);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.mergeLabels(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(SOURCE_ID)).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("children");

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/");
		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/");
	}

	@Test
	void mergeLabels_dryRun_returnsCountAndActions() {
		var target = leafLabel(TARGET_ID, "TARGET");
		var source = leafLabel(SOURCE_ID, "SOURCE");
		var actionWithLabel = actionConfigEntity("action-id", "ACTION", "Display", List.of(conditionEntity("hasLabel", List.of(SOURCE_ID))));
		var actionWithoutLabel = actionConfigEntity("other-action", "OTHER", null, List.of());

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/"))
			.thenReturn(false);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(source));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/"))
			.thenReturn(false);
		when(errandsRepositoryMock.countDistinctByLabelsMetadataLabelIdIn(Set.of(SOURCE_ID))).thenReturn(4L);
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(List.of(actionWithLabel, actionWithoutLabel));

		var result = service.mergeLabels(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(SOURCE_ID)).withDryRun(true));

		assertThat(result.getAffectedErrandCount()).isEqualTo(4L);
		assertThat(result.getAffectedActions()).hasSize(1)
			.first()
			.satisfies(a -> assertThat(a.getId()).isEqualTo("action-id"));
	}

	@Test
	@DisplayName("Verification that a namespace already worked on by a pending or running merge is not handed a second job, since two runs would race on the same errands")
	void startLabelMerge_activeJobInNamespace_throws409() {
		var target = leafLabel(TARGET_ID, "TARGET");
		var source = leafLabel(SOURCE_ID, "SOURCE");

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/"))
			.thenReturn(false);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(source));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/"))
			.thenReturn(false);
		when(jobServiceMock.hasActiveJob(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startLabelMerge(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(SOURCE_ID)).withDryRun(false)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(CONFLICT.value()));

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/");
		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/");
		verify(jobServiceMock).hasActiveJob(NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock, never()).countDistinctByLabelsMetadataLabelIdIn(any());
	}

	@Test
	void startLabelMerge_targetNotFound_throws404() {
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startLabelMerge(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(SOURCE_ID)).withDryRun(false)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(NOT_FOUND.value()));

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void startLabelMerge_createsJobAndReturnsJobResponse() {
		var target = leafLabel(TARGET_ID, "TARGET");
		var source = leafLabel(SOURCE_ID, "SOURCE");
		var jobResponse = JobResponse.create().withJobId("job-id").withType(MERGE_LABELS).withStatus(JobStatus.PENDING).withTotal(4);

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/"))
			.thenReturn(false);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(source));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/"))
			.thenReturn(false);
		when(errandsRepositoryMock.countDistinctByLabelsMetadataLabelIdIn(Set.of(SOURCE_ID))).thenReturn(4L);
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, MERGE_LABELS, 4, TARGET_ID)).thenReturn("job-id");
		when(jobServiceMock.get(NAMESPACE, MUNICIPALITY_ID, "job-id")).thenReturn(jobResponse);

		var result = service.startLabelMerge(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(SOURCE_ID)).withDryRun(false));

		assertThat(result).isEqualTo(jobResponse);
		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/");
		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/");
		verify(jobServiceMock).hasActiveJob(NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).countDistinctByLabelsMetadataLabelIdIn(Set.of(SOURCE_ID));
		verify(jobServiceMock).create(NAMESPACE, MUNICIPALITY_ID, MERGE_LABELS, 4, TARGET_ID);
		verify(labelMoveTaskExecutorMock).execute(any());
		verify(jobServiceMock).get(NAMESPACE, MUNICIPALITY_ID, "job-id");
	}

	@ParameterizedTest
	@EnumSource(value = Identifier.Type.class, names = {
		"AD_ACCOUNT", "CUSTOM"
	})
	void startLabelMerge_handsTheRunToTheWorkerWithExpectedParameters(final Identifier.Type callerType) {
		var target = leafLabel(TARGET_ID, "TARGET");
		var source = leafLabel(SOURCE_ID, "SOURCE");
		var handled = new ArrayList<LabelMergeRun>();

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/"))
			.thenReturn(false);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(source));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/"))
			.thenReturn(false);
		when(jobServiceMock.hasActiveJob(NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);
		when(errandsRepositoryMock.countDistinctByLabelsMetadataLabelIdIn(Set.of(SOURCE_ID))).thenReturn(0L);
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, MERGE_LABELS, 0, TARGET_ID)).thenReturn("job-id");
		when(jobServiceMock.get(NAMESPACE, MUNICIPALITY_ID, "job-id")).thenReturn(JobResponse.create().withJobId("job-id"));
		doAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(labelMoveTaskExecutorMock).execute(any());
		doAnswer(invocation -> {
			handled.add(invocation.getArgument(0));
			return null;
		}).when(labelMergeWorkerMock).run(any());
		Identifier.set(Identifier.create().withType(callerType).withValue("joe01doe"));

		service.startLabelMerge(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(SOURCE_ID)).withDryRun(false));

		assertThat(handled).hasSize(1);
		assertThat(handled.getFirst().jobId()).isEqualTo("job-id");
		assertThat(handled.getFirst().namespace()).isEqualTo(NAMESPACE);
		assertThat(handled.getFirst().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(handled.getFirst().targetLabelId()).isEqualTo(TARGET_ID);
		assertThat(handled.getFirst().sourceLabelIds()).containsExactly(SOURCE_ID);
		assertThat(handled.getFirst().startedBy()).isEqualTo("joe01doe");
		assertThat(handled.getFirst().startedByAdAccount()).isEqualTo(callerType == Identifier.Type.AD_ACCOUNT);

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/");
		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/");
		verify(jobServiceMock).hasActiveJob(NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).countDistinctByLabelsMetadataLabelIdIn(Set.of(SOURCE_ID));
		verify(jobServiceMock).create(NAMESPACE, MUNICIPALITY_ID, MERGE_LABELS, 0, TARGET_ID);
		verify(labelMoveTaskExecutorMock).execute(any());
		verify(labelMergeWorkerMock).run(any());
		verify(jobServiceMock).get(NAMESPACE, MUNICIPALITY_ID, "job-id");
	}

	@Test
	void startLabelMerge_dispatchRejected_failsJobAndThrows() {
		var target = leafLabel(TARGET_ID, "TARGET");
		var source = leafLabel(SOURCE_ID, "SOURCE");

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(target));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/"))
			.thenReturn(false);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(source));
		when(metadataLabelRepositoryMock.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/"))
			.thenReturn(false);
		when(jobServiceMock.hasActiveJob(NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);
		when(errandsRepositoryMock.countDistinctByLabelsMetadataLabelIdIn(Set.of(SOURCE_ID))).thenReturn(0L);
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, MERGE_LABELS, 0, TARGET_ID)).thenReturn("job-id");
		doThrow(new TaskRejectedException("No thread available")).when(labelMoveTaskExecutorMock).execute(any());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startLabelMerge(NAMESPACE, MUNICIPALITY_ID, TARGET_ID, LabelMergeRequest.create().withSourceLabelIds(List.of(SOURCE_ID)).withDryRun(false)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(INTERNAL_SERVER_ERROR.value()))
			.withMessageContaining("Label merge could not be started: No thread available");

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(TARGET_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "TARGET/");
		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(SOURCE_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOURCE/");
		verify(jobServiceMock).hasActiveJob(NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).countDistinctByLabelsMetadataLabelIdIn(Set.of(SOURCE_ID));
		verify(jobServiceMock).create(NAMESPACE, MUNICIPALITY_ID, MERGE_LABELS, 0, TARGET_ID);
		verify(jobServiceMock).fail("job-id", "Label merge could not be started: No thread available");
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(actionConfigRepositoryMock, metadataLabelRepositoryMock, errandsRepositoryMock, jobServiceMock, labelMergeWorkerMock, labelMoveTaskExecutorMock);
		Identifier.remove();
	}

	private static MetadataLabelEntity leafLabel(final String id, final String resourcePath) {
		return MetadataLabelEntity.create().withId(id).withResourcePath(resourcePath);
	}

	private static ActionConfigEntity actionConfigEntity(final String id, final String name, final String displayValue, final List<ActionConfigConditionEntity> conditions) {
		return ActionConfigEntity.create().withId(id).withName(name).withDisplayValue(displayValue).withConditions(conditions);
	}

	private static ActionConfigConditionEntity conditionEntity(final String key, final List<String> values) {
		return ActionConfigConditionEntity.create().withKey(key).withValues(values);
	}
}
