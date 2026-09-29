package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureRequest;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;
import se.sundsvall.supportmanagement.integration.db.ActionConfigRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.ADD;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.DELETE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MERGE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MOVE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.RESTRUCTURE_LABEL_TREE;

@ExtendWith(MockitoExtension.class)
class MetadataServiceRestructureLabelTreeTest {

	private static final String NAMESPACE = "NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ActionConfigRepository actionConfigRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private LabelTreeRestructureWorker labelTreeRestructureWorkerMock;

	@Mock
	private AsyncTaskExecutor labelMoveTaskExecutorMock;

	@InjectMocks
	private MetadataService service;

	// =================================================================
	// restructureLabelTree (dry-run)
	// =================================================================

	@Test
	@DisplayName("Verification that a MOVE step's destinationParentPath resolves against a label an earlier ADD step in the same request just created")
	void restructureLabelTree_moveReferencesLabelAddedByEarlierStep_computesAffectedCounts() {
		final var social = labelEntity("social-id", "SOCIAL_SERVICES");
		final var elderly = labelEntity("elderly-id", "SOCIAL_SERVICES/ELDERLY_CARE");
		final var support = labelEntity("support-id", "SOCIAL_SERVICES/SUPPORT_CARE");
		final var oldSubtype = labelEntity("old-id", "SOCIAL_SERVICES/SUPPORT_CARE/OLD_SUBTYPE");

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(List.of(social, elderly, support, oldSubtype));
		when(errandsRepositoryMock.countDistinctByLabelsMetadataLabelIdIn(Set.of("old-id"))).thenReturn(7L);
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());

		final var request = LabelRestructureRequest.create()
			.withDryRun(true)
			.withSteps(List.of(
				addStep(List.of("SOCIAL_SERVICES", "ELDERLY_CARE", "SPECIAL_HOUSING"), "Särskilt boende", "SUBTYPE"),
				moveStep(List.of("SOCIAL_SERVICES", "SUPPORT_CARE", "OLD_SUBTYPE"), List.of("SOCIAL_SERVICES", "ELDERLY_CARE", "SPECIAL_HOUSING"))));

		final var result = service.restructureLabelTree(NAMESPACE, MUNICIPALITY_ID, request);

		assertThat(result.getTotalAffectedErrandCount()).isEqualTo(7L);
		assertThat(result.getSteps()).hasSize(2);
		assertThat(result.getSteps().get(0).getAffectedErrandCount()).isZero();
		assertThat(result.getSteps().get(1).getAffectedErrandCount()).isEqualTo(7L);

