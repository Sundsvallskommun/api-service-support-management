package se.sundsvall.supportmanagement.service.scheduler.emaildispatch;

import generated.se.sundsvall.employee.PortalPersonData;
import generated.se.sundsvall.messaging.EmailRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.EmailDispatchOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailDispatchOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;
import se.sundsvall.supportmanagement.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.supportmanagement.service.EmployeeService;
import se.sundsvall.supportmanagement.service.model.MessagingSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL;

@ExtendWith(MockitoExtension.class)
class SubscriberEmailServiceTest {

	private static final String ERRAND_ID = "errand-id";
	private static final String ERRAND_NUMBER = "PRH-2022-000001";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";

	@Mock
	private EmailDispatchOutboxRepository outboxRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Mock
	private MessagingSettingsIntegration messagingSettingsIntegrationMock;

	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private EmployeeService employeeServiceMock;

	@InjectMocks
	private SubscriberEmailService service;

	@Captor
	private ArgumentCaptor<EmailDispatchOutboxEntity> outboxCaptor;

	@Captor
	private ArgumentCaptor<EmailRequest> emailRequestCaptor;

	@Test
	void enqueueWithDestination() {
		final var channel = NotificationChannelEmbeddable.create()
			.withType(EMAIL)
			.withDestination("test@example.com");
		final var subscriber = createSubscriber();
		final var events = List.of(
			NotificationDispatchEntity.create().withEventType("UPDATE").withDescription("Bilaga har skapats"));

		service.enqueue(ERRAND_ID, ERRAND_NUMBER, channel, subscriber, events);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		final var saved = outboxCaptor.getValue();
		assertThat(saved.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(saved.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(saved.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(saved.getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(saved.getSubscriberId()).isEqualTo("subscriber-id");
		assertThat(saved.getRecipientEmail()).isEqualTo("test@example.com");
		assertThat(saved.getIdentifierType()).isEqualTo("AD_ACCOUNT");
		assertThat(saved.getIdentifierValue()).isEqualTo("joe01doe");
		assertThat(saved.getEventSummary()).isEqualTo("Bilaga har skapats");
	}

	@Test
	void enqueueWithoutDestination() {
		final var channel = NotificationChannelEmbeddable.create().withType(EMAIL);
		final var subscriber = createSubscriber();
		final var events = List.of(
			NotificationDispatchEntity.create().withEventType("CREATE").withDescription("Ärende skapat"),
			NotificationDispatchEntity.create().withEventType("UPDATE").withDescription("Status ändrad"));

		service.enqueue(ERRAND_ID, ERRAND_NUMBER, channel, subscriber, events);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		final var saved = outboxCaptor.getValue();
		assertThat(saved.getRecipientEmail()).isNull();
		assertThat(saved.getEventSummary()).isEqualTo("Ärende skapat; Status ändrad");
	}

	@Test
	void fetchPendingWithNoEntries() {
		when(outboxRepositoryMock.findByAttemptsLessThanOrderByCreatedAsc(0)).thenReturn(List.of());

		final var result = service.fetchPending();

		assertThat(result).isEmpty();
		verifyNoInteractions(messagingClientMock);
	}

	@Test
	void processEntryWithDirectEmail() {
		final var entry = createOutboxEntry().withRecipientEmail("direct@example.com");
		final var settings = createMessagingSettings();

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(messagingSettingsIntegrationMock.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, NAMESPACE)).thenReturn(settings);

		service.processEntry(entry);

		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		final var request = emailRequestCaptor.getValue();
		assertThat(request.getRecipients()).containsExactly("direct@example.com");
		assertThat(request.getSubject()).contains(ERRAND_NUMBER);
		verify(outboxRepositoryMock).delete(entry);
	}

	@Test
	void processEntryWithEmployeeFallback() {
		final var entry = createOutboxEntry()
			.withIdentifierType("AD_ACCOUNT")
			.withIdentifierValue("joe01doe");
		final var settings = createMessagingSettings();
		final var portalPersonData = new PortalPersonData().email("joe@example.com");

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(messagingSettingsIntegrationMock.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, NAMESPACE)).thenReturn(settings);
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe")).thenReturn(portalPersonData);

		service.processEntry(entry);

		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		assertThat(emailRequestCaptor.getValue().getRecipients()).containsExactly("joe@example.com");
		verify(outboxRepositoryMock).delete(entry);
	}

	@Test
	void processEntryWithUnresolvableEmail() {
		final var entry = createOutboxEntry()
			.withIdentifierType("AD_ACCOUNT")
			.withIdentifierValue("joe01doe");

		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe")).thenReturn(null);

		service.processEntry(entry);

		verify(messagingClientMock, never()).sendEmail(any(), anyBoolean(), any());
		verify(outboxRepositoryMock).delete(entry);
	}

	@Test
	void processEntryIncrementsAttemptsOnFailure() {
		final var entry = createOutboxEntry().withRecipientEmail("fail@example.com").withAttempts(1);
		final var settings = createMessagingSettings();

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(messagingSettingsIntegrationMock.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, NAMESPACE)).thenReturn(settings);
		doThrow(new RuntimeException("send failed")).when(messagingClientMock).sendEmail(any(), anyBoolean(), any());

		service.processEntry(entry);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		assertThat(outboxCaptor.getValue().getAttempts()).isEqualTo(2);
		assertThat(outboxCaptor.getValue().getLastAttempted()).isNotNull();
		verify(outboxRepositoryMock, never()).delete(any());
	}

	private static SubscriberEntity createSubscriber() {
		return SubscriberEntity.create()
			.withId("subscriber-id")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withIdentifier(IdentifierEmbeddable.create()
				.withType("AD_ACCOUNT")
				.withValue("joe01doe"));
	}

	private static EmailDispatchOutboxEntity createOutboxEntry() {
		return EmailDispatchOutboxEntity.create()
			.withId("outbox-id")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandId(ERRAND_ID)
			.withErrandNumber(ERRAND_NUMBER)
			.withSubscriberId("subscriber-id");
	}

	private static MessagingSettings createMessagingSettings() {
		return new MessagingSettings(
			"Hej %s, ärende %s har uppdaterats. Se %s för detaljer. Ref: %s",
			null,
			"https://support.example.com",
			"https://katla.example.com",
			"SupportSMS",
			"noreply@example.com",
			"Support");
	}
}
