package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.eventlog.Metadata;
import se.sundsvall.supportmanagement.api.model.event.EventMetaData;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CASE_ID;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CURRENT_REVISION;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CURRENT_VERSION;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_EXECUTED_BY;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_NOTE_ID;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_PREVIOUS_REVISION;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_PREVIOUS_VERSION;

public class EventlogMapper {

	private static final String OWNER = "SupportManagement";

	private EventlogMapper() {}

	public static Event toEvent(EventType eventType, String message, String revision, Class<?> sourceType, Map<String, String> metaData, String executedByUserId) {
		return new Event()
			.created(now(systemDefault()))
			.historyReference(revision)
			.message(message)
			.owner(OWNER)
			.sourceType(ofNullable(sourceType).map(Class::getSimpleName).orElse(null))
			.type(eventType)
			.metadata(toMetadata(metaData, ofNullable(executedByUserId)));
	}

	private static List<Metadata> toMetadata(Map<String, String> metadata, Optional<String> executedByUserId) {
		final var metadataList = new ArrayList<Metadata>();
		metadataList.addAll(toMetadatas(metadata));
		executedByUserId.ifPresent(userId -> metadataList.add(toMetadata(entry(EXTERNAL_TAG_KEY_EXECUTED_BY, userId))));

		return metadataList;
	}

	private static List<Metadata> toMetadatas(Map<String, String> metadata) {
		return ofNullable(metadata).orElse(emptyMap()).entrySet().stream()
			.map(EventlogMapper::toMetadata)
			.toList();
	}

	private static Metadata toMetadata(Entry<String, String> entry) {
		return new Metadata().key(entry.getKey()).value(entry.getValue());
	}

	public static Map<String, String> toMetadataMap(ErrandEntity errandEntity, Revision currentRevision, Revision previousRevision) {
		String caseId = ofNullable(errandEntity)
			.map(ErrandEntity::getExternalTags)
			.orElse(emptyList())
			.stream()
			.filter(et -> Objects.equals(EXTERNAL_TAG_KEY_CASE_ID, et.getKey()))
			.map(DbExternalTag::getValue)
			.findAny()
			.orElse(null);

		return toMetadataMap(caseId, null, currentRevision, previousRevision);
	}

	public static Map<String, String> toMetadataMap(String caseId, String noteId, Revision currentRevision, Revision previousRevision) {
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

		return metadata;
	}

	public static se.sundsvall.supportmanagement.api.model.event.Event toEvent(Event event) {
		return se.sundsvall.supportmanagement.api.model.event.Event.create()
			.withCreated(event.getCreated())
			.withHistoryReference(event.getHistoryReference())
			.withMessage(event.getMessage())
			.withMetadata(toMetadatas(event.getMetadata()))
			.withOwner(event.getOwner())
			.withSourceType(event.getSourceType())
			.withType(toEventType(event.getType()));
	}

	private static se.sundsvall.supportmanagement.api.model.event.EventType toEventType(EventType eventType) {
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

	private static List<EventMetaData> toMetadatas(List<Metadata> metadatas) {
		return Optional.ofNullable(metadatas).orElse(Collections.emptyList()).stream()
			.map(EventlogMapper::toMetadata)
			.toList();
	}

	private static EventMetaData toMetadata(Metadata metadata) {
		return EventMetaData.create()
			.withKey(metadata.getKey())
			.withValue(metadata.getValue());
	}
}
