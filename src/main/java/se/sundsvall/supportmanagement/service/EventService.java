package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.notes.Note;
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
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.eventlog.EventlogClient;
import se.sundsvall.supportmanagement.service.mapper.EventlogMapper;
import se.sundsvall.supportmanagement.service.model.ProcessCommand;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CASE_ID;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.NOTE;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toEvent;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toMetadataMap;
import static se.sundsvall.supportmanagement.service.mapper.NotificationMapper.toNotification;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getExecutingUser;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getRequestGroupId;

@Service
public class EventService {

	private static final Logger LOG = LoggerFactory.getLogger(EventService.class);

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

	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision, final boolean sendNotification, final EventSubType subtype) {
		createErrandEvent(eventType, message, errandEntity, currentRevision, previousRevision, sendNotification, subtype, null);
	}

	/**
	 * Writes an errand event, and tells the process of the errand about it.
	 * <p>
	 * The command is what a request aimed at the process carries and publication cannot work out on its own - the key a
	 * handler chose to start, or the gate they stepped past. An ordinary errand event carries none.
	 *
	 * @param eventType        the type of the event.
	 * @param message          the text of the event.
	 * @param errandEntity     the errand the event is about.
	 * @param currentRevision  the revision the event points at, or null when the write made none.
	 * @param previousRevision the revision before it, if there is one.
	 * @param sendNotification whether the handler of the errand is to be notified. Says nothing about the process.
	 * @param subtype          what happened.
	 * @param command          the command the event carries, or null for an ordinary errand event.
	 */
	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision, final boolean sendNotification, final EventSubType subtype,
		final ProcessCommand command) {
		final var requestGroupId = getRequestGroupId();
		final var metadata = toMetadataMap(errandEntity, currentRevision, previousRevision);
		final var event = toEvent(eventType, message, extractId(currentRevision), Errand.class, metadata, getExecutingUser(), subtype.getValue(), requestGroupId);
		String eventId = null;
		try {
			eventId = extractEventId(eventLogClient.createEvent(errandEntity.getMunicipalityId(), errandEntity.getId(), event));
		} catch (final Exception e) {
			LOG.warn("Failed to create event log entry for errand {}: {}", sanitizeForLogging(errandEntity.getId()), sanitizeForLogging(e.getMessage()));
		}
		if (eventType != EventType.DELETE) {
			eventPublisher.publishEvent(new AutoSubscribeEvent(errandEntity));
		}

		if (sendNotification) {
			createNotification(errandEntity, event);
			saveDispatchEntry(errandEntity, eventType, requestGroupId, eventId, message, subtype.getValue());
		}

		// Last, and in the transaction of the change itself. The notification flag has no say here - an outbox row is no
		// notice to a handler but a message to a process.
		processEventPublisher.publish(errandEntity, eventType, subtype, executingIdentity(), requestGroupId, command);
	}

	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision, final EventSubType subtype) {
		createErrandEvent(eventType, message, errandEntity, currentRevision, previousRevision, true, subtype);
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
		createNotification(errandEntity, event);
		saveDispatchEntry(errandEntity, eventType, requestGroupId, eventId, message, NOTE.getValue());
	}

	public Page<Event> readEvents(final String namespace, final String municipalityId, final String id, final Pageable pageable) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, id, ProtectedResource.EVENT, LR);

		final var response = eventLogClient.getEvents(municipalityId, id, pageable);

		return new PageImpl<>(response.getContent().stream()
			.map(EventlogMapper::toEvent)
			.filter(event -> nonNull(event.getType()))
			.toList(), pageable, response.getTotalElements());
	}

	private String extractId(final Revision currentRevision) {
		return ofNullable(currentRevision).map(Revision::getId).orElse(null);
	}

	private void saveDispatchEntry(final ErrandEntity errandEntity, final EventType eventType, final String requestGroupId, final String eventId, final String description, final String subType) {
		final var executingUser = getExecutingUser();
		notificationDispatchRepository.save(NotificationDispatchEntity.create()
			.withEventId(eventId)
			.withRequestGroupId(requestGroupId)
			.withErrandId(errandEntity.getId())
			.withMunicipalityId(errandEntity.getMunicipalityId())
			.withNamespace(errandEntity.getNamespace())
			.withEventType(eventType.getValue())
			.withDescription(description)
			.withSubType(subType)
			.withExecutingUserId(Optional.ofNullable(executingUser).map(u -> u.getValue()).orElse(null)));
	}

	private String extractEventId(final ResponseEntity<Void> response) {
		return ofNullable(response.getHeaders().getLocation())
			.map(uri -> uri.getPath())
			.map(path -> path.substring(path.lastIndexOf('/') + 1))
			.orElse(null);
	}

	private void createNotification(final ErrandEntity errandEntity, final generated.se.sundsvall.eventlog.Event event) {
		Optional.ofNullable(errandEntity.getAssignedUserId()).ifPresent(_ -> {
			final var notification = toNotification(event, errandEntity, executingIdentity());
			notificationService.createNotification(errandEntity.getMunicipalityId(), errandEntity.getNamespace(), errandEntity.getId(), notification);
		});
	}

	/**
	 * Who the write was made by, which is what the notification says it came from and what the outbox row is stamped
	 * with.
	 * <p>
	 * Whatever the identifier of the request calls itself, whether that is an ad account or not - a process engine
	 * reporting on an errand is no ad account, and asking only for one would leave the handler with a notification from
	 * nobody and the outbox row with no trace of who wrote it.
	 */
	private static String executingIdentity() {
		return ofNullable(getExecutingUser())
			.map(Identifier::getValue)
			.orElse(null);
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
