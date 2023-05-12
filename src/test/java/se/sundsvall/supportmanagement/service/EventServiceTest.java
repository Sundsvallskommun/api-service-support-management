package se.sundsvall.supportmanagement.service;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.eventlog.Metadata;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.eventlog.EventlogClient;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	@Mock
	private EventlogClient eventLogClientMock;

	@InjectMocks
	private EventService service;

	@Captor
	private ArgumentCaptor<Event> eventCaptor;

	@Test
	void createEventWithAllDataPresent() {
		// Setup
		final var eventType = EventType.UPDATE;
		final var message = "message";
		final var errandId = randomUUID().toString();
		final var currentRevisionId = randomUUID().toString();
		final var currentRevisionVersion = 14;
		final var previousRevisionId = randomUUID().toString();
		final var previousRevisionVersion = 13;
		final var owner = "SupportManagement";
		final var sourceType = Errand.class.getSimpleName();

		final var entity = ErrandEntity.create().withId(errandId);
		final var currentRevision = Revision.create().withId(currentRevisionId).withVersion(currentRevisionVersion);
		final var previousRevision = Revision.create().withId(previousRevisionId).withVersion(previousRevisionVersion);
		final var executingUserId = "executingUserId";

		// Call
		service.createEvent(eventType, message, entity, currentRevision, previousRevision, executingUserId);

		// Verifications and assertions
		verify(eventLogClientMock).createEvent(eq(errandId), eventCaptor.capture());

		final var event = eventCaptor.getValue();
		assertThat(event.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(event.getExpires()).isNull();
		assertThat(event.getHistoryReference()).isEqualTo(currentRevisionId);
		assertThat(event.getMessage()).isEqualTo(message);
		assertThat(event.getMetadata()).isNotNull()
			.extracting(
				Metadata::getKey,
				Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple("CurrentVersion", String.valueOf(currentRevisionVersion)),
				tuple("CurrentRevision", currentRevisionId),
				tuple("PreviousVersion", String.valueOf(previousRevisionVersion)),
				tuple("PreviousRevision", previousRevisionId),
				tuple("ExecutedBy", executingUserId));
		assertThat(event.getOwner()).isEqualTo(owner);
		assertThat(event.getSourceType()).isEqualTo(sourceType);
		assertThat(event.getType()).isEqualTo(eventType);
	}

	@Test
	void createEventWithNoCurrentRevisionOrExecutingUser() {
		// Setup
		final var eventType = EventType.CREATE;
		final var message = "message";
		final var errandId = randomUUID().toString();
		final var owner = "SupportManagement";
		final var sourceType = Errand.class.getSimpleName();

		final var entity = ErrandEntity.create().withId(errandId);

		// Call
		service.createEvent(eventType, message, entity, null, null, null);

		// Verifications and assertions
		verify(eventLogClientMock).createEvent(eq(errandId), eventCaptor.capture());

		final var event = eventCaptor.getValue();
		assertThat(event.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(event.getExpires()).isNull();
		assertThat(event.getHistoryReference()).isNull();
		assertThat(event.getMessage()).isEqualTo(message);
		assertThat(event.getMetadata()).isEmpty();
		assertThat(event.getOwner()).isEqualTo(owner);
		assertThat(event.getSourceType()).isEqualTo(sourceType);
		assertThat(event.getType()).isEqualTo(eventType);
	}

	@Test
	void createEventWithNoPreviousRevisionOrExecutingUser() {
		// Setup
		final var eventType = EventType.CREATE;
		final var message = "message";
		final var errandId = randomUUID().toString();
		final var currentRevisionId = randomUUID().toString();
		final var currentRevisionVersion = 0;
		final var owner = "SupportManagement";
		final var sourceType = Errand.class.getSimpleName();

		final var entity = ErrandEntity.create().withId(errandId);
		final var currentRevision = Revision.create().withId(currentRevisionId).withVersion(currentRevisionVersion);

		// Call
		service.createEvent(eventType, message, entity, currentRevision, null, null);

		// Verifications and assertions
		verify(eventLogClientMock).createEvent(eq(errandId), eventCaptor.capture());

		final var event = eventCaptor.getValue();
		assertThat(event.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(event.getExpires()).isNull();
		assertThat(event.getHistoryReference()).isEqualTo(currentRevisionId);
		assertThat(event.getMessage()).isEqualTo(message);
		assertThat(event.getMetadata()).isNotNull()
			.extracting(
				Metadata::getKey,
				Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple("CurrentVersion", String.valueOf(currentRevisionVersion)),
				tuple("CurrentRevision", currentRevisionId));
		assertThat(event.getOwner()).isEqualTo(owner);
		assertThat(event.getSourceType()).isEqualTo(sourceType);
		assertThat(event.getType()).isEqualTo(eventType);
	}
}
