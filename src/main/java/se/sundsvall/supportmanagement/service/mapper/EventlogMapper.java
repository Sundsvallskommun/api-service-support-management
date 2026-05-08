package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.eventlog.ExecutingUser;
import generated.se.sundsvall.eventlog.Metadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.event.EventMetaData;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CASE_ID;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CHANNEL;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CURRENT_REVISION;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CURRENT_VERSION;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_EXECUTED_BY;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_NAMESPACE;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_NOTE_ID;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_PREVIOUS_REVISION;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_PREVIOUS_VERSION;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_STATUS;

public class EventlogMapper {

	private static final String OWNER = "SupportManagement";

	private EventlogMapper() {}

	public static Event toEvent(final EventType eventType, final String message, final String revision, final Class<?> sourceType, final Map<String, String> metaData, final Identifier executedBy, final String subType, final String requestGroupId) {
		return new Event()
			.created(now(systemDefault()))
			.historyReference(revision)
			.message(message)
			.owner(OWNER)
			.sourceType(ofNullable(sourceType).map(Class::getSimpleName).orElse(null))
			.type(eventType)
			.subType(subType)
			.requestGroupId(requestGroupId)
			.executingUser(toExecutingUser(executedBy))
			.metadata(toMetadata(metaData, ofNullable(executedBy)));
	}

	private static ExecutingUser toExecutingUser(final Identifier identifier) {
		return ofNullable(identifier)
			.map(id -> new ExecutingUser()
				.type(AD_ACCOUNT.equals(id.getType()) ? ExecutingUser.TypeEnum.AD_USER : ExecutingUser.TypeEnum.PARTY_ID)
				.value(id.getValue()))
			.orElse(null);
	}

	private static List<Metadata> toMetadata(final Map<String, String> metadata, final Optional<Identifier> executedBy) {
		final var metadataList = new ArrayList<Metadata>();
		metadataList.addAll(toMetadatas(metadata));
		// ExecutedBy kept for backwards compatibility — superseded by executingUser field
		executedBy.ifPresent(id -> metadataList.add(toMetadata(entry(EXTERNAL_TAG_KEY_EXECUTED_BY, id.getValue()))));

		return metadataList;
	}

	private static List<Metadata> toMetadatas(final Map<String, String> metadata) {
		return ofNullable(metadata).orElse(emptyMap()).entrySet().stream()
			.map(EventlogMapper::toMetadata)
			.toList();
	}

	private static Metadata toMetadata(final Entry<String, String> entry) {
		return new Metadata().key(entry.getKey()).value(entry.getValue());
	}

	public static Map<String, String> toMetadataMap(final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision) {
		final String caseId = ofNullable(errandEntity)
			.map(ErrandEntity::getExternalTags)
			.orElse(emptyList())
			.stream()
			.filter(et -> Objects.equals(EXTERNAL_TAG_KEY_CASE_ID.toLowerCase(),
				Optional.ofNullable(et.getKey()).map(String::toLowerCase).orElse("")))
			.map(DbExternalTag::getValue)
			.filter(Objects::nonNull)
			.findAny()
			.orElse(null);

		final var namespace = ofNullable(errandEntity)
			.map(ErrandEntity::getNamespace)
			.orElse(null);

		final var channel = ofNullable(errandEntity)
			.map(ErrandEntity::getChannel)
			.orElse(null);

		final var status = ofNullable(errandEntity)
			.map(ErrandEntity::getStatus)
			.orElse(null);

		return toMetadataMap(caseId, null, currentRevision, previousRevision, namespace, channel, status);
	}

	public static Map<String, String> toMetadataMap(final String caseId, final String noteId, final Revision currentRevision, final Revision previousRevision, final String namespace) {
		return toMetadataMap(caseId, noteId, currentRevision, previousRevision, namespace, null, null);
	}

	public static Map<String, String> toMetadataMap(final String caseId, final String noteId, final Revision currentRevision, final Revision previousRevision, final String namespace, final String channel, final String status) {
		final var metadata = new HashMap<String, String>();

		// Add caseId to metadata if present
		ofNullable(caseId).ifPresent(value -> metadata.put(EXTERNAL_TAG_KEY_CASE_ID, value));

		// Add noteId to metadata if present
		ofNullable(noteId).ifPresent(value -> metadata.put(EXTERNAL_TAG_KEY_NOTE_ID, value));

		// Add information for current revision of note
		ofNullable(currentRevision).ifPresent(rev -> {
			metadata.put(EXTERNAL_TAG_KEY_CURRENT_REVISION, rev.getId());
			metadata.put(EXTERNAL_TAG_KEY_CURRENT_VERSION, String.valueOf(rev.getVersion()));
		});

		// Add information for previous revision of errand
		ofNullable(previousRevision).ifPresent(rev -> {
			metadata.put(EXTERNAL_TAG_KEY_PREVIOUS_REVISION, rev.getId());
			metadata.put(EXTERNAL_TAG_KEY_PREVIOUS_VERSION, String.valueOf(rev.getVersion()));
		});

		ofNullable(namespace).ifPresent(value -> metadata.put(EXTERNAL_TAG_KEY_NAMESPACE, value));
		ofNullable(channel).ifPresent(value -> metadata.put(EXTERNAL_TAG_KEY_CHANNEL, value));
		ofNullable(status).ifPresent(value -> metadata.put(EXTERNAL_TAG_KEY_STATUS, value));

		return metadata;
	}

	public static se.sundsvall.supportmanagement.api.model.event.Event toEvent(final Event event) {
		return se.sundsvall.supportmanagement.api.model.event.Event.create()
			.withId(event.getId())
			.withCreated(event.getCreated())
			.withHistoryReference(event.getHistoryReference())
			.withMessage(event.getMessage())
			.withDetails(event.getDetails())
			.withMetadata(toMetadatas(event.getMetadata()))
			.withOwner(event.getOwner())
			.withSourceType(event.getSourceType())
			.withType(toEventType(event.getType()))
			.withSubType(event.getSubType())
			.withRequestGroupId(event.getRequestGroupId());
	}

	private static se.sundsvall.supportmanagement.api.model.event.EventType toEventType(final EventType eventType) {
		if (eventType == null) {
			return se.sundsvall.supportmanagement.api.model.event.EventType.UNKNOWN;
		}

		return switch (eventType) {
			case CREATE -> se.sundsvall.supportmanagement.api.model.event.EventType.CREATE;
			case UPDATE -> se.sundsvall.supportmanagement.api.model.event.EventType.UPDATE;
			case DELETE -> se.sundsvall.supportmanagement.api.model.event.EventType.DELETE;
			default -> se.sundsvall.supportmanagement.api.model.event.EventType.UNKNOWN;
		};
	}

	private static List<EventMetaData> toMetadatas(final List<Metadata> metadatas) {
		return Optional.ofNullable(metadatas).orElse(Collections.emptyList()).stream()
			.map(EventlogMapper::toMetadata)
			.toList();
	}

	private static EventMetaData toMetadata(final Metadata metadata) {
		return EventMetaData.create()
			.withKey(metadata.getKey())
			.withValue(metadata.getValue());
	}
}
