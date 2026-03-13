package se.sundsvall.supportmanagement.service.scheduler.action;

import static java.time.OffsetDateTime.now;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.ErrandActionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.service.action.Action;

@Component
public class ActionWorker {

	private static final Logger LOG = LoggerFactory.getLogger(ActionWorker.class);

	private final ErrandActionRepository errandActionRepository;
	private final ErrandsRepository errandsRepository;
	private final Map<String, Action> actions;

	public ActionWorker(final ErrandActionRepository errandActionRepository, final List<Action> actionList, final ErrandsRepository errandsRepository) {
		this.errandActionRepository = errandActionRepository;
		this.errandsRepository = errandsRepository;
		this.actions = new HashMap<>();
		actionList.forEach(action -> this.actions.put(action.getName(), action));
	}

	public List<ErrandActionEntity> getExpiredActions() {
		return errandActionRepository.findAllByExecuteAfterBefore(now());
	}

	@Transactional
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

		final var errand = actionEntity.getErrandEntity();
		final var parameters = toParameterMap(configEntity);

		if (action.actionFulfilled(errand, parameters)) {
			errandActionRepository.delete(actionEntity);
		} else {
			var lockedErrand = errandsRepository.findWithLockingById(errand.getId()).orElseThrow(() -> new RuntimeException("Could not find errand"));
			action.executeAction(lockedErrand, configEntity);
			errandActionRepository.delete(actionEntity);
		}
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
