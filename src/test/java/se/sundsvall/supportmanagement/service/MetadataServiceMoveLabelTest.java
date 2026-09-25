package se.sundsvall.supportmanagement.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import se.sundsvall.supportmanagement.api.model.metadata.LabelMoveRequest;
import se.sundsvall.supportmanagement.config.JobProperties;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.MOVE_LABEL;

@ExtendWith(MockitoExtension.class)
class MetadataServiceMoveLabelTest {

	private static final String NAMESPACE = "NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String LABEL_ID = "label-id";
	private static final String PARENT_ID = "parent-id";
	private static final String NEW_PARENT_ID = "new-parent-id";
	// MetadataService reads JobProperties#staleAfter() once, in its constructor - @InjectMocks builds that constructor
	// before this test class's own @BeforeEach ever runs, so jobPropertiesMock.staleAfter() is still unstubbed at that
	// point and Mockito's default answer for Duration (Duration.ZERO) is what actually gets captured. Stubbing it
	// afterward in @BeforeEach would have no effect on the already-constructed service, so tests stub
	// stealStaleLease(...) against this same Duration.ZERO rather than a value that was never really in play.
	private static final Duration STALE_AFTER = Duration.ZERO;

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
	private AsyncTaskExecutor labelMoveTaskExecutorMock;

	@Mock
	private PlatformTransactionManager transactionManagerMock;

	@Mock
	private TransactionStatus transactionStatusMock;

	@Mock
	private JobProperties jobPropertiesMock;

	@InjectMocks
	private MetadataService service;

	@BeforeEach
	void setUpTransactionManager() {
		// Only startLabelMove goes through readOnlyTransactionTemplate - lenient so moveLabel-only tests, which never
		// touch it, are not flagged for an unused stub.
		lenient().when(transactionManagerMock.getTransaction(any())).thenReturn(transactionStatusMock);
		// jobPropertiesMock.staleAfter() is NOT stubbed here on purpose - see STALE_AFTER's own comment for why that
		// would be too late to matter.
	}

