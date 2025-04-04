package se.sundsvall.supportmanagement.service.mapper;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.eventlog.Metadata;
import generated.se.sundsvall.notes.Note;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.event.EventMetaData;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

class EventlogMapperTest {

	private static final String OWNER = "SupportManagement";
	private static final String SOURCE_TYPE = "Errand";
	private static final String EXECUTED_BY = "ExecutedBy";
	private static final EventType EVENT_TYPE = EventType.CREATE;
	private static final String MESSAGE = "message";
	private static final String REVISION = "revision";
	private static final String META_KEY = "metaKey";
	private static final String META_VALUE = "metaValue";
	private static final Map<String, String> META_DATA = Map.of(META_KEY, META_VALUE);
	private static final String KEY_CASE_ID = "CaseId";
	private static final String KEY_PREVIOUS_REVISION = "PreviousRevision";
	private static final String KEY_PREVIOUS_VERSION = "PreviousVersion";
	private static final String KEY_CURRENT_REVISION = "CurrentRevision";
	private static final String KEY_CURRENT_VERSION = "CurrentVersion";
	private static final String CASE_ID = "caseId";
	private static final String PREVIOUS_ID = "previousRevisionId";
	private static final int PREVIOUS_VERSION = 123;
	private static final String CURRENT_ID = "currentRevisionId";
	private static final int CURRENT_VERSION = 456;

	@Test
	void toEventAllNulls() {
		assertThat(EventlogMapper.toEvent(null, null, null, null, null, null))
			.isNotNull()
			.hasAllNullFieldsOrPropertiesExcept("created", "owner", "metadata")
			.extracting(
				Event::getOwner,
				Event::getMetadata)
			.containsExactly(
				OWNER,
				emptyList());
	}

