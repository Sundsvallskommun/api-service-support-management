package se.sundsvall.supportmanagement.service.scheduler.messagingoutbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import generated.se.sundsvall.messaging.EmailBatchRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.MessagingOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.MessagingOutboxEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.service.scheduler.messagingoutbox.MessagingOutboxWorker.TYPE_EMAIL;

@ExtendWith(MockitoExtension.class)
class MessagingOutboxWorkerTest {

	@Mock
	private MessagingOutboxRepository repositoryMock;

	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private ObjectMapper objectMapperMock;

	@InjectMocks
	private MessagingOutboxWorker worker;

	@Test
	void fetchProcessable() {
		final var entry = MessagingOutboxEntity.create().withId("id-1");
		when(repositoryMock.findProcessable(any(OffsetDateTime.class))).thenReturn(List.of(entry));

		final var result = worker.fetchProcessable();

		assertThat(result).containsExactly(entry);
		verify(repositoryMock).findProcessable(any(OffsetDateTime.class));
		verifyNoMoreInteractions(repositoryMock);
		verifyNoInteractions(messagingClientMock, objectMapperMock);
	}

	@Test
	void enqueue() throws Exception {
		final var municipalityId = "2281";
		final var request = new EmailBatchRequest();
		final var payload = "{\"subject\":\"test\"}";

		when(objectMapperMock.writeValueAsString(request)).thenReturn(payload);

		worker.enqueue(municipalityId, request);

		final var captor = ArgumentCaptor.forClass(MessagingOutboxEntity.class);
		verify(repositoryMock).save(captor.capture());
		final var saved = captor.getValue();
		assertThat(saved.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(saved.getMessageType()).isEqualTo(TYPE_EMAIL);
		assertThat(saved.getPayload()).isEqualTo(payload);
		verifyNoMoreInteractions(repositoryMock, objectMapperMock);
		verifyNoInteractions(messagingClientMock);
	}

	@Test
	void enqueueThrowsWhenSerializationFails() throws Exception {
		final var request = new EmailBatchRequest();
		when(objectMapperMock.writeValueAsString(request)).thenThrow(new RuntimeException("serialize error"));

		assertThatThrownBy(() -> worker.enqueue("2281", request))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Failed to serialize email batch request for outbox");
	}

	@Test
	void processEmailEntrySuccessfully() throws Exception {
		final var payload = "{\"subject\":\"test\"}";
		final var entry = MessagingOutboxEntity.create()
			.withId("id-1")
			.withMunicipalityId("2281")
			.withMessageType(TYPE_EMAIL)
			.withPayload(payload);
		final var emailBatchRequest = new EmailBatchRequest();

		when(objectMapperMock.readValue(payload, EmailBatchRequest.class)).thenReturn(emailBatchRequest);

		worker.process(entry);

		verify(objectMapperMock).readValue(payload, EmailBatchRequest.class);
		verify(messagingClientMock).sendEmailBatch("2281", emailBatchRequest);
		verify(repositoryMock).delete(entry);
		verifyNoMoreInteractions(repositoryMock, messagingClientMock, objectMapperMock);
	}

	@Test
	void processUnknownMessageTypeMarksAsDeadLetter() {
		final var entry = MessagingOutboxEntity.create()
			.withId("id-1")
			.withMessageType("UNKNOWN");

		worker.process(entry);

		assertThat(entry.isDeadLetter()).isTrue();
		verify(repositoryMock).save(entry);
		verifyNoMoreInteractions(repositoryMock);
		verifyNoInteractions(messagingClientMock, objectMapperMock);
	}

	@Test
	void processEmailEntryRetiesOnFailure() throws Exception {
		final var payload = "{\"subject\":\"test\"}";
		final var entry = MessagingOutboxEntity.create()
			.withId("id-1")
			.withMunicipalityId("2281")
			.withMessageType(TYPE_EMAIL)
			.withPayload(payload);
		final var emailBatchRequest = new EmailBatchRequest();

		when(objectMapperMock.readValue(payload, EmailBatchRequest.class)).thenReturn(emailBatchRequest);
		when(messagingClientMock.sendEmailBatch(any(), any())).thenThrow(new RuntimeException("network error"));

		worker.process(entry);

		assertThat(entry.getRetryCount()).isEqualTo(1);
		assertThat(entry.isDeadLetter()).isFalse();
		assertThat(entry.getNextRetryAt()).isCloseTo(OffsetDateTime.now().plusMinutes(5), within(5, SECONDS));
		verify(repositoryMock).save(entry);
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void processEmailEntryMarksAsDeadLetterAfterMaxRetries() throws Exception {
		final var payload = "{\"subject\":\"test\"}";
		final var entry = MessagingOutboxEntity.create()
			.withId("id-1")
			.withMunicipalityId("2281")
			.withMessageType(TYPE_EMAIL)
			.withPayload(payload);
		entry.setRetryCount(4);

		final var emailBatchRequest = new EmailBatchRequest();
		when(objectMapperMock.readValue(payload, EmailBatchRequest.class)).thenReturn(emailBatchRequest);
		when(messagingClientMock.sendEmailBatch(any(), any())).thenThrow(new RuntimeException("network error"));

		worker.process(entry);

		assertThat(entry.isDeadLetter()).isTrue();
		verify(repositoryMock).save(entry);
		verifyNoMoreInteractions(repositoryMock);
	}
}
