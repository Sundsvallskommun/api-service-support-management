package se.sundsvall.supportmanagement.service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.action.ActionDefinition;
import se.sundsvall.supportmanagement.api.model.config.action.Config;
import se.sundsvall.supportmanagement.integration.db.ActionConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.OperationType;
import se.sundsvall.supportmanagement.service.action.Action;
import se.sundsvall.supportmanagement.service.action.ActionExecutor;
import se.sundsvall.supportmanagement.service.action.DueAction;
import se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper;

import static java.time.OffsetDateTime.now;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static org.springframework.util.CollectionUtils.isEmpty;
import static se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper.toEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper.toMap;
import static se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper.updateEntity;

@Service
public class ErrandActionService {

	static final String EVENT_LOG_ACTION = "Ärendet har uppdaterats av en åtgärd.";

	private final ActionConfigRepository actionConfigRepository;
	private final ActionExecutor actionExecutor;
	private final Map<String, Action> actions;

	public ErrandActionService(ActionConfigRepository actionConfigRepository, ActionExecutor actionExecutor, List<Action> actions) {
		this.actionConfigRepository = actionConfigRepository;
		this.actionExecutor = actionExecutor;
		this.actions = new HashMap<>();
		actions.forEach(action -> {
			var result = this.actions.putIfAbsent(action.getName(), action);
			if (result != null) {
				throw new IllegalStateException("Duplicate action.name '%s'".formatted(action.getName()));
			}
		});
	}

	@Transactional(readOnly = true)
	public List<ActionDefinition> getActionDefinitions(String municipalityId, String namespace) {
		return actions.values().stream()
			.map(action -> ActionDefinition.create()
				.withName(action.getName())
				.withDescription(action.getDescription())
				.withConditionDefinitions(action.getConditionDefinitions(municipalityId, namespace))
				.withParameterDefinitions(action.getParameterDefinitions(municipalityId, namespace))
				.withOperationTypes(List.copyOf(action.getValidOperationTypes())))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<Config> getActionConfigs(String municipalityId, String namespace) {
		return actionConfigRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId).stream()
			.map(ErrandActionMapper::toActionConfig)
			.toList();
	}

	@Transactional
	public String createActionConfig(String municipalityId, String namespace, Config config) {
		var action = actions.get(config.getName());

		if (action == null) {
			throw Problem.valueOf(BAD_REQUEST, "Could not find action with name '%s'".formatted(config.getName()));
		}

		action.validateConditions(municipalityId, namespace, toMap(config.getConditions()));
		action.validateParameters(municipalityId, namespace, toMap(config.getParameters()));
		validateOperationTypes(action, config);

		return actionConfigRepository.save(toEntity(municipalityId, namespace, config)).getId();
	}

	@Transactional
	public void updateActionConfig(String municipalityId, String namespace, String id, Config config) {
		var entity = actionConfigRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Could not find action config with id '%s'".formatted(id)));

		var action = actions.get(config.getName());

		if (action == null) {
			throw Problem.valueOf(BAD_REQUEST, "Could not find action with name '%s'".formatted(config.getName()));
		}

		action.validateConditions(municipalityId, namespace, toMap(config.getConditions()));
		action.validateParameters(municipalityId, namespace, toMap(config.getParameters()));
		validateOperationTypes(action, config);

		actionConfigRepository.save(updateEntity(entity, config));
	}

	@Transactional
	public void deleteActionConfig(String municipalityId, String namespace, String id) {
		if (!actionConfigRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, "Could not find action config with id '%s'".formatted(id));
		}

		actionConfigRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	/**
	 * Removes the actions of the errand that are fulfilled, and creates the ones the action configs of its namespace call
	 * for. Those due at once are not carried out here but returned, for the caller to carry out once the change that
	 * called for them has been recorded, so that what an action does is recorded after it. A draft is left alone: it
	 * gets no actions until it has been made active.
	 *
	 * @param  errand        the errand to act on.
	 * @param  operationType the operation the errand has been through.
	 * @return               the actions due at once, in the order they are to be carried out.
	 */
	@Transactional
	public List<DueAction> processErrandActions(ErrandEntity errand, OperationType operationType) {
		if (errand.isDraft()) {
			return List.of();
		}

		removeFulfilledActions(errand);

		final var dueActions = new ArrayList<DueAction>();
		final var actionsToAdd = createActionsToAdd(errand, operationType, dueActions);

		if (!actionsToAdd.isEmpty()) {
			if (errand.getActions() == null) {
				errand.setActions(new ArrayList<>());
			}
			errand.getActions().addAll(actionsToAdd);
		}
		return dueActions;
	}

	/**
	 * Carries out the actions due at once on the errand, each recorded as a change of its own after the change that
	 * called for them. What they log reaches the subscribers along with that change.
	 *
	 * @param errand     the errand to act on.
	 * @param dueActions the actions {@link #processErrandActions} returned.
	 */
	public void executeDueActions(ErrandEntity errand, List<DueAction> dueActions) {
		dueActions.forEach(dueAction -> actionExecutor.executeAndRecord(errand, dueAction.action(), dueAction.config(), EVENT_LOG_ACTION));
	}

	private List<ErrandActionEntity> createActionsToAdd(ErrandEntity errand, OperationType operationType, List<DueAction> dueActions) {
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
			.filter(config -> reactsTo(config, actions.get(config.getName()), operationType))
			.forEach(config -> {
				final var action = actions.get(config.getName());
				action.createAction(errand, config).ifPresent(errandAction -> {
					if (errandAction.getExecuteAfter().isEqual(now(ZoneId.systemDefault())) || errandAction.getExecuteAfter().isBefore(now(ZoneId.systemDefault()))) {
						dueActions.add(new DueAction(action, config));
					} else {
						actionsToAdd.add(errandAction);
					}
				});
			});

		return actionsToAdd;
	}

	/**
	 * Whether a config reacts to the operation at hand. An empty set on the config means every operation the action
	 * supports. The action is consulted either way, so a stored set can only narrow what the action supports.
	 */
	private static boolean reactsTo(ActionConfigEntity config, Action action, OperationType operationType) {
		if (!action.validForOperationType(operationType)) {
			return false;
		}
		final var configured = config.getOperationTypes();
		return isEmpty(configured) || configured.contains(operationType);
	}

	/**
	 * A config may say which of the operations of its action it reacts to, and nothing beyond them: an action that never
	 * runs on update cannot be made to by configuration.
	 */
	private static void validateOperationTypes(Action action, Config config) {
		ofNullable(config.getOperationTypes()).orElse(List.of()).forEach(operationType -> {
			if (!action.validForOperationType(operationType)) {
				throw Problem.valueOf(UNPROCESSABLE_CONTENT,
					"Operation type '%s' is not supported by action '%s'".formatted(operationType, action.getName()));
			}
		});
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
