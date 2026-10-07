package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.ADD;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.DELETE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MERGE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MOVE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.RENAME;

@ExtendWith(MockitoExtension.class)
class LabelTreeRestructureWorkerTest {

	private static final String JOB_ID = randomUUID().toString();
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String STARTED_BY = "joe01doe";

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private LabelMoveWorker labelMoveWorkerMock;

	@Mock
	private LabelMergeWorker labelMergeWorkerMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private PlatformTransactionManager transactionManagerMock;

	@Mock
	private TransactionStatus transactionStatusMock;

	private LabelTreeRestructureWorker worker;

	private LabelTreeRestructureWorker worker() {
		if (worker == null) {
			// Only steps that read-then-write a label (ADD) go through this - lenient so tests exercising other step
			// types alone are not flagged for an unused stub.
			lenient().when(transactionManagerMock.getTransaction(any())).thenReturn(transactionStatusMock);
			worker = new LabelTreeRestructureWorker(metadataLabelRepositoryMock, errandsRepositoryMock, labelMoveWorkerMock, labelMergeWorkerMock, jobServiceMock, transactionManagerMock);
		}
		return worker;
	}

	@Test
	@DisplayName("Verification that every step type is applied directly against the DB in order, MOVE/MERGE delegate to the standalone workers, and progress accumulates across both of them onto the one composite job")
	void run_addRenameDeleteMoveMerge_appliesEachStepAndCompletesJob() {
		final var category = labelEntity("category-id", "CATEGORY");
		final var renameTarget = labelEntity("rename-id", "CATEGORY/RENAME_ME");
		final var deleteTarget = labelEntity("delete-id", "CATEGORY/DELETE_ME");
		final var moveSource = labelEntity("move-id", "CATEGORY/MOVE_ME");
		final var moveDestParent = labelEntity("dest-parent-id", "CATEGORY/DEST");
		final var mergeTarget = labelEntity("merge-target-id", "CATEGORY/MERGE_TARGET");
		final var mergeSource = labelEntity("merge-source-id", "CATEGORY/MERGE_SOURCE");

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/NEW_TYPE")).thenReturn(Optional.empty());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY")).thenReturn(Optional.of(category));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/RENAME_ME")).thenReturn(Optional.of(renameTarget));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/DELETE_ME")).thenReturn(Optional.of(deleteTarget));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/DELETE_ME/")).thenReturn(List.of());
		when(errandsRepositoryMock.existsByLabelsMetadataLabelIdIn(Set.of("delete-id"))).thenReturn(false);
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/MOVE_ME")).thenReturn(Optional.of(moveSource));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/DEST")).thenReturn(Optional.of(moveDestParent));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/MERGE_TARGET")).thenReturn(Optional.of(mergeTarget));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/MERGE_SOURCE")).thenReturn(Optional.of(mergeSource));

		when(labelMoveWorkerMock.moveAndRestow(eq(JOB_ID), eq(MUNICIPALITY_ID), eq("move-id"), eq("dest-parent-id"), isNull(), isNull(), eq(STARTED_BY), eq(true), any()))
			.thenAnswer(invocation -> {
				((IntConsumer) invocation.getArgument(8)).accept(3);
				return 3;
			});
		when(labelMergeWorkerMock.mergeAndRestow(eq(JOB_ID), eq(NAMESPACE), eq(MUNICIPALITY_ID), eq("merge-target-id"), eq(Set.of("merge-source-id")), eq(STARTED_BY), eq(true), any()))
			.thenAnswer(invocation -> {
				((IntConsumer) invocation.getArgument(7)).accept(2);
				return 2;
			});

		final var steps = List.of(
			addStep(List.of("CATEGORY", "NEW_TYPE"), "New type", "TYPE"),
			renameStep(List.of("CATEGORY", "RENAME_ME"), "Renamed"),
			deleteStep(List.of("CATEGORY", "DELETE_ME")),
			moveStep(List.of("CATEGORY", "MOVE_ME"), List.of("CATEGORY", "DEST")),
			mergeStep(List.of("CATEGORY", "MERGE_TARGET"), List.of(List.of("CATEGORY", "MERGE_SOURCE"))));

		worker().run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, steps, STARTED_BY, true));

		verify(jobServiceMock).setRunning(JOB_ID);

		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/NEW_TYPE");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY");
		verify(metadataLabelRepositoryMock).save(argThat(saved -> "NEW_TYPE".equals(saved.getResourceName()) && saved.getParent() == category && "New type".equals(saved.getDisplayName())));

		assertThat(renameTarget.getDisplayName()).isEqualTo("Renamed");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/RENAME_ME");
		verify(metadataLabelRepositoryMock).save(renameTarget);

		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/DELETE_ME");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/DELETE_ME/");
		verify(errandsRepositoryMock).existsByLabelsMetadataLabelIdIn(Set.of("delete-id"));
		verify(metadataLabelRepositoryMock).deleteById("delete-id");

		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/MOVE_ME");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/DEST");
		verify(labelMoveWorkerMock).moveAndRestow(eq(JOB_ID), eq(MUNICIPALITY_ID), eq("move-id"), eq("dest-parent-id"), isNull(), isNull(), eq(STARTED_BY), eq(true), any());
		verify(jobServiceMock).updateProgress(JOB_ID, 3);

		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/MERGE_TARGET");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/MERGE_SOURCE");
		verify(labelMergeWorkerMock).mergeAndRestow(eq(JOB_ID), eq(NAMESPACE), eq(MUNICIPALITY_ID), eq("merge-target-id"), eq(Set.of("merge-source-id")), eq(STARTED_BY), eq(true), any());
		// Cumulative across both restow-reporting steps (3 from the move, then +2 from the merge), onto the one composite job.
		verify(jobServiceMock).updateProgress(JOB_ID, 5);

		verify(jobServiceMock).complete(eq(JOB_ID), argThat(message -> message.contains("5 step(s) applied") && message.contains("5 errand(s) restowed")));
	}

	@Test
	@DisplayName("Verification that a failing step stops the run there - later steps are never attempted - and the job is failed with a message naming which step failed")
	void run_stepFails_stopsAndFailsJobNamingTheStep() {
		final var category = labelEntity("category-id", "CATEGORY");

		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/NEW_TYPE")).thenReturn(Optional.empty());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY")).thenReturn(Optional.of(category));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/GONE")).thenReturn(Optional.empty());

		final var steps = List.of(
			addStep(List.of("CATEGORY", "NEW_TYPE"), "New type", "TYPE"),
			renameStep(List.of("CATEGORY", "GONE"), "Renamed"),
			moveStep(List.of("CATEGORY", "SHOULD_NOT_RUN"), List.of()));

		worker().run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, steps, STARTED_BY, true));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/NEW_TYPE");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY");
		verify(metadataLabelRepositoryMock).save(any());
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/GONE");
		verify(jobServiceMock).fail(eq(JOB_ID), argThat(message -> message.startsWith("Label tree restructure aborted: Step 1 (RENAME) failed") && message.contains("no longer exists")));

		verify(metadataLabelRepositoryMock, never()).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/SHOULD_NOT_RUN");
		verifyNoInteractions(labelMoveWorkerMock, labelMergeWorkerMock);
	}

	@Test
	@DisplayName("Verification that ADD is a no-op when the path already exists, so a resubmitted request is safe")
	void run_addAlreadyPresent_skipsWithoutSaving() {
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/EXISTING"))
			.thenReturn(Optional.of(labelEntity("existing-id", "CATEGORY/EXISTING")));

		worker().run(new LabelRestructureRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, List.of(addStep(List.of("CATEGORY", "EXISTING"), "Existing", "TYPE")), STARTED_BY, true));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CATEGORY/EXISTING");
		verify(metadataLabelRepositoryMock, never()).save(any());
		verify(jobServiceMock).complete(eq(JOB_ID), any());
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(metadataLabelRepositoryMock, errandsRepositoryMock, labelMoveWorkerMock, labelMergeWorkerMock, jobServiceMock);
	}

	private static MetadataLabelEntity labelEntity(final String id, final String resourcePath) {
		final var resourceName = resourcePath.contains("/") ? resourcePath.substring(resourcePath.lastIndexOf('/') + 1) : resourcePath;
		return MetadataLabelEntity.create().withId(id).withResourceName(resourceName).withResourcePath(resourcePath);
	}

	private static LabelRestructureStep addStep(final List<String> path, final String displayName, final String classification) {
		return LabelRestructureStep.create().withType(ADD).withPath(path).withDisplayName(displayName).withClassification(classification);
	}

	private static LabelRestructureStep renameStep(final List<String> path, final String displayName) {
		return LabelRestructureStep.create().withType(RENAME).withPath(path).withDisplayName(displayName);
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
