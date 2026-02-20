package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.action.Config;
import se.sundsvall.supportmanagement.api.model.config.action.Parameter;
import se.sundsvall.supportmanagement.integration.db.ActionConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.service.action.Action;

@ExtendWith(MockitoExtension.class)
class ErrandActionServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE";
	private static final String ACTION_NAME = "TEST_ACTION";
	private static final String CONFIG_ID = "config-id";

	@Mock
	private ActionConfigRepository actionConfigRepositoryMock;

	@Mock
	private Action actionMock;

	@Test
	void constructorThrowsOnDuplicateActionName() {
		final var action1 = createActionMock("DUPLICATE");
		final var action2 = createActionMock("DUPLICATE");

		assertThatThrownBy(() -> new ErrandActionService(actionConfigRepositoryMock, List.of(action1, action2)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Duplicate action.name 'DUPLICATE'");
	}

	@Test
	void getActionConfigs() {
		final var entity = createEntity();
		when(actionConfigRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(entity));

		final var service = createService();
		final var result = service.getActionConfigs(MUNICIPALITY_ID, NAMESPACE);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getName()).isEqualTo(ACTION_NAME);

		verify(actionConfigRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(actionConfigRepositoryMock);
	}

	@Test
	void createActionConfig() {
		final var config = createConfig();
		final var savedEntity = createEntity().withId(CONFIG_ID);

		when(actionConfigRepositoryMock.save(any())).thenReturn(savedEntity);

		final var service = createService();
		final var result = service.createActionConfig(MUNICIPALITY_ID, NAMESPACE, config);

		assertThat(result).isEqualTo(CONFIG_ID);

		verify(actionMock).validateConditions(MUNICIPALITY_ID, NAMESPACE, java.util.Map.of("condKey", List.of("condVal")));
		verify(actionMock).validateParameters(MUNICIPALITY_ID, NAMESPACE, java.util.Map.of("paramKey", List.of("paramVal")));
		verify(actionConfigRepositoryMock).save(any());
		verifyNoMoreInteractions(actionConfigRepositoryMock, actionMock);
	}

	@Test
	void createActionConfigWithUnknownAction() {
		final var config = createConfig().withName("UNKNOWN");

		final var service = createService();

		assertThatThrownBy(() -> service.createActionConfig(MUNICIPALITY_ID, NAMESPACE, config))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: Could not find action with name 'UNKNOWN'");

		verifyNoInteractions(actionConfigRepositoryMock);
	}

	@Test
	void updateActionConfig() {
		final var config = createConfig();
		final var existingEntity = createEntity().withId(CONFIG_ID);

		when(actionConfigRepositoryMock.findByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(existingEntity));
		when(actionConfigRepositoryMock.save(any())).thenReturn(existingEntity);

		final var service = createService();
		service.updateActionConfig(MUNICIPALITY_ID, NAMESPACE, CONFIG_ID, config);

		verify(actionMock).validateConditions(MUNICIPALITY_ID, NAMESPACE, java.util.Map.of("condKey", List.of("condVal")));
		verify(actionMock).validateParameters(MUNICIPALITY_ID, NAMESPACE, java.util.Map.of("paramKey", List.of("paramVal")));
		verify(actionConfigRepositoryMock).findByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(actionConfigRepositoryMock).save(any());
		verifyNoMoreInteractions(actionConfigRepositoryMock, actionMock);
	}

	@Test
	void updateActionConfigNotFound() {
		final var config = createConfig();

		when(actionConfigRepositoryMock.findByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		final var service = createService();

		assertThatThrownBy(() -> service.updateActionConfig(MUNICIPALITY_ID, NAMESPACE, CONFIG_ID, config))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: Could not find action config with id 'config-id'");

		verify(actionConfigRepositoryMock).findByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(actionConfigRepositoryMock);
	}

	@Test
	void updateActionConfigWithUnknownAction() {
		final var config = createConfig().withName("UNKNOWN");
		final var existingEntity = createEntity().withId(CONFIG_ID);

		when(actionConfigRepositoryMock.findByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(existingEntity));

		final var service = createService();

		assertThatThrownBy(() -> service.updateActionConfig(MUNICIPALITY_ID, NAMESPACE, CONFIG_ID, config))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: Could not find action with name 'UNKNOWN'");

		verify(actionConfigRepositoryMock).findByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(actionConfigRepositoryMock);
	}

	@Test
	void deleteActionConfig() {
		when(actionConfigRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		final var service = createService();
		service.deleteActionConfig(MUNICIPALITY_ID, NAMESPACE, CONFIG_ID);

		verify(actionConfigRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(actionConfigRepositoryMock).deleteByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(actionConfigRepositoryMock);
	}

	@Test
	void deleteActionConfigNotFound() {
		when(actionConfigRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		final var service = createService();

		assertThatThrownBy(() -> service.deleteActionConfig(MUNICIPALITY_ID, NAMESPACE, CONFIG_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: Could not find action config with id 'config-id'");

		verify(actionConfigRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(CONFIG_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(actionConfigRepositoryMock);
	}

	private ErrandActionService createService() {
		when(actionMock.getName()).thenReturn(ACTION_NAME);
		return new ErrandActionService(actionConfigRepositoryMock, List.of(actionMock));
	}

	private Action createActionMock(String name) {
		final var mock = org.mockito.Mockito.mock(Action.class);
		when(mock.getName()).thenReturn(name);
		return mock;
	}

	private ActionConfigEntity createEntity() {
		final var entity = ActionConfigEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withActive(true)
			.withName(ACTION_NAME)
			.withDisplayValue("Display value");

		entity.setConditions(new java.util.ArrayList<>(List.of(ActionConfigConditionEntity.create()
			.withKey("condKey")
			.withValues(List.of("condVal")))));

		entity.setParameters(new java.util.ArrayList<>(List.of(ActionConfigParameterEntity.create()
			.withKey("paramKey")
			.withValues(List.of("paramVal")))));

		return entity;
	}

	private Config createConfig() {
		return Config.create()
			.withActive(true)
			.withName(ACTION_NAME)
			.withDisplayValue("Display value")
			.withConditions(List.of(Parameter.create().withKey("condKey").withValues(List.of("condVal"))))
			.withParameters(List.of(Parameter.create().withKey("paramKey").withValues(List.of("paramVal"))));
	}
}
