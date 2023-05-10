package se.sundsvall.supportmanagement.service.mapper;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.eventlog.Metadata;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

public class EventlogMapper {

	private static final String OWNER = "SupportManagement";
	private static final String SOURCE_TYPE = Errand.class.getSimpleName();

	private static final String KEY_EXECUTED_BY = "ExecutedBy";
	private static final String KEY_CASE_ID = "CaseId";
	private static final String KEY_PREVIOUS_REVISION = "PreviousRevision";
	private static final String KEY_PREVIOUS_VERSION = "PreviousVersion";
	private static final String KEY_CURRENT_REVISION = "CurrentRevision";
	private static final String KEY_CURRENT_VERSION = "CurrentVersion";

	private EventlogMapper() {}

	public static Event toEvent(EventType eventType, String message, String revision, Map<String, String> metaData, String executedByUserId) {
		return new Event()
			.created(now(systemDefault()))
			.historyReference(revision)
			.message(message)
			.owner(OWNER)
			.sourceType(SOURCE_TYPE)
			.type(eventType)
			.metadata(toMetadata(metaData, ofNullable(executedByUserId)));
	}

	private static List<Metadata> toMetadata(Map<String, String> metadata, Optional<String> executedByUserId) {
		final var metadataList = new ArrayList<Metadata>();
		metadataList.addAll(toMetadatas(metadata));
		executedByUserId.ifPresent(userId -> metadataList.add(toMetadata(entry(KEY_EXECUTED_BY, userId))));

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
		final var metadata = new HashMap<String, String>();

		// Add information for current revision of errand
		ofNullable(currentRevision).ifPresent(rev -> {
			metadata.put(KEY_CURRENT_REVISION, rev.getId());
			metadata.put(KEY_CURRENT_VERSION, String.valueOf(rev.getVersion()));
		});

		// Add information for previous revision of errand
		ofNullable(previousRevision).ifPresent(rev -> {
			metadata.put(KEY_PREVIOUS_REVISION, rev.getId());
			metadata.put(KEY_PREVIOUS_VERSION, String.valueOf(rev.getVersion()));
		});

		// Add caseId to metadata if errand and external tag with caseId are present
		ofNullable(errandEntity)
			.map(ErrandEntity::getExternalTags)
			.orElse(emptyList())
			.stream()
			.filter(et -> Objects.equals(KEY_CASE_ID, et.getKey()))
			.map(DbExternalTag::getValue)
			.findAny()
			.ifPresent(value -> metadata.put(KEY_CASE_ID, value));

		return metadata;
	}
}
