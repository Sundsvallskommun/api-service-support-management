package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.notes.Note;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.event.Event;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.eventlog.EventlogClient;
import se.sundsvall.supportmanagement.service.mapper.EventlogMapper;
import se.sundsvall.supportmanagement.service.model.ProcessCommand;
import se.sundsvall.supportmanagement.service.model.RevisionResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CASE_ID;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.DECISION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.NOTE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SYSTEM;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toEvent;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toMetadataMap;
import static se.sundsvall.supportmanagement.service.mapper.NotificationMapper.toNotification;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getExecutingUser;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getRequestGroupId;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.shouldNotify;

@Service
public class EventService {

	private static final Logger LOG = LoggerFactory.getLogger(EventService.class);
	private static final RevisionResult NO_REVISION = new RevisionResult(null, null);

	/**
	 * Who an event about an errand notifies, beyond what a draft or a request asking to notify no one rules out.
	 */
	private enum Notice {
		/** The handler of the errand directly, and its subscribers as their subscriptions decide. */
		HANDLER_AND_SUBSCRIBERS,
		/** The subscribers of the errand as their subscriptions decide, but not its handler directly. */
		SUBSCRIBERS,
		/** No one. */
		NO_ONE
	}

	private final EventlogClient eventLogClient;
	private final NotificationService notificationService;
	private final ApplicationEventPublisher eventPublisher;
	private final NotificationDispatchRepository notificationDispatchRepository;
	private final AccessControlService accessControlService;
	private final ProcessEventPublisher processEventPublisher;

