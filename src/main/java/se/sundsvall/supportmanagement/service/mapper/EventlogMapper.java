package se.sundsvall.supportmanagement.service.mapper;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.eventlog.Metadata;
import se.sundsvall.supportmanagement.api.model.errand.Errand;

public class EventlogMapper {

	private static final String EXECUTED_BY = "ExecutedBy";
	private static final String OWNER = "SupportManagement";
	private static final String SOURCE_TYPE = Errand.class.getSimpleName();

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
		executedByUserId.ifPresent(userId -> metadataList.add(toMetadata(entry(EXECUTED_BY, userId))));

		return metadataList;
	}

	private static List<Metadata> toMetadatas(Map<String, String> metadata) {
		return ofNullable(metadata).orElse(emptyMap()).entrySet().stream()
			.toList()
			.stream()
			.map(EventlogMapper::toMetadata)
			.toList();
	}

	private static Metadata toMetadata(Entry<String, String> entry) {
		return new Metadata().key(entry.getKey()).value(entry.getValue());
	}
}
