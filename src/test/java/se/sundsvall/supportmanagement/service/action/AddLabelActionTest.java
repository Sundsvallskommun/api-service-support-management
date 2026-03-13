package se.sundsvall.supportmanagement.service.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Labels;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.service.MetadataService;

@ExtendWith(MockitoExtension.class)
class AddLabelActionTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "testNamespace";
	private static final String LABEL_ID_1 = "label-id-1";
	private static final String LABEL_ID_2 = "label-id-2";
	private static final String STATUS_OPEN = "OPEN";
	private static final String STATUS_CLOSED = "CLOSED";

	@Mock
	private MetadataService metadataService;

	@Mock
	private ErrandsRepository errandsRepository;

	@InjectMocks
	private AddLabelAction addLabelAction;

	@Test
	void getName() {
		assertThat(addLabelAction.getName()).isEqualTo("ADD_LABEL");
	}

	@Test
	void getDescription() {
		assertThat(addLabelAction.getDescription()).isEqualTo("Adds a new label within a configurable amount of time if conditions are met");
	}

	@Test
	void getConditionDefinitions() {
		when(metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(
			Status.create().withName(STATUS_OPEN),
			Status.create().withName(STATUS_CLOSED)));
		when(metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Labels.create()
			.withLabelStructure(List.of(
				Label.create().withId(LABEL_ID_1).withDisplayName("Label 1").withClassification("type").withResourceName("LABEL_1"),
				Label.create().withId(LABEL_ID_2).withDisplayName("Label 2").withClassification("type").withResourceName("LABEL_2"))));

		var result = addLabelAction.getConditionDefinitions(MUNICIPALITY_ID, NAMESPACE);

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getKey()).isEqualTo("status");
		assertThat(result.getFirst().getMandatory()).isFalse();
		assertThat(result.getFirst().getPossibleValues()).hasSize(2);
		assertThat(result.get(1).getKey()).isEqualTo("hasLabel");
		assertThat(result.get(1).getMandatory()).isFalse();
		assertThat(result.get(1).getPossibleValues()).hasSize(2);

		verify(metadataService).findStatuses(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataService).findLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(metadataService);
	}

	@Test
	void getParameterDefinitions() {
		when(metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Labels.create()
			.withLabelStructure(List.of(
				Label.create().withId(LABEL_ID_1).withDisplayName("Label 1").withClassification("type").withResourceName("LABEL_1"))));

		var result = addLabelAction.getParameterDefinitions(MUNICIPALITY_ID, NAMESPACE);

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getKey()).isEqualTo("label");
		assertThat(result.getFirst().getMandatory()).isTrue();
		assertThat(result.getFirst().getPossibleValues()).hasSize(1);
		assertThat(result.get(1).getKey()).isEqualTo("duration");
		assertThat(result.get(1).getMandatory()).isFalse();

		verify(metadataService).findLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(metadataService);
	}

	// validateConditions tests

	@Test
	void validateConditionsWithValidStatusAndLabel() {
		when(metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(Status.create().withName(STATUS_OPEN)));
		when(metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Labels.create()
			.withLabelStructure(List.of(Label.create().withId(LABEL_ID_1).withDisplayName("Label 1").withClassification("type").withResourceName("LABEL_1"))));

		addLabelAction.validateConditions(MUNICIPALITY_ID, NAMESPACE, Map.of(
			"status", List.of(STATUS_OPEN),
			"hasLabel", List.of(LABEL_ID_1)));

		verify(metadataService).findStatuses(NAMESPACE, MUNICIPALITY_ID);
		verify(metadataService).findLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(metadataService);
	}

	@Test
	void validateConditionsWithInvalidKey() {
		assertThatThrownBy(() -> addLabelAction.validateConditions(MUNICIPALITY_ID, NAMESPACE, Map.of("invalidKey", List.of("value"))))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Key 'invalidKey' is not valid");
	}

	@Test
	void validateConditionsWithInvalidStatus() {
		when(metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(Status.create().withName(STATUS_OPEN)));

		assertThatThrownBy(() -> addLabelAction.validateConditions(MUNICIPALITY_ID, NAMESPACE, Map.of("status", List.of("INVALID_STATUS"))))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Status 'INVALID_STATUS' is not valid for this namespace");
	}

	@Test
	void validateConditionsWithInvalidLabel() {
		when(metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Labels.create()
			.withLabelStructure(List.of(Label.create().withId(LABEL_ID_1).withDisplayName("Label 1").withClassification("type").withResourceName("LABEL_1"))));

		assertThatThrownBy(() -> addLabelAction.validateConditions(MUNICIPALITY_ID, NAMESPACE, Map.of("hasLabel", List.of("invalid-label-id"))))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Label ID 'invalid-label-id' is not valid");
	}

	// validateParameters tests

	@Test
	void validateParametersWithValidLabelAndDuration() {
		when(metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Labels.create()
			.withLabelStructure(List.of(Label.create().withId(LABEL_ID_1).withDisplayName("Label 1").withClassification("type").withResourceName("LABEL_1"))));

		addLabelAction.validateParameters(MUNICIPALITY_ID, NAMESPACE, Map.of(
			"label", List.of(LABEL_ID_1),
			"duration", List.of("PT1H")));

		verify(metadataService).findLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(metadataService);
	}

	@Test
	void validateParametersWithInvalidKey() {
		assertThatThrownBy(() -> addLabelAction.validateParameters(MUNICIPALITY_ID, NAMESPACE, Map.of(
			"label", List.of(LABEL_ID_1),
			"invalidKey", List.of("value"))))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Key 'invalidKey' is not valid");
	}

	@Test
	void validateParametersWithMissingLabel() {
		assertThatThrownBy(() -> addLabelAction.validateParameters(MUNICIPALITY_ID, NAMESPACE, Map.of("duration", List.of("PT1H"))))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Key 'label' is mandatory and cannot be empty");
	}

	@Test
	void validateParametersWithEmptyLabel() {
		assertThatThrownBy(() -> addLabelAction.validateParameters(MUNICIPALITY_ID, NAMESPACE, Map.of("label", List.of())))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Key 'label' is mandatory and cannot be empty");
	}

	@Test
	void validateParametersWithMultipleDurationValues() {
		assertThatThrownBy(() -> addLabelAction.validateParameters(MUNICIPALITY_ID, NAMESPACE, Map.of(
			"label", List.of(LABEL_ID_1),
			"duration", List.of("PT1H", "PT2H"))))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Cannot handle multiple values of key 'duration'");
	}

	@Test
	void validateParametersWithInvalidDuration() {
		assertThatThrownBy(() -> addLabelAction.validateParameters(MUNICIPALITY_ID, NAMESPACE, Map.of(
			"label", List.of(LABEL_ID_1),
			"duration", List.of("not-a-duration"))))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Could not parse duration 'not-a-duration'");
	}

	@Test
	void validateParametersWithInvalidLabelId() {
		when(metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Labels.create()
			.withLabelStructure(List.of(Label.create().withId(LABEL_ID_1).withDisplayName("Label 1").withClassification("type").withResourceName("LABEL_1"))));

		assertThatThrownBy(() -> addLabelAction.validateParameters(MUNICIPALITY_ID, NAMESPACE, Map.of("label", List.of("invalid-label-id"))))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Label ID 'invalid-label-id' is not valid");
	}

	// actionFulfilled tests

	@Test
	void actionFulfilledWhenErrandHasAllLabels() {
		var errand = ErrandEntity.create()
			.withLabels(List.of(
				ErrandLabelEmbeddable.create().withMetadataLabelId(LABEL_ID_1),
				ErrandLabelEmbeddable.create().withMetadataLabelId(LABEL_ID_2)));

		var result = addLabelAction.actionFulfilled(errand, Map.of("label", List.of(LABEL_ID_1, LABEL_ID_2)));

		assertThat(result).isTrue();
	}

	@Test
	void actionFulfilledWhenErrandMissingLabel() {
		var errand = ErrandEntity.create()
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(LABEL_ID_1)));

		var result = addLabelAction.actionFulfilled(errand, Map.of("label", List.of(LABEL_ID_1, LABEL_ID_2)));

		assertThat(result).isFalse();
	}

	// createAction tests

	@Test
	void createActionWhenConditionsMet() {
		var errand = ErrandEntity.create()
			.withStatus(STATUS_OPEN)
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(LABEL_ID_1)));

		var config = ActionConfigEntity.create()
			.withActive(true);
		config.setConditions(new ArrayList<>(List.of(
			ActionConfigConditionEntity.create().withKey("status").withValues(List.of(STATUS_OPEN)),
			ActionConfigConditionEntity.create().withKey("hasLabel").withValues(List.of(LABEL_ID_1)))));
		config.setParameters(new ArrayList<>(List.of(
			ActionConfigParameterEntity.create().withKey("duration").withValues(List.of("PT1H")))));

		var result = addLabelAction.createAction(errand, config);

		assertThat(result).isPresent();
		assertThat(result.get().getActionConfigEntity()).isEqualTo(config);
		assertThat(result.get().getErrandEntity()).isEqualTo(errand);
		assertThat(result.get().getExecuteAfter()).isCloseTo(OffsetDateTime.now().plusHours(1), within(5, ChronoUnit.SECONDS));
	}

	@Test
	void createActionWithoutDuration() {
		var errand = ErrandEntity.create()
			.withStatus(STATUS_OPEN)
			.withLabels(List.of());

		var config = ActionConfigEntity.create()
			.withActive(true);
		config.setConditions(new ArrayList<>());
		config.setParameters(new ArrayList<>());

		var result = addLabelAction.createAction(errand, config);

		assertThat(result).isPresent();
		assertThat(result.get().getExecuteAfter()).isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));
	}

	@Test
	void createActionWhenStatusConditionNotMet() {
		var errand = ErrandEntity.create()
			.withStatus(STATUS_CLOSED)
			.withLabels(List.of());

		var config = ActionConfigEntity.create()
			.withActive(true);
		config.setConditions(new ArrayList<>(List.of(
			ActionConfigConditionEntity.create().withKey("status").withValues(List.of(STATUS_OPEN)))));
		config.setParameters(new ArrayList<>());

		var result = addLabelAction.createAction(errand, config);

		assertThat(result).isEmpty();
	}

	@Test
	void createActionWhenHasLabelConditionNotMet() {
		var errand = ErrandEntity.create()
			.withStatus(STATUS_OPEN)
			.withLabels(List.of());

		var config = ActionConfigEntity.create()
			.withActive(true);
		config.setConditions(new ArrayList<>(List.of(
			ActionConfigConditionEntity.create().withKey("hasLabel").withValues(List.of(LABEL_ID_1)))));
		config.setParameters(new ArrayList<>());

		var result = addLabelAction.createAction(errand, config);

		assertThat(result).isEmpty();
	}

	@Test
	void createActionWhenNotActive() {
		var errand = ErrandEntity.create()
			.withStatus(STATUS_OPEN)
			.withLabels(List.of());

		var config = ActionConfigEntity.create()
			.withActive(false);
		config.setConditions(new ArrayList<>());
		config.setParameters(new ArrayList<>());

		var result = addLabelAction.createAction(errand, config);

		assertThat(result).isEmpty();
	}

	// executeAction tests

	@Test
	void executeAction() {
		var errand = ErrandEntity.create()
			.withLabels(new ArrayList<>(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(LABEL_ID_1))));

		var config = ActionConfigEntity.create();
		config.setParameters(new ArrayList<>(List.of(
			ActionConfigParameterEntity.create().withKey("label").withValues(List.of(LABEL_ID_2)))));

		addLabelAction.executeAction(errand, config);

		assertThat(errand.getLabels()).hasSize(2);
		assertThat(errand.getLabels()).extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(LABEL_ID_1, LABEL_ID_2);
		verify(errandsRepository).save(errand);
	}

	@Test
	void executeActionWithNoExistingLabels() {
		var errand = ErrandEntity.create()
			.withLabels(new ArrayList<>());

		var config = ActionConfigEntity.create();
		config.setParameters(new ArrayList<>(List.of(
			ActionConfigParameterEntity.create().withKey("label").withValues(List.of(LABEL_ID_1, LABEL_ID_2)))));

		addLabelAction.executeAction(errand, config);

		assertThat(errand.getLabels()).hasSize(2);
		assertThat(errand.getLabels()).extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(LABEL_ID_1, LABEL_ID_2);
		verify(errandsRepository).save(errand);
	}

	@Test
	void executeActionWithNoLabelParameter() {
		var errand = ErrandEntity.create()
			.withLabels(new ArrayList<>(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(LABEL_ID_1))));

		var config = ActionConfigEntity.create();
		config.setParameters(new ArrayList<>(List.of(
			ActionConfigParameterEntity.create().withKey("duration").withValues(List.of("PT1H")))));

		addLabelAction.executeAction(errand, config);

		assertThat(errand.getLabels()).hasSize(1);
		verify(errandsRepository).save(errand);
	}
}