	@Test
	void moveLabel_labelNotFound_throws404() {
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, LabelMoveRequest.create().withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(NOT_FOUND.value()));

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void moveLabel_newParentNotFound_throws400() {
		var label = labelEntity(LABEL_ID, "ROOT", null);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(NEW_PARENT_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID,
				LabelMoveRequest.create().withNewParentId(NEW_PARENT_ID).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()));
	}

	@Test
	void moveLabel_noOp_sameParent_throws400() {
		var parent = labelEntity(PARENT_ID, "PARENT", null);
		var label = labelEntityWithParent(LABEL_ID, "CHILD", "PARENT/CHILD", parent);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(PARENT_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(parent));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID,
				LabelMoveRequest.create().withNewParentId(PARENT_ID).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("no-op");
	}

	@Test
	void moveLabel_noOp_bothNull_throws400() {
		var label = labelEntity(LABEL_ID, "ROOT", null);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID,
				LabelMoveRequest.create().withNewParentId(null).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("no-op");
	}

	@Test
	void moveLabel_cycle_newParentIsDescendant_throws400() {
		var label = labelEntity(LABEL_ID, "ROOT", null);
		// newParent has label as its ancestor — cycle
		var newParent = labelEntityWithParent(NEW_PARENT_ID, "CHILD", "ROOT/CHILD", label);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(NEW_PARENT_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(newParent));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID,
				LabelMoveRequest.create().withNewParentId(NEW_PARENT_ID).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("cycle");
	}

	@Test
	@DisplayName("Verification that a cycle is still caught when the client's labelId differs in case from how it is stored, since the check must compare against the canonical id on both sides")
	void moveLabel_cycle_detectedRegardlessOfLabelIdCasing_throws400() {
		var differentlyCasedLabelId = LABEL_ID.toUpperCase();
		var label = labelEntity(LABEL_ID, "ROOT", "ROOT");
		// newParent has label as its ancestor — cycle
		var newParent = labelEntityWithParent(NEW_PARENT_ID, "CHILD", "ROOT/CHILD", label);
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(differentlyCasedLabelId, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(NEW_PARENT_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(newParent));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.moveLabel(NAMESPACE, MUNICIPALITY_ID, differentlyCasedLabelId,
				LabelMoveRequest.create().withNewParentId(NEW_PARENT_ID).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("cycle");
	}

	@Test
	void moveLabel_pathCollision_throws409() {
		var label = labelEntity(LABEL_ID, "CHILD", "SOME/CHILD");
		var newParent = labelEntity(NEW_PARENT_ID, "TARGET", "TARGET");
		var collision = labelEntity("other-id", "CHILD", "TARGET/CHILD");

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(NEW_PARENT_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(newParent));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "TARGET/CHILD"))
			.thenReturn(Optional.of(collision));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID,
				LabelMoveRequest.create().withNewParentId(NEW_PARENT_ID).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(CONFLICT.value()));
	}

	@Test
	@DisplayName("Verification that a move is rejected when the moved label's own resulting path would not fit the resource_path column")
	void moveLabel_resultingPathTooLong_throws400() {
		var longParentPath = "A".repeat(250);
		var label = labelEntity(LABEL_ID, "CHILD", "SOME/CHILD");
		var newParent = labelEntity(NEW_PARENT_ID, "TARGET", longParentPath);

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(NEW_PARENT_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(newParent));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, longParentPath + "/CHILD"))
			.thenReturn(Optional.empty());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOME/CHILD/"))
			.thenReturn(List.of());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID,
				LabelMoveRequest.create().withNewParentId(NEW_PARENT_ID).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("exceeds the maximum");
	}

	@Test
	@DisplayName("Verification that a move is rejected when it is a descendant's resulting path, not the moved label's own, that would not fit the resource_path column")
	void moveLabel_descendantResultingPathTooLong_throws400() {
		var label = labelEntity(LABEL_ID, "CHILD", "SOME/CHILD");
		var newParent = labelEntity(NEW_PARENT_ID, "TARGET", "TARGET");
		var longDescendantSuffix = "B".repeat(250);
		var descendant = labelEntity("descendant-id", "LEAF", "SOME/CHILD/" + longDescendantSuffix);

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(NEW_PARENT_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(newParent));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "TARGET/CHILD"))
			.thenReturn(Optional.empty());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOME/CHILD/"))
			.thenReturn(List.of(descendant));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "TARGET/CHILD/" + longDescendantSuffix))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID,
				LabelMoveRequest.create().withNewParentId(NEW_PARENT_ID).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(BAD_REQUEST.value()))
			.withMessageContaining("exceeds the maximum");
	}

	@Test
	@DisplayName("Verification that a move is rejected when it is a descendant's resulting path, not the moved label's own, that would collide with an existing label")
	void moveLabel_descendantPathCollision_throws409() {
		var label = labelEntity(LABEL_ID, "CHILD", "SOME/CHILD");
		var newParent = labelEntity(NEW_PARENT_ID, "TARGET", "TARGET");
		var descendant = labelEntity("descendant-id", "LEAF", "SOME/CHILD/LEAF");
		var collision = labelEntity("other-id", "LEAF", "TARGET/CHILD/LEAF");

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(NEW_PARENT_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(newParent));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "TARGET/CHILD"))
			.thenReturn(Optional.empty());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "SOME/CHILD/"))
			.thenReturn(List.of(descendant));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "TARGET/CHILD/LEAF"))
			.thenReturn(Optional.of(collision));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID,
				LabelMoveRequest.create().withNewParentId(NEW_PARENT_ID).withDryRun(true)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(CONFLICT.value()))
			.withMessageContaining("TARGET/CHILD/LEAF");
	}

	@Test
	void moveLabel_toRoot_dryRun_returnsCountAndActions() {
		var label = labelEntityWithParent(LABEL_ID, "CHILD", "PARENT/CHILD", labelEntity(PARENT_ID, "PARENT", null));
		var actionWithLabel = actionConfigEntity("action-id", "ACTION", "Display",
			List.of(conditionEntity("hasLabel", List.of(LABEL_ID))));
		var actionWithoutLabel = actionConfigEntity("other-action", "OTHER", null, List.of());

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD"))
			.thenReturn(Optional.empty());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "PARENT/CHILD/"))
			.thenReturn(List.of());
		when(errandsRepositoryMock.findDistinctIdsByLabelsMetadataLabelIdIn(Set.of(LABEL_ID))).thenReturn(List.of("errand-1", "errand-2", "errand-3"));
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(List.of(actionWithLabel, actionWithoutLabel));

		var result = service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, LabelMoveRequest.create().withDryRun(true));

		assertThat(result.getAffectedErrandCount()).isEqualTo(3L);
		assertThat(result.getAffectedActions()).hasSize(1)
			.first()
			.satisfies(a -> {
				assertThat(a.getId()).isEqualTo("action-id");
				assertThat(a.getName()).isEqualTo("ACTION");
				assertThat(a.getDisplayValue()).isEqualTo("Display");
			});
	}

	@Test
	@DisplayName("Verification that moving a label with descendants counts errands reached through the whole subtree, not only ones tagged with the moved label itself")
	void moveLabel_withDescendants_countsAcrossSubtree() {
		var child = labelEntity("child-id", "CHILD", "ROOT/CHILD");
		var label = labelEntity(LABEL_ID, "ROOT", "ROOT");

		var newParent = labelEntity(NEW_PARENT_ID, "TARGET", "TARGET");

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(NEW_PARENT_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(newParent));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "TARGET/ROOT"))
			.thenReturn(Optional.empty());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "ROOT/"))
			.thenReturn(List.of(child));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "TARGET/ROOT/CHILD"))
			.thenReturn(Optional.empty());
		// An errand tagged only with the descendant - not the moved label itself - must still be counted
		when(errandsRepositoryMock.findDistinctIdsByLabelsMetadataLabelIdIn(Set.of(LABEL_ID, "child-id")))
			.thenReturn(List.of("errand-1", "errand-2", "errand-3", "errand-4", "errand-5"));
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(List.of());

		var result = service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID,
			LabelMoveRequest.create().withNewParentId(NEW_PARENT_ID).withDryRun(true));

		assertThat(result.getAffectedErrandCount()).isEqualTo(5L);
		assertThat(result.getAffectedActions()).isEmpty();
	}

	@Test
	@DisplayName("Verification that a namespace already worked on by a pending or running move is not handed a second job for the same label, since two runs would race on the same errands")
	void startLabelMove_activeJobInNamespace_throws409() {
		var label = labelEntityWithParent(LABEL_ID, "CHILD", "PARENT/CHILD", labelEntity(PARENT_ID, "PARENT", null));

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD"))
			.thenReturn(Optional.empty());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "PARENT/CHILD/"))
			.thenReturn(List.of());
		when(jobServiceMock.stealStaleLease(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, STALE_AFTER)).thenReturn(false);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startLabelMove(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, LabelMoveRequest.create().withDryRun(false)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(CONFLICT.value()));

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "PARENT/CHILD/");
		verify(jobServiceMock).stealStaleLease(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, STALE_AFTER);
		verify(errandsRepositoryMock, never()).findDistinctIdsByLabelsMetadataLabelIdIn(any());
	}

	@Test
	@DisplayName("Verification that the guard also refuses a move of a label whose ancestor or descendant a pending move already targets, since the namespace-wide check makes overlapping subtrees safe without having to compare id sets")
	void startLabelMove_activeJobOnUnrelatedLabelInSameNamespace_throws409() {
		// A different label entirely (not an ancestor or descendant of LABEL_ID by id) - the guard must still refuse,
		// since it no longer compares label ids at all once a job is active anywhere in the namespace.
		var label = labelEntityWithParent(LABEL_ID, "CHILD", "PARENT/CHILD", labelEntity(PARENT_ID, "PARENT", null));

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD"))
			.thenReturn(Optional.empty());
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "PARENT/CHILD/"))
			.thenReturn(List.of());
		when(jobServiceMock.stealStaleLease(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, STALE_AFTER)).thenReturn(false);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startLabelMove(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, LabelMoveRequest.create().withDryRun(false)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(CONFLICT.value()))
			.withMessageContaining(NAMESPACE)
			.withMessageContaining(MUNICIPALITY_ID);

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "PARENT/CHILD/");
		verify(jobServiceMock).stealStaleLease(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, STALE_AFTER);
		verify(errandsRepositoryMock, never()).findDistinctIdsByLabelsMetadataLabelIdIn(any());
	}

	@Test
	void startLabelMove_labelNotFound_throws404() {
		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startLabelMove(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, LabelMoveRequest.create().withDryRun(false)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(NOT_FOUND.value()));

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	@DisplayName("Verification that the accepted job's response carries the same affectedActions a dry-run would have reported, so an admin who skips straight to a real move still learns which actions are affected")
	void startLabelMove_createsJobAndReturnsJobResponse() {
		var label = labelEntityWithParent(LABEL_ID, "CHILD", "PARENT/CHILD", labelEntity(PARENT_ID, "PARENT", null));
		var jobResponse = JobResponse.create().withJobId("job-id").withType(MOVE_LABEL).withStatus(JobStatus.PENDING).withTotal(3);
		var actionWithLabel = actionConfigEntity("action-id", "ACTION", "Display", List.of(conditionEntity("hasLabel", List.of(LABEL_ID))));

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD"))
			.thenReturn(Optional.empty());
		when(jobServiceMock.stealStaleLease(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, STALE_AFTER)).thenReturn(true);
		when(errandsRepositoryMock.findDistinctIdsByLabelsMetadataLabelIdIn(Set.of(LABEL_ID))).thenReturn(List.of("errand-1", "errand-2", "errand-3"));
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(actionWithLabel));
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, 3, LABEL_ID)).thenReturn("job-id");
		when(jobServiceMock.get(NAMESPACE, MUNICIPALITY_ID, "job-id")).thenReturn(jobResponse);

		var result = service.startLabelMove(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, LabelMoveRequest.create().withDryRun(false));

		assertThat(result).isSameAs(jobResponse);
		assertThat(result.getAffectedActions()).hasSize(1).first().satisfies(a -> assertThat(a.getId()).isEqualTo("action-id"));
		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "PARENT/CHILD/");
		verify(jobServiceMock).stealStaleLease(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, STALE_AFTER);
		verify(errandsRepositoryMock).findDistinctIdsByLabelsMetadataLabelIdIn(Set.of(LABEL_ID));
		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(jobServiceMock).create(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, 3, LABEL_ID);
		verify(labelMoveTaskExecutorMock).execute(any());
		verify(jobServiceMock).get(NAMESPACE, MUNICIPALITY_ID, "job-id");
	}

	@Test
	void startLabelMove_handsTheRunToTheWorkerWithExpectedParameters() {
		var label = labelEntityWithParent(LABEL_ID, "CHILD", "PARENT/CHILD", labelEntity(PARENT_ID, "PARENT", null));
		var handled = new ArrayList<LabelMoveRun>();

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD"))
			.thenReturn(Optional.empty());
		when(jobServiceMock.stealStaleLease(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, STALE_AFTER)).thenReturn(true);
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "PARENT/CHILD/"))
			.thenReturn(List.of());
		when(errandsRepositoryMock.findDistinctIdsByLabelsMetadataLabelIdIn(Set.of(LABEL_ID))).thenReturn(List.of());
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, 0, LABEL_ID)).thenReturn("job-id");
		when(jobServiceMock.get(NAMESPACE, MUNICIPALITY_ID, "job-id")).thenReturn(JobResponse.create().withJobId("job-id"));
		doAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(labelMoveTaskExecutorMock).execute(any());
		doAnswer(invocation -> {
			handled.add(invocation.getArgument(0));
			return null;
		}).when(labelMoveWorkerMock).run(any());
		var identifier = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe");
		Identifier.set(identifier);

		service.startLabelMove(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, LabelMoveRequest.create().withDryRun(false));

		assertThat(handled).hasSize(1);
		assertThat(handled.getFirst().jobId()).isEqualTo("job-id");
		assertThat(handled.getFirst().namespace()).isEqualTo(NAMESPACE);
		assertThat(handled.getFirst().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(handled.getFirst().labelId()).isEqualTo(LABEL_ID);
		assertThat(handled.getFirst().newParentId()).isNull();
		assertThat(handled.getFirst().errandIds()).isEmpty();
		// The whole identifier (type and value), not just the value - see startedBy()'s own doc comment for why.
		assertThat(handled.getFirst().startedBy()).isEqualTo(identifier.toHeaderValue());

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "PARENT/CHILD/");
		verify(jobServiceMock).stealStaleLease(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, STALE_AFTER);
		verify(errandsRepositoryMock).findDistinctIdsByLabelsMetadataLabelIdIn(Set.of(LABEL_ID));
		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(jobServiceMock).create(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, 0, LABEL_ID);
		verify(labelMoveTaskExecutorMock).execute(any());
		verify(labelMoveWorkerMock).run(any());
		verify(jobServiceMock).get(NAMESPACE, MUNICIPALITY_ID, "job-id");
	}

	@Test
	void startLabelMove_dispatchRejected_failsJobAndThrows() {
		var label = labelEntityWithParent(LABEL_ID, "CHILD", "PARENT/CHILD", labelEntity(PARENT_ID, "PARENT", null));

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD"))
			.thenReturn(Optional.empty());
		when(jobServiceMock.stealStaleLease(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, STALE_AFTER)).thenReturn(true);
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "PARENT/CHILD/"))
			.thenReturn(List.of());
		when(errandsRepositoryMock.findDistinctIdsByLabelsMetadataLabelIdIn(Set.of(LABEL_ID))).thenReturn(List.of());
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, 0, LABEL_ID)).thenReturn("job-id");
		doThrow(new TaskRejectedException("No thread available")).when(labelMoveTaskExecutorMock).execute(any());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startLabelMove(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, LabelMoveRequest.create().withDryRun(false)))
			.satisfies(p -> assertThat(p.getStatus().value()).isEqualTo(INTERNAL_SERVER_ERROR.value()))
			.withMessageContaining("Label move could not be started: No thread available");

		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD");
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(NAMESPACE, MUNICIPALITY_ID, "PARENT/CHILD/");
		verify(jobServiceMock).stealStaleLease(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, STALE_AFTER);
		verify(errandsRepositoryMock).findDistinctIdsByLabelsMetadataLabelIdIn(Set.of(LABEL_ID));
		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(jobServiceMock).create(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, 0, LABEL_ID);
		verify(jobServiceMock).fail("job-id", "Label move could not be started: No thread available");
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(actionConfigRepositoryMock, metadataLabelRepositoryMock, errandsRepositoryMock, jobServiceMock, labelMoveWorkerMock, labelMoveTaskExecutorMock);
		Identifier.remove();
	}

	private static MetadataLabelEntity labelEntity(final String id, final String resourceName, final String resourcePath) {
		return MetadataLabelEntity.create().withId(id).withResourceName(resourceName).withResourcePath(resourcePath);
	}

	private static MetadataLabelEntity labelEntityWithParent(final String id, final String resourceName, final String resourcePath, final MetadataLabelEntity parent) {
		return MetadataLabelEntity.create().withId(id).withResourceName(resourceName).withResourcePath(resourcePath).withParent(parent);
	}

	private static ActionConfigEntity actionConfigEntity(final String id, final String name, final String displayValue, final List<ActionConfigConditionEntity> conditions) {
		return ActionConfigEntity.create().withId(id).withName(name).withDisplayValue(displayValue).withConditions(conditions);
	}

	private static ActionConfigConditionEntity conditionEntity(final String key, final List<String> values) {
		return ActionConfigConditionEntity.create().withKey(key).withValues(values);
	}
}