	public EventService(final EventlogClient eventLogClient, final NotificationService notificationService, final ApplicationEventPublisher eventPublisher, final NotificationDispatchRepository notificationDispatchRepository,
		final AccessControlService accessControlService, final ProcessEventPublisher processEventPublisher) {
		this.eventLogClient = eventLogClient;
		this.notificationService = notificationService;
		this.eventPublisher = eventPublisher;
		this.notificationDispatchRepository = notificationDispatchRepository;
		this.accessControlService = accessControlService;
		this.processEventPublisher = processEventPublisher;
	}

	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision, final boolean sendNotification,
		final EventSubType subtype) {
		createErrandEvent(eventType, message, errandEntity, currentRevision, previousRevision, sendNotification, subtype, getExecutingUser());
	}

	/**
	 * Logs an errand event on behalf of a given user rather than of the caller of the request.
	 * <p>
	 * For work done where no request says who acted, such as a scheduled job carrying out what a user did elsewhere. The
	 * user is recorded as the one who acted, and so is not notified of it as a subscriber.
	 *
	 * @param actor the user who acted, or null when nobody did
	 */
	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision, final boolean sendNotification,
		final EventSubType subtype, final Identifier actor) {
		writeErrandEvent(eventType, message, errandEntity, new RevisionResult(previousRevision, currentRevision), subtype, actor, sendNotification ? Notice.HANDLER_AND_SUBSCRIBERS : Notice.SUBSCRIBERS);
		publishToProcess(errandEntity, eventType, subtype, null, false);
	}

	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision, final EventSubType subtype) {
		createErrandEvent(eventType, message, errandEntity, currentRevision, previousRevision, true, subtype);
	}

	/**
	 * Logs an errand event that notifies no one, neither the handler of the errand nor its subscribers, while the process
	 * of the errand learns of it as of any other event. For a change no one made by hand, such as a scheduled action or
	 * labels rebuilt after a label was moved or merged.
	 */
	public void createErrandEventWithoutNotification(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision,
		final EventSubType subtype) {
		writeErrandEvent(eventType, message, errandEntity, new RevisionResult(previousRevision, currentRevision), subtype, getExecutingUser(), Notice.NO_ONE);
		publishToProcess(errandEntity, eventType, subtype, null, false);
	}

	/**
	 * Writes the event of a command, and hands the command on to the process of the errand.
	 * <p>
	 * A command is a request aimed straight at the process rather than a change to the errand, so it makes no revision and
	 * the event points at none. What it carries is what publication cannot work out on its own - the key a handler chose to
	 * start, or the gate they stepped past. No one is notified, neither the handler of the errand nor its subscribers.
	 * <p>
	 * Without a command only the event is written, for a command the process already has on its way.
	 *
	 * @param eventType    the type of the event.
	 * @param message      the text of the event.
	 * @param errandEntity the errand the command is aimed at.
	 * @param subtype      the kind of command.
	 * @param command      what the command carries, or null when nothing is to be handed on to the process.
	 */
	public void createProcessCommandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final EventSubType subtype, final ProcessCommand command) {
		writeErrandEvent(eventType, message, errandEntity, NO_REVISION, subtype, getExecutingUser(), Notice.NO_ONE);

		if (nonNull(command)) {
			publishToProcess(errandEntity, eventType, subtype, command, false);
		}
	}

	/**
	 * Tells the process consumer of the namespace that the errand is gone, whether the errand had a process or not, without
	 * writing an event or notifying anyone: for a removal that is to leave no record of the errand behind, as the retention
	 * purge.
	 *
	 * @param errandEntity the errand that has been removed.
	 */
	public void publishDeletionToProcess(final ErrandEntity errandEntity) {
		publishToProcess(errandEntity, DELETE, ERRAND, null, false);
	}

	/**
	 * Writes the event of a change to one of the decisions of the errand, and tells the process of the errand about it.
	 * <p>
	 * A decision is no part of the revision of the errand, so the event points at none. A decision being concluded is what
	 * a process waits for, and one concluded by a handler is told past the emergency brake - see
	 * {@link ProcessEventPublisher}.
	 *
	 * @param message           the text of the event.
	 * @param errandEntity      the errand the decision belongs to.
	 * @param concludesDecision whether the change is the one that concludes the decision.
	 */
	public void createDecisionEvent(final String message, final ErrandEntity errandEntity, final boolean concludesDecision) {
		writeErrandEvent(UPDATE, message, errandEntity, NO_REVISION, DECISION, getExecutingUser(), Notice.HANDLER_AND_SUBSCRIBERS);
		publishToProcess(errandEntity, UPDATE, DECISION, null, concludesDecision);
	}

	/**
	 * Writes one event for a label move as a whole, logged against the moved label rather than an errand.
	 * <p>
	 * {@code startedBy} is recorded as the one who executed it, and is to be read on the request thread that accepted the
	 * move, since the thread carrying it out has no identifier of its own.
	 */
	public void createLabelMoveEvent(final String municipalityId, final String labelId, final String startedBy, final String message) {
		createLabelOperationEvent(municipalityId, labelId, startedBy, message, "label move");
	}

	/**
	 * Writes one event for a label merge as a whole, logged against the label the others were merged into, in the same way
	 * as {@link #createLabelMoveEvent}.
	 */
	public void createLabelMergeEvent(final String municipalityId, final String targetLabelId, final String startedBy, final String message) {
		createLabelOperationEvent(municipalityId, targetLabelId, startedBy, message, "label merge");
	}

	/**
	 * Writes the event of a label operation to the event log. A failure to write it is logged rather than thrown.
	 */
	private void createLabelOperationEvent(final String municipalityId, final String labelId, final String startedBy, final String message, final String operationName) {
		final var executedBy = Identifier.create().withType(Identifier.Type.CUSTOM).withValue(startedBy);
		final var event = toEvent(EventType.UPDATE, message, null, MetadataLabelEntity.class, Map.of(), executedBy, SYSTEM.getValue(), getRequestGroupId());
		try {
			eventLogClient.createEvent(municipalityId, labelId, event);
		} catch (final Exception e) {
			LOG.warn("Failed to create event log entry for {} {}: {}", operationName, sanitizeForLogging(labelId), sanitizeForLogging(e.getMessage()));
		}
	}

	public void createErrandNoteEvent(final EventType eventType, final String message, final String logKey, final ErrandEntity errandEntity, final String noteId, final Revision currentRevision, final Revision previousRevision) {
		final var requestGroupId = getRequestGroupId();
		final var caseId = extractCaseId(errandEntity);
		final var metadata = toMetadataMap(caseId, noteId, currentRevision, previousRevision, errandEntity.getNamespace());
		final var event = toEvent(eventType, message, extractId(currentRevision), Note.class, metadata, getExecutingUser(), NOTE.getValue(), requestGroupId);
		String eventId = null;
		try {
			eventId = extractEventId(eventLogClient.createEvent(errandEntity.getMunicipalityId(), logKey, event));
		} catch (final Exception e) {
			LOG.warn("Failed to create event log entry for errand note {}: {}", sanitizeForLogging(logKey), sanitizeForLogging(e.getMessage()));
		}
		eventPublisher.publishEvent(new AutoSubscribeEvent(errandEntity));

		if (!errandEntity.isDraft()) {
			createNotification(errandEntity, event);
		}
		if (notifies(errandEntity)) {
			saveDispatchEntry(errandEntity, eventType, requestGroupId, eventId, message, NOTE.getValue(), getExecutingUser());
		}
	}

	public Page<Event> readEvents(final String namespace, final String municipalityId, final String id, final Pageable pageable) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, id, ProtectedResource.EVENT, LR);

		final var response = eventLogClient.getEvents(municipalityId, id, pageable);

		return new PageImpl<>(response.getContent().stream()
			.map(EventlogMapper::toEvent)
			.filter(event -> nonNull(event.getType()))
			.toList(), pageable, response.getTotalElements());
	}

	/**
	 * Writes the event to the event log, and notifies those it is to notify.
	 *
	 * @param revisions the revision the event points at and the one before it, either of them null.
	 * @param notice    who the event notifies.
	 */
	private void writeErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final RevisionResult revisions, final EventSubType subtype, final Identifier actor,
		final Notice notice) {
		final var requestGroupId = getRequestGroupId();
		final var metadata = toMetadataMap(errandEntity, revisions.latest(), revisions.previous());
		final var event = toEvent(eventType, message, extractId(revisions.latest()), Errand.class, metadata, actor, subtype.getValue(), requestGroupId);
		String eventId = null;
		try {
			eventId = extractEventId(eventLogClient.createEvent(errandEntity.getMunicipalityId(), errandEntity.getId(), event));
		} catch (final Exception e) {
			LOG.warn("Failed to create event log entry for errand {}: {}", sanitizeForLogging(errandEntity.getId()), sanitizeForLogging(e.getMessage()));
		}
		if (eventType != EventType.DELETE) {
			eventPublisher.publishEvent(new AutoSubscribeEvent(errandEntity));
		}

		if (notice == Notice.HANDLER_AND_SUBSCRIBERS && notifies(errandEntity)) {
			createNotification(errandEntity, event);
		}

		// Which subscribers hear of the event is up to their subscriptions
		if (notice != Notice.NO_ONE && notifies(errandEntity)) {
			saveDispatchEntry(errandEntity, eventType, requestGroupId, eventId, message, subtype.getValue(), actor);
		}
	}

	/**
	 * Whether an event about the errand may notify anyone at all, its handler or its subscribers. A draft notifies no one,
	 * and neither does a request that asked to notify no one.
	 */
	private static boolean notifies(final ErrandEntity errandEntity) {
		return !errandEntity.isDraft() && shouldNotify();
	}

	/**
	 * Tells the process of the errand about the event, last and in the transaction of the change itself, whatever the
	 * notification flag says.
	 */
	private void publishToProcess(final ErrandEntity errandEntity, final EventType eventType, final EventSubType subtype, final ProcessCommand command, final boolean concludesDecision) {
		processEventPublisher.publish(errandEntity, eventType, subtype, getCallerIdentity(), getRequestGroupId(), command, concludesDecision);
	}

	private String extractId(final Revision currentRevision) {
		return ofNullable(currentRevision).map(Revision::getId).orElse(null);
	}

	private void saveDispatchEntry(final ErrandEntity errandEntity, final EventType eventType, final String requestGroupId, final String eventId, final String description, final String subType,
		final Identifier executingUser) {
		notificationDispatchRepository.save(NotificationDispatchEntity.create()
			.withEventId(eventId)
			.withRequestGroupId(requestGroupId)
			.withErrandId(errandEntity.getId())
			.withMunicipalityId(errandEntity.getMunicipalityId())
			.withNamespace(errandEntity.getNamespace())
			.withEventType(eventType.getValue())
			.withDescription(description)
			.withSubType(subType)
			.withExecutingUserId(ofNullable(executingUser).map(Identifier::getValue).orElse(null)));
	}

	private String extractEventId(final ResponseEntity<Void> response) {
		return ofNullable(response.getHeaders().getLocation())
			.map(URI::getPath)
			.map(path -> path.substring(path.lastIndexOf('/') + 1))
			.orElse(null);
	}

	private void createNotification(final ErrandEntity errandEntity, final generated.se.sundsvall.eventlog.Event event) {
		Optional.ofNullable(errandEntity.getAssignedUserId()).ifPresent(_ -> {
			final var notification = toNotification(event, errandEntity, getCallerIdentity());
			notificationService.createNotification(errandEntity.getMunicipalityId(), errandEntity.getNamespace(), errandEntity.getId(), notification);
		});
	}

	private String extractCaseId(final ErrandEntity errand) {
		return ofNullable(errand)
			.map(ErrandEntity::getExternalTags)
			.orElse(emptyList())
			.stream()
			.filter(et -> Objects.equals(EXTERNAL_TAG_KEY_CASE_ID, et.getKey()))
			.map(DbExternalTag::getValue)
			.findAny()
			.orElse(null);
	}
}
