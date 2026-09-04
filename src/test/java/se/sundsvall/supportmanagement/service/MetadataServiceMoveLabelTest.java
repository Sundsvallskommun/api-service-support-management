package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.api.model.metadata.LabelMoveRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.MOVE_LABEL;

@ExtendWith(MockitoExtension.class)
class MetadataServiceMoveLabelTest {

	private static final String NAMESPACE = "NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String LABEL_ID = "label-id";
	private static final String PARENT_ID = "parent-id";
	private static final String NEW_PARENT_ID = "new-parent-id";

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

	@InjectMocks
	private MetadataService service;

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
		when(errandsRepositoryMock.countByLabelsMetadataLabelId(LABEL_ID)).thenReturn(3L);
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
	void moveLabel_withDescendants_countsByLabelIdOnly() {
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
		when(errandsRepositoryMock.countByLabelsMetadataLabelId(LABEL_ID)).thenReturn(5L);
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(List.of());

		var result = service.moveLabel(NAMESPACE, MUNICIPALITY_ID, LABEL_ID,
			LabelMoveRequest.create().withNewParentId(NEW_PARENT_ID).withDryRun(true));

		assertThat(result.getAffectedErrandCount()).isEqualTo(5L);
		assertThat(result.getAffectedActions()).isEmpty();
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
	void startLabelMove_createsJobAndReturnsJobResponse() {
		var label = labelEntityWithParent(LABEL_ID, "CHILD", "PARENT/CHILD", labelEntity(PARENT_ID, "PARENT", null));
		var jobResponse = JobResponse.create().withJobId("job-id").withType(MOVE_LABEL).withStatus(JobStatus.PENDING).withTotal(3);

		when(metadataLabelRepositoryMock.findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(label));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD"))
			.thenReturn(Optional.empty());
		when(errandsRepositoryMock.countByLabelsMetadataLabelId(LABEL_ID)).thenReturn(3L);
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, 3)).thenReturn("job-id");
		when(jobServiceMock.get(NAMESPACE, MUNICIPALITY_ID, "job-id")).thenReturn(jobResponse);

		var result = service.startLabelMove(NAMESPACE, MUNICIPALITY_ID, LABEL_ID, LabelMoveRequest.create().withDryRun(false));

		assertThat(result).isEqualTo(jobResponse);
		verify(metadataLabelRepositoryMock).findByIdAndNamespaceAndMunicipalityId(LABEL_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePath(NAMESPACE, MUNICIPALITY_ID, "CHILD");
		verify(errandsRepositoryMock).countByLabelsMetadataLabelId(LABEL_ID);
		verify(jobServiceMock).create(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, 3);
		verify(jobServiceMock).get(NAMESPACE, MUNICIPALITY_ID, "job-id");
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(actionConfigRepositoryMock, metadataLabelRepositoryMock, errandsRepositoryMock, jobServiceMock);
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