	@ParameterizedTest
	@ValueSource(classes = {
		Errand.class, Note.class
	})
	void toEventWithExecutingUserId(Class<?> clazz) {
		// Setup
		final var userId = "userId";

		// Execute
		final var result = EventlogMapper.toEvent(EVENT_TYPE, MESSAGE, REVISION, clazz, META_DATA, userId);

		// Assert
		assertThat(result.getCreated()).isCloseTo(now(systemDefault()), within(2, SECONDS));
		assertThat(result.getExpires()).isNull();
		assertThat(result.getHistoryReference()).isEqualTo(REVISION);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getOwner()).isEqualTo(OWNER);
		assertThat(result.getSourceType()).isEqualTo(clazz.getSimpleName());
		assertThat(result.getType()).isEqualTo(EVENT_TYPE);
		assertThat(result.getMetadata()).hasSize(2)
			.extracting(
				Metadata::getKey, Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple(META_KEY, META_VALUE),
				tuple(EXECUTED_BY, userId));
	}

	@ParameterizedTest
	@ValueSource(classes = {
		Errand.class, Note.class
	})
	void toEventWithoutExecutingUserId(Class<?> clazz) {
		// Execute
		final var result = EventlogMapper.toEvent(EVENT_TYPE, MESSAGE, REVISION, clazz, META_DATA, null);

		// Assert
		assertThat(result.getCreated()).isCloseTo(now(systemDefault()), within(2, SECONDS));
		assertThat(result.getExpires()).isNull();
		assertThat(result.getHistoryReference()).isEqualTo(REVISION);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getOwner()).isEqualTo(OWNER);
		assertThat(result.getSourceType()).isEqualTo(clazz.getSimpleName());
		assertThat(result.getType()).isEqualTo(EVENT_TYPE);
		assertThat(result.getMetadata()).hasSize(1)
			.extracting(
				Metadata::getKey, Metadata::getValue)
			.containsExactly(
				tuple(META_KEY, META_VALUE));
	}

	@ParameterizedTest
	@MethodSource("metadataMapArgumentProvider")
	void toMetadataMap(ErrandEntity errandEntity, Revision currentRevision, Revision previousRevision, Map<String, String> result) {
		assertThat(EventlogMapper.toMetadataMap(errandEntity, currentRevision, previousRevision)).containsExactlyInAnyOrderEntriesOf(result);
	}

	private static Stream<Arguments> metadataMapArgumentProvider() {
		return Stream.of(
			Arguments.of(ErrandEntity.create().withExternalTags(List.of(DbExternalTag.create().withKey(KEY_CASE_ID).withValue(CASE_ID))), null, null, Map.of(KEY_CASE_ID, CASE_ID)),
			Arguments.of(null, null, Revision.create().withId(PREVIOUS_ID).withVersion(PREVIOUS_VERSION), Map.of(KEY_PREVIOUS_REVISION, PREVIOUS_ID, KEY_PREVIOUS_VERSION, String.valueOf(PREVIOUS_VERSION))),
			Arguments.of(null, Revision.create().withId(CURRENT_ID).withVersion(CURRENT_VERSION), null, Map.of(KEY_CURRENT_REVISION, CURRENT_ID, KEY_CURRENT_VERSION, String.valueOf(CURRENT_VERSION))),
			Arguments.of(null, null, null, emptyMap()));
	}

	@ParameterizedTest
	@MethodSource("eventFromEventlogEventArgumentProvider")
	void toEventFromEventlogEvent(EventType returnedType, se.sundsvall.supportmanagement.api.model.event.EventType mappedType) {
		final var created = now(systemDefault());
		final var eventlogEvent = new Event()
			.created(created)
			.historyReference(CURRENT_ID)
			.message(MESSAGE)
			.metadata(List.of(new Metadata().key(META_KEY).value(META_VALUE)))
			.owner(OWNER)
			.sourceType(SOURCE_TYPE)
			.type(returnedType);

		final var event = EventlogMapper.toEvent(eventlogEvent);

		assertThat(event).isNotNull()
			.extracting(
				se.sundsvall.supportmanagement.api.model.event.Event::getCreated,
				se.sundsvall.supportmanagement.api.model.event.Event::getHistoryReference,
				se.sundsvall.supportmanagement.api.model.event.Event::getMessage,
				se.sundsvall.supportmanagement.api.model.event.Event::getMetadata,
				se.sundsvall.supportmanagement.api.model.event.Event::getOwner,
				se.sundsvall.supportmanagement.api.model.event.Event::getSourceType,
				se.sundsvall.supportmanagement.api.model.event.Event::getType)
			.containsExactly(
				created,
				CURRENT_ID,
				MESSAGE,
				List.of(EventMetaData.create().withKey(META_KEY).withValue(META_VALUE)),
				OWNER,
				SOURCE_TYPE,
				mappedType);
	}

	private static Stream<Arguments> eventFromEventlogEventArgumentProvider() {
		return Stream.of(
			Arguments.of(EventType.ACCESS, se.sundsvall.supportmanagement.api.model.event.EventType.UNKNOWN),
			Arguments.of(EventType.CANCEL, se.sundsvall.supportmanagement.api.model.event.EventType.UNKNOWN),
			Arguments.of(EventType.CREATE, se.sundsvall.supportmanagement.api.model.event.EventType.CREATE),
			Arguments.of(EventType.DELETE, se.sundsvall.supportmanagement.api.model.event.EventType.DELETE),
			Arguments.of(EventType.DROP, se.sundsvall.supportmanagement.api.model.event.EventType.UNKNOWN),
			Arguments.of(EventType.EXECUTE, se.sundsvall.supportmanagement.api.model.event.EventType.UNKNOWN),
			Arguments.of(EventType.READ, se.sundsvall.supportmanagement.api.model.event.EventType.UNKNOWN),
			Arguments.of(EventType.UPDATE, se.sundsvall.supportmanagement.api.model.event.EventType.UPDATE));
	}

	@Test
	void toEventFromEmptyEventlogEvent() {
		assertThat(EventlogMapper.toEvent(new Event()))
			.isNotNull()
			.hasAllNullFieldsOrPropertiesExcept("type", "metadata")
			.extracting(
				se.sundsvall.supportmanagement.api.model.event.Event::getType,
				se.sundsvall.supportmanagement.api.model.event.Event::getMetadata)
			.contains(
				se.sundsvall.supportmanagement.api.model.event.EventType.UNKNOWN,
				emptyList());
	}
}
