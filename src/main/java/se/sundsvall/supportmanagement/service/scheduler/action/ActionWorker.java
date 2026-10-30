package se.sundsvall.supportmanagement.service.scheduler.action;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.ErrandActionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.action.Action;
import se.sundsvall.supportmanagement.service.action.ActionExecutor;

import static java.time.OffsetDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.inNewRequestGroup;

@Component
public class ActionWorker {

	static final String EVENT_LOG_ACTION = "Ärendet har uppdaterats av en schemalagd åtgärd.";

	private final ErrandActionRepository errandActionRepository;
	private final ErrandsRepository errandsRepository;
	private final ActionExecutor actionExecutor;
	private final Map<String, Action> actions;

	public ActionWorker(final ErrandActionRepository errandActionRepository, final List<Action> actionList, final ErrandsRepository errandsRepository, final ActionExecutor actionExecutor) {
		this.errandActionRepository = errandActionRepository;
		this.errandsRepository = errandsRepository;
		this.actionExecutor = actionExecutor;
		this.actions = new HashMap<>();
		actionList.forEach(action -> this.actions.put(action.getName(), action));
	}

	@Transactional
	public List<ErrandActionEntity> getExpiredActions() {
		return errandActionRepository.findAllByExecuteAfterBeforeAndErrandEntityDeletedIsNull(now(ZoneId.systemDefault()));
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void processAction(final ErrandActionEntity actionEntity) {
		final var configEntity = actionEntity.getActionConfigEntity();

		if (configEntity == null) {
			throw new IllegalStateException("No action config found for errand action with id: " + actionEntity.getId());
		}

		final var actionName = configEntity.getName();
		final var action = actions.get(actionName);

		if (action == null) {
			throw new IllegalStateException("No action implementation found for name: " + actionName);
		}

		final var errand = errandsRepository.findWithLockingById(actionEntity.getErrandEntity().getId()).orElseThrow(() -> new RuntimeException("Could not find errand"));

		// Soft deleted since the action was picked up: left waiting, to be carried out once the errand is restored
		if (nonNull(errand.getDeleted())) {
			return;
		}

		final var parameters = toParameterMap(configEntity);

		if (action.actionFulfilled(errand, parameters) || !action.conditionsFulfilled(errand, configEntity)) {
			removeAction(errand, actionEntity);
			return;
		}

		// What the action logs is grouped together, to reach the subscribers as one notification. The action is taken off
		// the errand first, so the revision recording what it did no longer lists it.
		inNewRequestGroup(() -> {
			removeAction(errand, actionEntity);
			actionExecutor.executeAndRecord(errand, action, configEntity, EVENT_LOG_ACTION);
		});
	}

	/**
	 * Takes the action off the errand as well as out of the database.
	 */
	private void removeAction(final ErrandEntity errand, final ErrandActionEntity actionEntity) {
		// The actions cascade: one deleted while the errand still lists it is written back at flush, and runs again.
		ofNullable(errand.getActions()).ifPresent(errandActions -> errandActions.removeIf(listed -> Objects.equals(listed.getId(), actionEntity.getId())));
		errandActionRepository.delete(actionEntity);
	}

	private Map<String, List<String>> toParameterMap(final ActionConfigEntity configEntity) {
		if (configEntity.getParameters() == null) {
			return Map.of();
		}
		return configEntity.getParameters().stream()
			.collect(Collectors.toMap(
				ActionConfigParameterEntity::getKey,
				ActionConfigParameterEntity::getValues));
	}
}
