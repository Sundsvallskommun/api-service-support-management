package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.notes.Note;
import java.net.URI;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.eventlog.EventlogClient;
import se.sundsvall.supportmanagement.service.mapper.EventlogMapper;
import se.sundsvall.supportmanagement.service.model.ErrandEventOptions;
import se.sundsvall.supportmanagement.service.model.RevisionResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CASE_ID;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ASSIGNMENT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.LABEL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.NOTE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.PHASE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.STATUS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SYSTEM;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toEvent;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toMetadataMap;
import static se.sundsvall.supportmanagement.service.mapper.NotificationMapper.toNotification;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getAdUser;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getExecutingUser;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getRequestGroupId;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.shouldNotify;

@Service
public class EventService {

	private static final Logger LOG = LoggerFactory.getLogger(EventService.class);

	private static final String EVENT_LOG_ADD_LABEL = "Ärendet uppdaterat med etikett \"%s\"";
	private static final String EVENT_LOG_CHANGE_STATUS = "Status har ändrats till \"%s\"";

	private static final Set<EventSubType> LOGGED_ALONGSIDE_ERRAND_CHANGE = EnumSet.of(ASSIGNMENT, LABEL, PHASE, STATUS);

	private final EventlogClient eventLogClient;
	private final NotificationService notificationService;
	private final ApplicationEventPublisher eventPublisher;
	private final NotificationDispatchRepository notificationDispatchRepository;
	private final AccessControlService accessControlService;
	private final MetadataLabelRepository metadataLabelRepository;
	private final StatusRepository statusRepository;

