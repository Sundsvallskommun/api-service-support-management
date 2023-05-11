package se.sundsvall.supportmanagement.service;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.eventlog.Metadata;
import generated.se.sundsvall.eventlog.PageEvent;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.eventlog.EventlogClient;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	@Mock
	private EventlogClient eventLogClientMock;

	@Mock
	private PageEvent pageEventMock;

	@Mock
	private Event eventMock;

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

	@Test
	void readEvents() {
		final var errandId = randomUUID().toString();
		final var pageable = Pageable.unpaged();

		when(eventLogClientMock.getEvents(errandId, pageable)).thenReturn(pageEventMock);
		when(pageEventMock.getContent()).thenReturn(List.of(eventMock, eventMock, eventMock));
		when(pageEventMock.getTotalElements()).thenReturn(3L);
		when(eventMock.getType()).thenReturn(EventType.CREATE, EventType.UPDATE, EventType.DELETE);

		final var pagedEvents = service.readEvents(errandId, pageable);

		verify(eventMock, times(3)).getType();
		verify(pageEventMock).getTotalElements();

		assertThat(pagedEvents.getContent()).hasSize(3)
			.extracting(
				se.sundsvall.supportmanagement.api.model.event.Event::getType)
			.containsExactly(
				se.sundsvall.supportmanagement.api.model.event.EventType.CREATE,
				se.sundsvall.supportmanagement.api.model.event.EventType.UPDATE,
				se.sundsvall.supportmanagement.api.model.event.EventType.DELETE);
	}

	@Test
	void readUnknownEvents() {
		final var errandId = randomUUID().toString();
		final var pageable = Pageable.unpaged();

		when(eventLogClientMock.getEvents(errandId, pageable)).thenReturn(pageEventMock);
		when(pageEventMock.getContent()).thenReturn(List.of(eventMock, eventMock, eventMock, eventMock, eventMock, eventMock));
		when(pageEventMock.getTotalElements()).thenReturn(6L);
		when(eventMock.getType()).thenReturn(EventType.ACCESS, EventType.CANCEL, EventType.DROP, EventType.EXECUTE, EventType.READ, null);

		final var pagedEvents = service.readEvents(errandId, pageable);

		verify(eventMock, times(6)).getType();
		verify(pageEventMock).getTotalElements();

		assertThat(pagedEvents.getContent()).hasSize(6)
			.extracting(se.sundsvall.supportmanagement.api.model.event.Event::getType)
			.containsOnly(se.sundsvall.supportmanagement.api.model.event.EventType.UNKNOWN);
	}
}