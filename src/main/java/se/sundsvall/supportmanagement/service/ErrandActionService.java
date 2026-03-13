package se.sundsvall.supportmanagement.service;

import static java.time.OffsetDateTime.now;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper.toEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper.toMap;
import static se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper.updateEntity;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.action.ActionDefinition;
import se.sundsvall.supportmanagement.api.model.config.action.Config;
import se.sundsvall.supportmanagement.integration.db.ActionConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.action.Action;
import se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper;

@Service
@Transactional
public class ErrandActionService {

	private final ActionConfigRepository actionConfigRepository;
	private final Map<String, Action> actions;

	public ErrandActionService(ActionConfigRepository actionConfigRepository, List<Action> actions) {
		this.actionConfigRepository = actionConfigRepository;
		this.actions = new HashMap<>();
		actions.forEach(action -> {
			var result = this.actions.putIfAbsent(action.getName(), action);
			if (result != null) {
				throw new IllegalStateException("Duplicate action.name '%s'".formatted(action.getName()));
			}
		});
	}

	public List<ActionDefinition> getActionDefinitions(String municipalityId, String namespace) {
		return actions.values().stream()
			.map(action -> ActionDefinition.create()
				.withName(action.getName())
				.withDescription(action.getDescription())
				.withConditionDefinitions(action.getConditionDefinitions(municipalityId, namespace))
				.withParameterDefinitions(action.getParameterDefinitions(municipalityId, namespace)))
			.toList();
	}

	public List<Config> getActionConfigs(String municipalityId, String namespace) {
		return actionConfigRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId).stream()
			.map(ErrandActionMapper::toActionConfig)
			.toList();
	}

	public String createActionConfig(String municipalityId, String namespace, Config config) {
		var action = actions.get(config.getName());

		if (action == null) {
			throw Problem.valueOf(BAD_REQUEST, "Could not find action with name '%s'".formatted(config.getName()));
		}

		action.validateConditions(municipalityId, namespace, toMap(config.getConditions()));
		action.validateParameters(municipalityId, namespace, toMap(config.getParameters()));

		return actionConfigRepository.save(toEntity(municipalityId, namespace, config)).getId();
	}

	public void updateActionConfig(String municipalityId, String namespace, String id, Config config) {
		var entity = actionConfigRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Could not find action config with id '%s'".formatted(id)));

		var action = actions.get(config.getName());

		if (action == null) {
			throw Problem.valueOf(BAD_REQUEST, "Could not find action with name '%s'".formatted(config.getName()));
		}

		action.validateConditions(municipalityId, namespace, toMap(config.getConditions()));
		action.validateParameters(municipalityId, namespace, toMap(config.getParameters()));

		actionConfigRepository.save(updateEntity(entity, config));
	}

	public void deleteActionConfig(String municipalityId, String namespace, String id) {
		if (!actionConfigRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, "Could not find action config with id '%s'".formatted(id));
		}

		actionConfigRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	public void processErrandActions(ErrandEntity errand) {
		removeFulfilledActions(errand);

		var actionsToAdd = createActionsToAdd(errand);

		if (!actionsToAdd.isEmpty()) {
			if (errand.getActions() == null) {
				errand.setActions(new ArrayList<>());
			}
			errand.getActions().addAll(actionsToAdd);
		}
	}

	private List<ErrandActionEntity> createActionsToAdd(ErrandEntity errand) {
		var actionsToAdd = new ArrayList<ErrandActionEntity>();

		final var configs = actionConfigRepository.findAllByNamespaceAndMunicipalityId(errand.getNamespace(), errand.getMunicipalityId());

		final var existingConfigIds = Optional.ofNullable(errand.getActions()).orElse(new ArrayList<>()).stream()
			.map(action -> action.getActionConfigEntity().getId())
			.collect(Collectors.toSet());

		configs.stream()
			.filter(ActionConfigEntity::getActive)
			.filter(config -> actions.get(config.getName()) != null)
			.filter(config -> !existingConfigIds.contains(config.getId()))
			.filter(config -> !actions.get(config.getName()).actionFulfilled(errand, toParameterMap(config)))
			.forEach(config -> {
				final var action = actions.get(config.getName());
				action.createAction(errand, config).ifPresent(errandAction -> {
					if (errandAction.getExecuteAfter().isBefore(now())) {
						action.executeAction(errand, config);
					} else {
						actionsToAdd.add(errandAction);
					}
				});
			});

		return actionsToAdd;
	}

	private void removeFulfilledActions(ErrandEntity errand) {
		if (errand.getActions() != null && !errand.getActions().isEmpty()) {
			// Remove existing actions that are now fulfilled
			final var actionsToRemove = new ArrayList<>(errand.getActions().stream()
				.filter(errandAction -> {
					final var config = errandAction.getActionConfigEntity();
					final var action = actions.get(config.getName());
					return action != null && action.actionFulfilled(errand, toParameterMap(config));
				})
				.toList());

			errand.getActions().removeAll(actionsToRemove);
		}
	}

	private Map<String, List<String>> toParameterMap(final ActionConfigEntity config) {
		if (config.getParameters() == null) {
			return Map.of();
		}
		return config.getParameters().stream()
			.collect(Collectors.toMap(
				ActionConfigParameterEntity::getKey,
				ActionConfigParameterEntity::getValues));
	}
}
