package se.sundsvall.supportmanagement.service.scheduler.emaildispatch;

import generated.se.sundsvall.employee.PortalPersonData;
import generated.se.sundsvall.messaging.EmailRequest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.EmailDispatchOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailDispatchOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.EmailDispatchOutboxEventEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;
import se.sundsvall.supportmanagement.service.EmployeeService;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.INTERNAL;

@ExtendWith(MockitoExtension.class)
class SubscriberEmailServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String DISPLAY_NAME = "Avvikelser";
	private static final String SUBSCRIBER_ID = "subscriber-id";
	private static final String ERRAND_ID_1 = "errand-id-1";
	private static final String ERRAND_ID_2 = "errand-id-2";
	private static final Map<String, String> ERRAND_NUMBERS = Map.of(ERRAND_ID_1, "ABC123", ERRAND_ID_2, "ABC456");
	private static final String SENDER_EMAIL = "noreply@example.com";
	private static final String SENDER_NAME = "Support";
	private static final Duration MAX_AGE = Duration.ofDays(30);

	@Mock
	private EmailDispatchOutboxRepository outboxRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Mock
	private NamespaceConfigMapper namespaceConfigMapperMock;

	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private EmployeeService employeeServiceMock;

	@Captor
	private ArgumentCaptor<EmailDispatchOutboxEntity> outboxCaptor;

	@Captor
	private ArgumentCaptor<EmailRequest> emailRequestCaptor;

	private final NamespaceConfigEntity namespaceConfigEntity = NamespaceConfigEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID);

	private SubscriberEmailService service;

	@BeforeEach
	void setUp() {
		service = new SubscriberEmailService(outboxRepositoryMock, namespaceConfigRepositoryMock, namespaceConfigMapperMock, messagingClientMock, employeeServiceMock, SENDER_EMAIL, SENDER_NAME, MAX_AGE);
	}

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(outboxRepositoryMock, namespaceConfigRepositoryMock, namespaceConfigMapperMock, messagingClientMock, employeeServiceMock);
	}

	@Test
	void enqueue() {
		final var subscriber = createSubscriber(emailChannel("chef@example.com"));
		final var events = List.of(
			NotificationDispatchEntity.create().withEventId("event-1").withEventType("UPDATE").withDescription("Ärendet har uppdaterats."),
			NotificationDispatchEntity.create().withEventId("event-2").withEventType("UPDATE").withDescription("En anteckning har skapats."));

		service.enqueue(ERRAND_ID_1, "ABC123", subscriber, events);

		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		final var saved = outboxCaptor.getValue();
		assertThat(saved.getSubscriber()).isSameAs(subscriber);
		assertThat(saved.getErrandId()).isEqualTo(ERRAND_ID_1);
		assertThat(saved.getErrandNumber()).isEqualTo("ABC123");
		assertThat(saved.getEvents()).containsExactly(
			EmailDispatchOutboxEventEmbeddable.create().withEventId("event-1").withDescription("Ärendet har uppdaterats."),
			EmailDispatchOutboxEventEmbeddable.create().withEventId("event-2").withDescription("En anteckning har skapats."));
	}

	@Test
	void findPendingSubscriberIds() {
		when(outboxRepositoryMock.findDistinctSubscriberIds()).thenReturn(List.of(SUBSCRIBER_ID));

		assertThat(service.findPendingSubscriberIds()).containsExactly(SUBSCRIBER_ID);

		verify(outboxRepositoryMock).findDistinctSubscriberIds();
	}

	@Test
	void sendPendingWithNothingInOutbox() {
		when(outboxRepositoryMock.findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID)).thenReturn(List.of());

		service.sendPending(SUBSCRIBER_ID);

		verify(outboxRepositoryMock).findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID);
	}

	@Test
	void sendPendingGroupsEventsPerErrandInOneEmail() {
		final var subscriber = createSubscriber(emailChannel("chef@example.com"));
		final var entries = List.of(
			createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."),
			createEntry(subscriber, ERRAND_ID_2, "Ärendet har uppdaterats."),
			createEntry(subscriber, ERRAND_ID_1, "En anteckning har skapats."));
		mockSendPending(entries, namespaceConfig(false, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		final var request = emailRequestCaptor.getValue();
		assertThat(request.getRecipients()).containsExactly("chef@example.com");
		assertThat(request.getSubject()).isEqualTo("Uppdatering i " + DISPLAY_NAME);
		assertThat(request.getSender().getAddress()).isEqualTo(SENDER_EMAIL);
		assertThat(request.getSender().getName()).isEqualTo(SENDER_NAME);
		assertThat(request.getMessage()).isEqualTo("""
			Uppdatering i Avvikelser

			Ärende ABC123:
			    - Ärendet har uppdaterats.
			    - En anteckning har skapats.

			Ärende ABC456:
			    - Ärendet har uppdaterats.""");
		verifySendPending(entries);
	}

	@Test
	void sendPendingWithBaseUrlLinksToEveryErrand() {
		final var subscriber = createSubscriber(emailChannel("chef@example.com"));
		final var entries = List.of(
			createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."),
			createEntry(subscriber, ERRAND_ID_2, "Ärendet har uppdaterats."),
			createEntry(subscriber, ERRAND_ID_1, "En anteckning har skapats."));
		mockSendPending(entries, namespaceConfig(false, "https://draken.example.com/"));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		assertThat(emailRequestCaptor.getValue().getMessage()).isEqualTo("""
			Uppdatering i Avvikelser

			Ärende ABC123:
			    Länk: https://draken.example.com/2281/NAMESPACE-1/errands/ABC123
			    - Ärendet har uppdaterats.
			    - En anteckning har skapats.

			Ärende ABC456:
			    Länk: https://draken.example.com/2281/NAMESPACE-1/errands/ABC456
			    - Ärendet har uppdaterats.""");
		verifySendPending(entries);
	}

	@Test
	void sendPendingWithBaseUrlExcludingEventDescriptions() {
		final var subscriber = createSubscriber(emailChannel("chef@example.com"));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."));
		mockSendPending(entries, namespaceConfig(true, "https://draken.example.com"));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		assertThat(emailRequestCaptor.getValue().getMessage()).isEqualTo("""
			Uppdatering i Avvikelser

			Ärende ABC123:
			    Länk: https://draken.example.com/2281/NAMESPACE-1/errands/ABC123""");
		verifySendPending(entries);
	}

	@Test
	void sendPendingExcludingEventDescriptions() {
		final var subscriber = createSubscriber(emailChannel("chef@example.com"));
		final var entries = List.of(
			createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."),
			createEntry(subscriber, ERRAND_ID_2, "En anteckning har skapats."));
		mockSendPending(entries, namespaceConfig(true, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		assertThat(emailRequestCaptor.getValue().getMessage()).isEqualTo("""
			Uppdatering i Avvikelser

			Ärende ABC123:

			Ärende ABC456:""");
		verifySendPending(entries);
	}

	@Test
	void sendPendingWithoutNamespaceConfigOrErrandNumberFallsBackToIds() {
		final var subscriber = createSubscriber(emailChannel("chef@example.com"));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats.").withErrandNumber(null));
		when(outboxRepositoryMock.findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID)).thenReturn(entries);
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		assertThat(emailRequestCaptor.getValue().getSubject()).isEqualTo("Uppdatering i " + NAMESPACE);
		assertThat(emailRequestCaptor.getValue().getMessage()).isEqualTo("""
			Uppdatering i NAMESPACE-1

			Ärende errand-id-1:
			    - Ärendet har uppdaterats.""");
		verify(outboxRepositoryMock).findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID);
		verify(namespaceConfigRepositoryMock).findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(outboxRepositoryMock).deleteAll(entries);
	}

	@Test
	void sendPendingResolvesEmailFromEmployeeWhenChannelHasNoDestination() {
		final var subscriber = createSubscriber(emailChannel(null), NotificationChannelEmbeddable.create().withType(INTERNAL));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."));
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe")).thenReturn(new PortalPersonData().email("joe@example.com"));
		mockSendPending(entries, namespaceConfig(false, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(employeeServiceMock).getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe");
		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		assertThat(emailRequestCaptor.getValue().getRecipients()).containsExactly("joe@example.com");
		verifySendPending(entries);
	}

	@Test
	void sendPendingSendsOneEmailToEveryEmailChannel() {
		final var subscriber = createSubscriber(emailChannel("first@example.com"), emailChannel("second@example.com"), emailChannel("first@example.com"));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."));
		mockSendPending(entries, namespaceConfig(false, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		assertThat(emailRequestCaptor.getValue().getRecipients()).containsExactly("first@example.com", "second@example.com");
		verifySendPending(entries);
	}

	@Test
	void sendPendingAddsEmployeeEmailForEmailChannelWithoutDestination() {
		final var subscriber = createSubscriber(emailChannel("chef@example.com"), emailChannel(null));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."));
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe")).thenReturn(new PortalPersonData().email("joe@example.com"));
		mockSendPending(entries, namespaceConfig(false, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(employeeServiceMock).getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe");
		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		assertThat(emailRequestCaptor.getValue().getRecipients()).containsExactly("chef@example.com", "joe@example.com");
		verifySendPending(entries);
	}

	@Test
	void sendPendingDiscardsEntriesWhenSubscriberNoLongerHasEmailChannel() {
		final var subscriber = createSubscriber(NotificationChannelEmbeddable.create().withType(INTERNAL));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."));
		when(outboxRepositoryMock.findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID)).thenReturn(entries);

		service.sendPending(SUBSCRIBER_ID);

		// The employee directory is not consulted, so the subscriber's own address is not used either
		verify(outboxRepositoryMock).findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID);
		verify(outboxRepositoryMock).deleteAll(entries);
	}

	@Test
	void sendPendingDiscardsEntriesWhenEmailCannotBeResolved() {
		final var subscriber = createSubscriber(emailChannel(null));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."));
		when(outboxRepositoryMock.findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID)).thenReturn(entries);
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe")).thenReturn(null);

		service.sendPending(SUBSCRIBER_ID);

		verify(outboxRepositoryMock).findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID);
		verify(employeeServiceMock).getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe");
		verify(outboxRepositoryMock).deleteAll(entries);
		verify(messagingClientMock, never()).sendEmail(any(), eq(true), any());
	}

	@Test
	void sendPendingDiscardsEntriesWhenSubscriberIsNotAnAdAccount() {
		final var subscriber = createSubscriber(emailChannel(null))
			.withIdentifier(IdentifierEmbeddable.create().withType("partyId").withValue("some-party-id"));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."));
		when(outboxRepositoryMock.findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID)).thenReturn(entries);

		service.sendPending(SUBSCRIBER_ID);

		verify(outboxRepositoryMock).findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID);
		verify(outboxRepositoryMock).deleteAll(entries);
	}

	@Test
	void sendPendingLeavesOutEntriesOlderThanMaxAge() {
		final var subscriber = createSubscriber(emailChannel("chef@example.com"));
		final var stale = createEntry(subscriber, ERRAND_ID_2, "Gammal händelse.").withCreated(OffsetDateTime.now().minus(MAX_AGE).minusDays(1));
		final var current = createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats.").withCreated(OffsetDateTime.now().minusDays(1));
		final var entries = List.of(stale, current);
		mockSendPending(entries, namespaceConfig(false, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), emailRequestCaptor.capture());
		assertThat(emailRequestCaptor.getValue().getMessage()).isEqualTo("""
			Uppdatering i Avvikelser

			Ärende ABC123:
			    - Ärendet har uppdaterats.""");
		verifySendPending(entries);
	}

	@Test
	void sendPendingDiscardsEntriesWhenAllAreOlderThanMaxAge() {
		final var subscriber = createSubscriber(emailChannel("chef@example.com"));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats.").withCreated(OffsetDateTime.now().minus(MAX_AGE).minusDays(1)));
		when(outboxRepositoryMock.findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID)).thenReturn(entries);

		service.sendPending(SUBSCRIBER_ID);

		verify(outboxRepositoryMock).findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID);
		verify(outboxRepositoryMock).deleteAll(entries);
	}

	@Test
	void sendPendingLeavesEntriesInPlaceWhenSendFails() {
		final var subscriber = createSubscriber(emailChannel("chef@example.com"));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1, "Ärendet har uppdaterats."));
		mockSendPending(entries, namespaceConfig(false, null));
		doThrow(new RuntimeException("send failed")).when(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), any());

		assertThatThrownBy(() -> service.sendPending(SUBSCRIBER_ID))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("send failed");

		verify(outboxRepositoryMock).findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID);
		verify(namespaceConfigRepositoryMock).findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(namespaceConfigMapperMock).toNamespaceConfig(namespaceConfigEntity);
		verify(messagingClientMock).sendEmail(eq(MUNICIPALITY_ID), eq(true), any());
		verify(outboxRepositoryMock, never()).deleteAll(any());
	}

	private void mockSendPending(final List<EmailDispatchOutboxEntity> entries, final NamespaceConfig namespaceConfig) {
		when(outboxRepositoryMock.findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID)).thenReturn(entries);
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(namespaceConfigEntity));
		when(namespaceConfigMapperMock.toNamespaceConfig(namespaceConfigEntity)).thenReturn(namespaceConfig);
	}

	private void verifySendPending(final List<EmailDispatchOutboxEntity> entries) {
		verify(outboxRepositoryMock).findBySubscriberIdOrderByCreatedAsc(SUBSCRIBER_ID);
		verify(namespaceConfigRepositoryMock).findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(namespaceConfigMapperMock).toNamespaceConfig(namespaceConfigEntity);
		verify(outboxRepositoryMock).deleteAll(entries);
	}

	private static SubscriberEntity createSubscriber(final NotificationChannelEmbeddable... channels) {
		return SubscriberEntity.create()
			.withId(SUBSCRIBER_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"))
			.withChannels(List.of(channels));
	}

	private static NotificationChannelEmbeddable emailChannel(final String destination) {
		return NotificationChannelEmbeddable.create().withType(EMAIL).withDestination(destination);
	}

	private static EmailDispatchOutboxEntity createEntry(final SubscriberEntity subscriber, final String errandId, final String description) {
		return EmailDispatchOutboxEntity.create()
			.withSubscriber(subscriber)
			.withErrandId(errandId)
			.withErrandNumber(ERRAND_NUMBERS.get(errandId))
			.withEvents(List.of(EmailDispatchOutboxEventEmbeddable.create().withDescription(description)));
	}

	private static NamespaceConfig namespaceConfig(final boolean excludeEventDescriptions, final String baseUrl) {
		return NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withExcludeEventDescriptionsInEmail(excludeEventDescriptions)
			.withBaseUrl(baseUrl);
	}
}
