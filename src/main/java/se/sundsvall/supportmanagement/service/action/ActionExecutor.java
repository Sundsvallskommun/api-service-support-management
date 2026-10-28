package se.sundsvall.supportmanagement.service.action;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.RevisionService;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.shouldNotify;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.withNotify;

/**
 * Carries out an action on an errand and records what it did, the same whether the action was due at once in a request
 * or scheduled for later.
 */
@Component
public class ActionExecutor {

	private static final Logger LOG = LoggerFactory.getLogger(ActionExecutor.class);

	private final RevisionService revisionService;
	private final EventService eventService;

	public ActionExecutor(final RevisionService revisionService, final EventService eventService) {
		this.revisionService = revisionService;
		this.eventService = eventService;
	}

	/**
	 * Carries out the action and records what it did to the errand as a change of its own: a revision and an event for
	 * it, through which the process of the errand learns of the change, and an event for every label it added. What it
	 * logs notifies the subscribers unless the config of the action says not to, or the request it runs in was sent with
	 * X-notify: false, while the assigned user is not notified. Nobody acted, so nobody is left out as the one who did.
	 *
	 * @param errand  the errand to act on
	 * @param action  the action to carry out
	 * @param config  the configuration of the action
	 * @param message the text the change is logged with
	 */
	public void executeAndRecord(final ErrandEntity errand, final Action action, final ActionConfigEntity config, final String message) {
		withNotify(config.getNotify() && shouldNotify(), () -> {
			final var previousLabelIds = labelIdsOf(errand);
			if (action.executeAction(errand, config)) {
				logChange(errand, message);
			}

			final var addedLabelIds = labelIdsOf(errand);
			addedLabelIds.removeAll(previousLabelIds);
			if (!addedLabelIds.isEmpty()) {
				logLabelEvents(errand, addedLabelIds);
			}
		});
	}

	/**
	 * Asked only of an action that says it changed the errand. A failed publication marks the transaction for rollback,
	 * so the action is rolled back with it.
	 */
	private void logChange(final ErrandEntity errand, final String message) {
		final var revision = revisionService.createErrandRevision(errand);

		if (isNull(revision)) {
			return;
		}

		try {
			eventService.createErrandEvent(UPDATE, message, errand, revision.latest(), revision.previous(), false, ERRAND, null);
		} catch (final Exception e) {
			LOG.warn("Failed to log UPDATE event for errand {} after an action: {}", sanitizeForLogging(errand.getId()), sanitizeForLogging(e.getMessage()));
		}
	}

	/**
	 * A log that cannot be written is not worth undoing the action for.
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
}
