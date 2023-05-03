package se.sundsvall.supportmanagement.service.mapper;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.Map;

import org.junit.jupiter.api.Test;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.eventlog.Metadata;

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

	@Test
	void toEventAllNulls() {

		assertThat(EventlogMapper.toEvent(null, null, null, null, null))
			.isNotNull()
			.hasAllNullFieldsOrPropertiesExcept("created", "owner", "sourceType", "metadata")
			.extracting(
				Event::getOwner,
				Event::getSourceType,
				Event::getMetadata)
			.containsExactly(
				OWNER,
				SOURCE_TYPE,
				emptyList());
	}

	@Test
	void toEventWithExecutingUserId() {
		// Setup
		final var userId = "userId";

		// Execute
		final var result = EventlogMapper.toEvent(EVENT_TYPE, MESSAGE, REVISION, META_DATA, userId);

		// Assert
		assertThat(result.getCreated()).isCloseTo(now(systemDefault()), within(2, SECONDS));
		assertThat(result.getExpires()).isNull();
		assertThat(result.getHistoryReference()).isEqualTo(REVISION);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getOwner()).isEqualTo(OWNER);
		assertThat(result.getSourceType()).isEqualTo(SOURCE_TYPE);
		assertThat(result.getType()).isEqualTo(EVENT_TYPE);
		assertThat(result.getMetadata()).hasSize(2)
			.extracting(
				Metadata::getKey, Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple(META_KEY, META_VALUE),
				tuple(EXECUTED_BY, userId));
	}

	@Test
	void toEventWithoutExecutingUserId() {
		// Execute
		final var result = EventlogMapper.toEvent(EVENT_TYPE, MESSAGE, REVISION, META_DATA, null);

		// Assert
		assertThat(result.getCreated()).isCloseTo(now(systemDefault()), within(2, SECONDS));
		assertThat(result.getExpires()).isNull();
		assertThat(result.getHistoryReference()).isEqualTo(REVISION);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getOwner()).isEqualTo(OWNER);
		assertThat(result.getSourceType()).isEqualTo(SOURCE_TYPE);
		assertThat(result.getType()).isEqualTo(EVENT_TYPE);
		assertThat(result.getMetadata()).hasSize(1)
			.extracting(
				Metadata::getKey, Metadata::getValue)
			.containsExactly(
				tuple(META_KEY, META_VALUE));
	}
}