		verify(errandsRepositoryMock).countDistinctByLabelsMetadataLabelIdIn(Set.of("old-id"));
		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void restructureLabelTree_addWithMissingParent_throws400() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());

		final var request = LabelRestructureRequest.create()
			.withDryRun(true)
			.withSteps(List.of(addStep(List.of("MISSING_PARENT", "NEW_TYPE"), "New type", "TYPE")));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.restructureLabelTree(NAMESPACE, MUNICIPALITY_ID, request))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()));
	}

	@Test
	@DisplayName("Verification that ADD is a no-op, not an error, when the path already exists - a resubmitted request must be safe")
	void restructureLabelTree_addAlreadyPresent_isNoOp() {
		final var existing = labelEntity("existing-id", "CATEGORY/TYPE");

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(existing));

		final var request = LabelRestructureRequest.create()
			.withDryRun(true)
			.withSteps(List.of(addStep(List.of("CATEGORY", "TYPE"), "Type", "TYPE")));

		final var result = service.restructureLabelTree(NAMESPACE, MUNICIPALITY_ID, request);

		assertThat(result.getTotalAffectedErrandCount()).isZero();
	}

	@Test
	void restructureLabelTree_moveOfUnknownLabel_throws404() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());

		final var request = LabelRestructureRequest.create()
			.withDryRun(true)
			.withSteps(List.of(moveStep(List.of("GONE"), List.of())));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.restructureLabelTree(NAMESPACE, MUNICIPALITY_ID, request))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(NOT_FOUND.value()));

		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void restructureLabelTree_moveToUnknownDestination_throws400() {
		final var label = labelEntity("label-id", "CATEGORY");

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(label));

		final var request = LabelRestructureRequest.create()
			.withDryRun(true)
			.withSteps(List.of(moveStep(List.of("CATEGORY"), List.of("MISSING_DESTINATION"))));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.restructureLabelTree(NAMESPACE, MUNICIPALITY_ID, request))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()));

		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void restructureLabelTree_deleteReferencedByErrand_throws400() {
		final var label = labelEntity("label-id", "CATEGORY/TYPE");

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(label));
		when(errandsRepositoryMock.existsByLabelsMetadataLabelIdIn(Set.of("label-id"))).thenReturn(true);

		final var request = LabelRestructureRequest.create()
			.withDryRun(true)
			.withSteps(List.of(deleteStep(List.of("CATEGORY", "TYPE"))));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.restructureLabelTree(NAMESPACE, MUNICIPALITY_ID, request))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("referenced");
	}

	@Test
	void restructureLabelTree_deleteWithChildren_throws400() {
		final var parent = labelEntity("parent-id", "CATEGORY/TYPE");
		final var child = labelEntity("child-id", "CATEGORY/TYPE/SUBTYPE");

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(parent, child));

		final var request = LabelRestructureRequest.create()
			.withDryRun(true)
			.withSteps(List.of(deleteStep(List.of("CATEGORY", "TYPE"))));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.restructureLabelTree(NAMESPACE, MUNICIPALITY_ID, request))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("children");
	}

	@Test
	void restructureLabelTree_mergeSourceWithChildren_throws400() {
		final var target = labelEntity("target-id", "CATEGORY/TARGET");
		final var source = labelEntity("source-id", "CATEGORY/SOURCE");
		final var sourceChild = labelEntity("source-child-id", "CATEGORY/SOURCE/CHILD");

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(List.of(target, source, sourceChild));

		final var request = LabelRestructureRequest.create()
			.withDryRun(true)
			.withSteps(List.of(mergeStep(List.of("CATEGORY", "TARGET"), List.of(List.of("CATEGORY", "SOURCE")))));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.restructureLabelTree(NAMESPACE, MUNICIPALITY_ID, request))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("children");

		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void restructureLabelTree_moveWouldCreateCycle_throws400() {
		final var parent = labelEntity("parent-id", "CATEGORY");
		final var child = labelEntity("child-id", "CATEGORY/CHILD");

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(parent, child));

		final var request = LabelRestructureRequest.create()
			.withDryRun(true)
			.withSteps(List.of(moveStep(List.of("CATEGORY"), List.of("CATEGORY", "CHILD"))));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.restructureLabelTree(NAMESPACE, MUNICIPALITY_ID, request))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("cycle");

		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	@DisplayName("Verification that a MOVE step rejects a descendant's rebased path colliding with an existing unrelated label, not just the moved label's own new path")
	void restructureLabelTree_moveDescendantPathCollision_throws409() {
		final var destination = labelEntity("destination-id", "DESTINATION");
		final var category = labelEntity("category-id", "CATEGORY");
		final var child = labelEntity("child-id", "CATEGORY/CHILD");
		final var colliding = labelEntity("colliding-id", "DESTINATION/CATEGORY/CHILD");

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(List.of(destination, category, child, colliding));

		final var request = LabelRestructureRequest.create()
			.withDryRun(true)
			.withSteps(List.of(moveStep(List.of("CATEGORY"), List.of("DESTINATION"))));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.restructureLabelTree(NAMESPACE, MUNICIPALITY_ID, request))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(CONFLICT.value()))
			.withMessageContaining("DESTINATION/CATEGORY/CHILD");

		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
	}

	// =================================================================
	// startLabelTreeRestructure
	// =================================================================

	@Test
	@DisplayName("Verification that the guard is the type-agnostic hasActiveJob overload, so a restructure is refused alongside a lone MOVE_LABEL/MERGE_LABELS job too, not just another restructure")
	void startLabelTreeRestructure_activeJobInNamespace_throws409() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());
		when(jobServiceMock.hasActiveJob(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		final var request = LabelRestructureRequest.create().withDryRun(false).withSteps(List.of(addStep(List.of("CATEGORY"), "Category", "CATEGORY")));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startLabelTreeRestructure(NAMESPACE, MUNICIPALITY_ID, request))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(CONFLICT.value()));

		verify(jobServiceMock).hasActiveJob(NAMESPACE, MUNICIPALITY_ID);
		verify(jobServiceMock, never()).create(any(), any(), any(), anyInt());
	}

	@Test
	void startLabelTreeRestructure_handsTheRunToTheWorkerWithExpectedParameters() {
		final var handled = new ArrayList<LabelRestructureRun>();
		final var jobResponse = JobResponse.create().withJobId("job-id").withType(RESTRUCTURE_LABEL_TREE).withStatus(JobStatus.PENDING);

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());
		when(jobServiceMock.hasActiveJob(NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, RESTRUCTURE_LABEL_TREE, 0)).thenReturn("job-id");
		when(jobServiceMock.get(NAMESPACE, MUNICIPALITY_ID, "job-id")).thenReturn(jobResponse);
		doAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(labelMoveTaskExecutorMock).execute(any());
		doAnswer(invocation -> {
			handled.add(invocation.getArgument(0));
			return null;
		}).when(labelTreeRestructureWorkerMock).run(any());
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe"));

		final var steps = List.of(addStep(List.of("CATEGORY"), "Category", "CATEGORY"));
		final var result = service.startLabelTreeRestructure(NAMESPACE, MUNICIPALITY_ID, LabelRestructureRequest.create().withDryRun(false).withSteps(steps));

		assertThat(result).isEqualTo(jobResponse);
		assertThat(handled).hasSize(1);
		assertThat(handled.getFirst().jobId()).isEqualTo("job-id");
		assertThat(handled.getFirst().namespace()).isEqualTo(NAMESPACE);
		assertThat(handled.getFirst().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(handled.getFirst().steps()).isEqualTo(steps);
		assertThat(handled.getFirst().startedBy()).isEqualTo("joe01doe");

		verify(jobServiceMock).hasActiveJob(NAMESPACE, MUNICIPALITY_ID);
		verify(jobServiceMock).create(NAMESPACE, MUNICIPALITY_ID, RESTRUCTURE_LABEL_TREE, 0);
		verify(labelMoveTaskExecutorMock).execute(any());
		verify(labelTreeRestructureWorkerMock).run(any());
		verify(jobServiceMock).get(NAMESPACE, MUNICIPALITY_ID, "job-id");
	}

	@Test
	void startLabelTreeRestructure_dispatchRejected_failsJobAndThrows() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());
		when(jobServiceMock.hasActiveJob(NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, RESTRUCTURE_LABEL_TREE, 0)).thenReturn("job-id");
		doThrow(new TaskRejectedException("No thread available")).when(labelMoveTaskExecutorMock).execute(any());

		final var request = LabelRestructureRequest.create().withDryRun(false).withSteps(List.of(addStep(List.of("CATEGORY"), "Category", "CATEGORY")));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startLabelTreeRestructure(NAMESPACE, MUNICIPALITY_ID, request))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(INTERNAL_SERVER_ERROR.value()))
			.withMessageContaining("Label tree restructure could not be started: No thread available");

		verify(jobServiceMock).fail("job-id", "Label tree restructure could not be started: No thread available");
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(actionConfigRepositoryMock, metadataLabelRepositoryMock, errandsRepositoryMock, jobServiceMock, labelTreeRestructureWorkerMock, labelMoveTaskExecutorMock);
		Identifier.remove();
	}

	private static MetadataLabelEntity labelEntity(final String id, final String resourcePath) {
		final var resourceName = resourcePath.contains("/") ? resourcePath.substring(resourcePath.lastIndexOf('/') + 1) : resourcePath;
		return MetadataLabelEntity.create().withId(id).withResourceName(resourceName).withResourcePath(resourcePath);
	}

	private static LabelRestructureStep addStep(final List<String> path, final String displayName, final String classification) {
		return LabelRestructureStep.create().withType(ADD).withPath(path).withDisplayName(displayName).withClassification(classification);
	}

	private static LabelRestructureStep deleteStep(final List<String> path) {
		return LabelRestructureStep.create().withType(DELETE).withPath(path);
	}

	private static LabelRestructureStep moveStep(final List<String> path, final List<String> destinationParentPath) {
		return LabelRestructureStep.create().withType(MOVE).withPath(path).withDestinationParentPath(destinationParentPath);
	}

	private static LabelRestructureStep mergeStep(final List<String> path, final List<List<String>> sourcePaths) {
		return LabelRestructureStep.create().withType(MERGE).withPath(path).withSourcePaths(sourcePaths);
	}
}
