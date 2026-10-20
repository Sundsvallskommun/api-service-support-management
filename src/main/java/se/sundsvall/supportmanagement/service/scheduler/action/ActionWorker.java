package se.sundsvall.supportmanagement.service.scheduler.action;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.action.Action;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.time.OffsetDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Component
public class ActionWorker {

	private static final Logger LOG = LoggerFactory.getLogger(ActionWorker.class);

	private final ErrandActionRepository errandActionRepository;
	private final ErrandsRepository errandsRepository;
	private final EventService eventService;
	private final Map<String, Action> actions;

	public ActionWorker(final ErrandActionRepository errandActionRepository, final List<Action> actionList, final ErrandsRepository errandsRepository, final EventService eventService) {
		this.errandActionRepository = errandActionRepository;
		this.errandsRepository = errandsRepository;
		this.eventService = eventService;
		this.actions = new HashMap<>();
		actionList.forEach(action -> this.actions.put(action.getName(), action));
	}

	@Transactional
	public List<ErrandActionEntity> getExpiredActions() {
		return errandActionRepository.findAllByExecuteAfterBefore(now(ZoneId.systemDefault()));
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
		final var parameters = toParameterMap(configEntity);

		if (action.actionFulfilled(errand, parameters) || !action.conditionsFulfilled(errand, configEntity)) {
			errandActionRepository.delete(actionEntity);
		} else {
			final var previousLabelIds = labelIdsOf(errand);
			action.executeAction(errand, configEntity);
			errandActionRepository.delete(actionEntity);

			final var addedLabelIds = labelIdsOf(errand);
			addedLabelIds.removeAll(previousLabelIds);
			if (!addedLabelIds.isEmpty()) {
				logLabelEvents(errand, addedLabelIds);
			}
		}
	}

	/**
	 * Logs the labels the action added to the errand, so that subscribers may be notified of them. Nobody acted, so nobody
	 * is left out as the one who did. A log that cannot be written is not worth undoing the action for.
	 */
	private void logLabelEvents(final ErrandEntity errand, final Set<String> addedLabelIds) {
		try {
			eventService.createLabelEvents(UPDATE, errand, null, addedLabelIds, null);
		} catch (final Exception e) {
			LOG.warn("Failed to log LABEL events for errand {}: {}", errand.getId(), e.getMessage());
		}
	}

	private static Set<String> labelIdsOf(final ErrandEntity errand) {
		return ofNullable(errand.getLabels()).orElse(emptyList()).stream()
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.filter(Objects::nonNull)
			.collect(toCollection(LinkedHashSet::new));
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