	public EventService(final EventlogClient eventLogClient, final NotificationService notificationService, final ApplicationEventPublisher eventPublisher, final NotificationDispatchRepository notificationDispatchRepository,
		final AccessControlService accessControlService, final MetadataLabelRepository metadataLabelRepository, final StatusRepository statusRepository) {
		this.eventLogClient = eventLogClient;
		this.notificationService = notificationService;
		this.eventPublisher = eventPublisher;
		this.notificationDispatchRepository = notificationDispatchRepository;
		this.accessControlService = accessControlService;
		this.metadataLabelRepository = metadataLabelRepository;
		this.statusRepository = statusRepository;
	}

	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision, final boolean sendNotification,
		final EventSubType subtype) {
		createErrandEvent(eventType, message, errandEntity, new RevisionResult(previousRevision, currentRevision), subtype, new ErrandEventOptions(sendNotification, getExecutingUser(), Set.of()));
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
		createErrandEvent(eventType, message, errandEntity, new RevisionResult(previousRevision, currentRevision), subtype, new ErrandEventOptions(sendNotification, actor, Set.of()));
	}

	/**
	 * Logs the event and, unless the request asked to notify no one, queues it for the subscribers. The user who acted is
	 * recorded as such, and the labels the event added to the errand go along with it, as subscribers may filter on a
	 * label being added.
	 *
	 * @param revisions the revisions of the errand the event is about, either of which may be null
	 * @param options   what goes along with the event beyond the event itself
	 */
	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final RevisionResult revisions, final EventSubType subtype,
		final ErrandEventOptions options) {
		final var currentRevision = ofNullable(revisions).map(RevisionResult::latest).orElse(null);
		final var previousRevision = ofNullable(revisions).map(RevisionResult::previous).orElse(null);
		final var actor = options.actor();
		final var requestGroupId = getRequestGroupId();
		final var metadata = toMetadataMap(errandEntity, currentRevision, previousRevision);
		final var event = toEvent(eventType, message, extractId(currentRevision), Errand.class, metadata, actor, subtype.getValue(), requestGroupId);
		String eventId = null;
		try {
			eventId = extractEventId(eventLogClient.createEvent(errandEntity.getMunicipalityId(), errandEntity.getId(), event));
		} catch (final Exception e) {
			LOG.warn("Failed to create event log entry for errand {}: {}", sanitizeForLogging(errandEntity.getId()), sanitizeForLogging(e.getMessage()));
		}
		// An assignment, a label being added and a change of status or phase are logged alongside the change to the errand
		// they are part of, which already subscribes the assignee. A label added by a scheduled action has no such change,
		// and subscribes no one either.
		if (eventType != EventType.DELETE && !LOGGED_ALONGSIDE_ERRAND_CHANGE.contains(subtype)) {
			eventPublisher.publishEvent(new AutoSubscribeEvent(errandEntity, eventType == EventType.CREATE && subtype == EventSubType.ERRAND));
		}

		// A request that asked to notify no one reaches no one, neither those notified directly nor any subscriber on any
		// channel
		if (options.sendNotification() && shouldNotify()) {
			createNotification(errandEntity, event);
		}

		// Which subscribers hear of the event is up to their subscriptions
		if (shouldNotify()) {
			saveDispatchEntry(errandEntity, eventType, requestGroupId, eventId, message, subtype.getValue(), options);
		}
	}

	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision, final EventSubType subtype) {
		createErrandEvent(eventType, message, errandEntity, currentRevision, previousRevision, true, subtype);
	}

	/**
	 * Logs the status of the errand having changed to the one it has now, apart from the change it was part of, so that
	 * subscribers may be notified of it alone. No direct notification is made, as the change itself makes one.
	 *
	 * @param currentRevision  the revision of the errand the status changed in, or null
	 * @param previousRevision the revision before it, or null
	 */
	public void createStatusChangeEvent(final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision) {
		createErrandEvent(EventType.UPDATE, EVENT_LOG_CHANGE_STATUS.formatted(statusNameOf(errandEntity)), errandEntity, currentRevision, previousRevision, false, STATUS);
	}

	/**
	 * The name the status of the errand is told by, which is its display name, or the status itself for one without a
	 * display name or no longer defined in the namespace.
	 */
	private String statusNameOf(final ErrandEntity errandEntity) {
		return statusRepository.findByNamespaceAndMunicipalityIdAndName(errandEntity.getNamespace(), errandEntity.getMunicipalityId(), errandEntity.getStatus())
			.map(StatusEntity::getDisplayName)
			.filter(displayName -> !displayName.isBlank())
			.orElse(errandEntity.getStatus());
	}

	/**
	 * Logs the labels having been added to the errand, one event for every label, so that subscribers may be notified of a
	 * label being added apart from the change it was part of. Each event carries the one label it is about, which is what a
	 * filter on a label matches. No direct notification is made, as the change that added the labels makes one where there
	 * is one.
	 *
	 * @param revisions the revisions of the errand the labels were added in, either of which may be null
	 * @param labelIds  the ids of the metadata labels added
	 * @param actor     the user who added the labels, or null when nobody did
	 */
	public void createLabelEvents(final EventType eventType, final ErrandEntity errandEntity, final RevisionResult revisions, final Collection<String> labelIds, final Identifier actor) {
		if (labelIds == null || labelIds.isEmpty()) {
			return;
		}

		final var labelNames = metadataLabelRepository.findAllById(labelIds).stream()
			.collect(toMap(MetadataLabelEntity::getId, EventService::nameOf));

		labelIds.forEach(labelId -> createErrandEvent(eventType, EVENT_LOG_ADD_LABEL.formatted(labelNames.getOrDefault(labelId, labelId)), errandEntity, revisions, LABEL,
			new ErrandEventOptions(false, actor, Set.of(labelId))));
	}

	/**
	 * The name a label is told by, which is its display name, or its resource path for a label without one.
	 */
	private static String nameOf(final MetadataLabelEntity label) {
		return ofNullable(label.getDisplayName())
			.or(() -> ofNullable(label.getResourcePath()))
			.orElse(label.getId());
	}

	/**
	 * Logs a single, aggregated entry for a system-level operation that is not tied to one errand's revision diff, such
	 * as a label move. Logged under the operation's own id rather than an errand id, since no single errand's revision
	 * history is what this is about.
	 * <p>
	 * {@code startedBy} is taken as a parameter rather than read here through {@link #getExecutingUser()}: this is
	 * called from the background thread that carries a label move out, where the request-scoped thread-local behind
	 * {@code getExecutingUser()} was never set, and reading it there would silently record every such event as
	 * executed by nobody. The caller is expected to have captured it from the request thread that accepted the move.
	 */
	public void createLabelMoveEvent(final String municipalityId, final String labelId, final String startedBy, final String message) {
		createLabelOperationEvent(municipalityId, labelId, startedBy, message, "label move");
	}

	/**
	 * Logs a single, aggregated entry for a label merge, against the destination label's id - mirrors
	 * {@link #createLabelMoveEvent} for the same reasons: no single errand's revision history is what this is about,
	 * and {@code startedBy} is taken as a parameter since this too runs from the background thread carrying the merge
	 * out.
	 */
	public void createLabelMergeEvent(final String municipalityId, final String targetLabelId, final String startedBy, final String message) {
		createLabelOperationEvent(municipalityId, targetLabelId, startedBy, message, "label merge");
	}

	/**
	 * Shared by {@link #createLabelMoveEvent}/{@link #createLabelMergeEvent} - the two differ only in which operation
	 * a failure to log is reported as, and which label id the entry is logged against, not in how the entry itself is
	 * built or that logging it is best-effort.
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
		createNotification(errandEntity, event);
		if (shouldNotify()) {
			saveDispatchEntry(errandEntity, eventType, requestGroupId, eventId, message, NOTE.getValue(), new ErrandEventOptions(true, getExecutingUser(), Set.of()));
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

	private String extractId(final Revision currentRevision) {
		return ofNullable(currentRevision).map(Revision::getId).orElse(null);
	}

	private void saveDispatchEntry(final ErrandEntity errandEntity, final EventType eventType, final String requestGroupId, final String eventId, final String description, final String subType,
		final ErrandEventOptions options) {
		notificationDispatchRepository.save(NotificationDispatchEntity.create()
			.withEventId(eventId)
			.withRequestGroupId(requestGroupId)
			.withErrandId(errandEntity.getId())
			.withMunicipalityId(errandEntity.getMunicipalityId())
			.withNamespace(errandEntity.getNamespace())
			.withEventType(eventType.getValue())
			.withDescription(description)
			.withSubType(subType)
			.withAddedLabelIds(new HashSet<>(ofNullable(options.addedLabelIds()).orElse(Set.of())))
			.withExecutingUserId(ofNullable(options.actor()).map(Identifier::getValue).orElse(null)));
	}

	private String extractEventId(final ResponseEntity<Void> response) {
		return ofNullable(response.getHeaders().getLocation())
			.map(URI::getPath)
			.map(path -> path.substring(path.lastIndexOf('/') + 1))
			.orElse(null);
	}

	/**
	 * Notifies the assigned user of the event. The notification is made on their behalf, not on behalf of the one who
	 * acted, so it is not held to what the one who acted may do with the notifications of the errand - whoever may act on
	 * an errand may have its assigned user told of it. A notification that cannot be made is logged and left out, so that
	 * the subscribers are still notified of the event.
	 */
	private void createNotification(final ErrandEntity errandEntity, final generated.se.sundsvall.eventlog.Event event) {
		Optional.ofNullable(errandEntity.getAssignedUserId()).ifPresent(_ -> {
			try {
				notificationService.createNotification(errandEntity, toNotification(event, errandEntity, getAdUser()));
			} catch (final Exception e) {
				LOG.warn("Failed to notify the assigned user of errand {}: {}", sanitizeForLogging(errandEntity.getId()), sanitizeForLogging(e.getMessage()));
			}
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
