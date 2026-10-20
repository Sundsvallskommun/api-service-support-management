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
import se.sundsvall.supportmanagement.service.RevisionService;
import se.sundsvall.supportmanagement.service.action.Action;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.time.OffsetDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;

@Component
public class ActionWorker {

	static final String EVENT_LOG_ACTION = "Ärendet har uppdaterats av en schemalagd åtgärd.";

	private static final Logger LOG = LoggerFactory.getLogger(ActionWorker.class);

	private final ErrandActionRepository errandActionRepository;
	private final ErrandsRepository errandsRepository;
	private final RevisionService revisionService;
	private final EventService eventService;
	private final Map<String, Action> actions;

	public ActionWorker(final ErrandActionRepository errandActionRepository, final List<Action> actionList, final ErrandsRepository errandsRepository, final RevisionService revisionService,
		final EventService eventService) {
		this.errandActionRepository = errandActionRepository;
		this.errandsRepository = errandsRepository;
		this.revisionService = revisionService;
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
			removeAction(errand, actionEntity);
			return;
		}

		final var previousLabelIds = labelIdsOf(errand);
		final var changed = action.executeAction(errand, configEntity);
		removeAction(errand, actionEntity);

		if (changed) {
			logChange(errand);
		}

		final var addedLabelIds = labelIdsOf(errand);
		addedLabelIds.removeAll(previousLabelIds);
		if (!addedLabelIds.isEmpty()) {
			logLabelEvents(errand, addedLabelIds);
		}
	}

	/**
	 * Takes the action off the errand as well as out of the database.
	 */
	private void removeAction(final ErrandEntity errand, final ErrandActionEntity actionEntity) {
		// The actions cascade: one deleted while the errand still lists it is written back at flush, and runs again.
		ofNullable(errand.getActions()).ifPresent(errandActions -> errandActions.removeIf(listed -> Objects.equals(listed.getId(), actionEntity.getId())));
		errandActionRepository.delete(actionEntity);
	}

	/**
	 * Records what a scheduled action did to the errand, the way a write through the API is recorded: a revision, and an
	 * event for it, through which the process of the errand learns of the change.
	 * <p>
	 * Asked only of an action that says it changed the errand. No notification is sent. A failed publication marks the
	 * transaction for rollback, so the action is rolled back with it and tried again on the next run.
	 */
	private void logChange(final ErrandEntity errand) {
		final var revision = revisionService.createErrandRevision(errand);

		if (isNull(revision)) {
			return;
		}

		try {
			eventService.createErrandEventWithoutNotification(UPDATE, EVENT_LOG_ACTION, errand, revision.latest(), revision.previous(), ERRAND);
		} catch (final Exception e) {
			LOG.warn("Failed to log UPDATE event for errand {} after a scheduled action: {}", sanitizeForLogging(errand.getId()), sanitizeForLogging(e.getMessage()));
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
