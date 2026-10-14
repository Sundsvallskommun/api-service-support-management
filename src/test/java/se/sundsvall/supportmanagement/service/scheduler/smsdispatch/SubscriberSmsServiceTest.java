package se.sundsvall.supportmanagement.service.scheduler.smsdispatch;

import generated.se.sundsvall.employee.PortalPersonData;
import generated.se.sundsvall.messaging.SmsRequest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchOutboxEventEmbeddable;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.INTERNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.SMS;

@ExtendWith(MockitoExtension.class)
class SubscriberSmsServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String DISPLAY_NAME = "Avvikelser";
	private static final String SUBSCRIBER_ID = "subscriber-id";
	private static final String ERRAND_ID_1 = "errand-id-1";
	private static final String ERRAND_ID_2 = "errand-id-2";
	private static final Map<String, String> ERRAND_NUMBERS = Map.of(ERRAND_ID_1, "ABC123", ERRAND_ID_2, "ABC456");
	private static final String SENDER = "Sundsvall";
	private static final String MOBILE_NUMBER = "+46701234567";
	private static final Duration MAX_AGE = Duration.ofDays(30);

	@Mock
	private NotificationDispatchOutboxRepository outboxRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Mock
	private NamespaceConfigMapper namespaceConfigMapperMock;

	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private EmployeeService employeeServiceMock;

	@Captor
	private ArgumentCaptor<NotificationDispatchOutboxEntity> outboxCaptor;

	@Captor
	private ArgumentCaptor<SmsRequest> smsRequestCaptor;

	private final NamespaceConfigEntity namespaceConfigEntity = NamespaceConfigEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID);

	private SubscriberSmsService service;

	@BeforeEach
	void setUp() {
		service = new SubscriberSmsService(outboxRepositoryMock, namespaceConfigRepositoryMock, namespaceConfigMapperMock, messagingClientMock, employeeServiceMock, SENDER, MAX_AGE);
	}

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(outboxRepositoryMock, namespaceConfigRepositoryMock, namespaceConfigMapperMock, messagingClientMock, employeeServiceMock);
	}

	@Test
	void enqueue() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER));
		final var events = List.of(
			NotificationDispatchEntity.create().withEventId("event-1").withEventType("UPDATE").withDescription("Ärendet har uppdaterats."),
			NotificationDispatchEntity.create().withEventId("event-2").withEventType("UPDATE").withDescription("En anteckning har skapats."));
		mockNamespaceConfig(namespaceConfig(true, null));

		service.enqueue(ERRAND_ID_1, "ABC123", subscriber, events);

		verifyNamespaceConfig();
		verify(outboxRepositoryMock).save(outboxCaptor.capture());
		final var saved = outboxCaptor.getValue();
		assertThat(saved.getSubscriber()).isSameAs(subscriber);
		assertThat(saved.getChannel()).isEqualTo(SMS);
		assertThat(saved.getErrandId()).isEqualTo(ERRAND_ID_1);
		assertThat(saved.getErrandNumber()).isEqualTo("ABC123");
		assertThat(saved.getEvents()).containsExactly(
			NotificationDispatchOutboxEventEmbeddable.create().withEventId("event-1").withDescription("Ärendet har uppdaterats."),
			NotificationDispatchOutboxEventEmbeddable.create().withEventId("event-2").withDescription("En anteckning har skapats."));
	}

	@Test
	void enqueueWhenSmsIsNotEnabledInNamespace() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER));
		mockNamespaceConfig(namespaceConfig(false, null));

		service.enqueue(ERRAND_ID_1, "ABC123", subscriber, List.of(NotificationDispatchEntity.create().withEventId("event-1")));

		verifyNamespaceConfig();
		verify(outboxRepositoryMock, never()).save(any());
	}

	@Test
	void enqueueWithoutNamespaceConfig() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER));
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		service.enqueue(ERRAND_ID_1, "ABC123", subscriber, List.of(NotificationDispatchEntity.create().withEventId("event-1")));

		verify(namespaceConfigRepositoryMock).findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(outboxRepositoryMock, never()).save(any());
	}

	@Test
	void findPendingSubscriberIds() {
		when(outboxRepositoryMock.findDistinctSubscriberIdsByChannel(SMS)).thenReturn(List.of(SUBSCRIBER_ID));

		assertThat(service.findPendingSubscriberIds()).containsExactly(SUBSCRIBER_ID);

		verify(outboxRepositoryMock).findDistinctSubscriberIdsByChannel(SMS);
	}

	@Test
	void sendPendingWithNothingInOutbox() {
		when(outboxRepositoryMock.findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS)).thenReturn(List.of());

		service.sendPending(SUBSCRIBER_ID);

		verify(outboxRepositoryMock).findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS);
	}

	@Test
	void sendPendingNamesEveryErrandOnceInOneSms() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER));
		final var entries = List.of(
			createEntry(subscriber, ERRAND_ID_1),
			createEntry(subscriber, ERRAND_ID_2),
			createEntry(subscriber, ERRAND_ID_1));
		mockSendPending(entries, namespaceConfig(true, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(true), smsRequestCaptor.capture());
		final var request = smsRequestCaptor.getValue();
		assertThat(request.getMobileNumber()).isEqualTo(MOBILE_NUMBER);
		assertThat(request.getSender()).isEqualTo(SENDER);
		assertThat(request.getMessage()).isEqualTo("""
			Uppdatering i Avvikelser:
			Ärende ABC123
			Ärende ABC456""");
		verifySendPending(entries);
	}

	@Test
	void sendPendingWithBaseUrlLinksToEveryErrand() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER));
		final var entries = List.of(
			createEntry(subscriber, ERRAND_ID_1),
			createEntry(subscriber, ERRAND_ID_2));
		mockSendPending(entries, namespaceConfig(true, "https://draken.example.com/AVV/arende/"));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(true), smsRequestCaptor.capture());
		assertThat(smsRequestCaptor.getValue().getMessage()).isEqualTo("""
			Uppdatering i Avvikelser:
			Ärende ABC123: https://draken.example.com/AVV/arende/ABC123
			Ärende ABC456: https://draken.example.com/AVV/arende/ABC456""");
		verifySendPending(entries);
	}

	@Test
	void sendPendingOnlyCountsErrandsPastTheFirstFive() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER));
		final var entries = IntStream.rangeClosed(1, 7)
			.mapToObj(index -> createEntry(subscriber, "errand-id-" + index).withErrandNumber("NR-" + index))
			.toList();
		mockSendPending(entries, namespaceConfig(true, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(true), smsRequestCaptor.capture());
		assertThat(smsRequestCaptor.getValue().getMessage()).isEqualTo("""
			Uppdatering i Avvikelser:
			Ärende NR-1
			Ärende NR-2
			Ärende NR-3
			Ärende NR-4
			Ärende NR-5
			...och 2 ärenden till""");
		verifySendPending(entries);
	}

	@Test
	void sendPendingWithoutNamespaceConfigOrErrandNumberFallsBackToIds() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1).withErrandNumber(null));
		when(outboxRepositoryMock.findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS)).thenReturn(entries);
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(true), smsRequestCaptor.capture());
		assertThat(smsRequestCaptor.getValue().getMessage()).isEqualTo("""
			Uppdatering i NAMESPACE-1:
			Ärende errand-id-1""");
		verify(outboxRepositoryMock).findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS);
		verify(namespaceConfigRepositoryMock).findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(outboxRepositoryMock).deleteAll(entries);
	}

	@ParameterizedTest
	@CsvSource({
		"070-123 45 67, +46701234567",
		"0701234567, +46701234567",
		"0046701234567, +46701234567",
		"+46 (0)70 123 45 67, +46701234567",
		"+46701234567, +46701234567"
	})
	void sendPendingResolvesMobileNumberFromEmployeeWhenChannelHasNoDestination(final String mobilePhone, final String expectedMsisdn) {
		final var subscriber = createSubscriber(smsChannel(null), NotificationChannelEmbeddable.create().withType(INTERNAL));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1));
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe")).thenReturn(new PortalPersonData().mobilePhone(mobilePhone));
		mockSendPending(entries, namespaceConfig(true, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(employeeServiceMock).getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe");
		verify(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(true), smsRequestCaptor.capture());
		assertThat(smsRequestCaptor.getValue().getMobileNumber()).isEqualTo(expectedMsisdn);
		verifySendPending(entries);
	}

	@Test
	void sendPendingResolvesMobileNumberFromEmployeeWhenSubscriberHasNoSmsChannel() {
		// Sms routed by a subscription profile to a subscriber that only has an internal channel
		final var subscriber = createSubscriber(NotificationChannelEmbeddable.create().withType(INTERNAL));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1));
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe")).thenReturn(new PortalPersonData().mobilePhone("070-123 45 67"));
		mockSendPending(entries, namespaceConfig(true, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(employeeServiceMock).getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe");
		verify(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(true), smsRequestCaptor.capture());
		assertThat(smsRequestCaptor.getValue().getMobileNumber()).isEqualTo(MOBILE_NUMBER);
		verifySendPending(entries);
	}

	@Test
	void sendPendingSendsOneSmsToEverySmsChannel() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER), smsChannel("+46709876543"), smsChannel("0701234567"));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1));
		mockSendPending(entries, namespaceConfig(true, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock, times(2)).sendSms(eq(MUNICIPALITY_ID), eq(true), smsRequestCaptor.capture());
		assertThat(smsRequestCaptor.getAllValues())
			.extracting(SmsRequest::getMobileNumber)
			.containsExactly(MOBILE_NUMBER, "+46709876543");
		verifySendPending(entries);
	}

	@Test
	void sendPendingLeavesOutInvalidMobileNumbers() {
		final var subscriber = createSubscriber(smsChannel("not-a-number"), smsChannel(MOBILE_NUMBER));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1));
		mockSendPending(entries, namespaceConfig(true, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(true), smsRequestCaptor.capture());
		assertThat(smsRequestCaptor.getValue().getMobileNumber()).isEqualTo(MOBILE_NUMBER);
		verifySendPending(entries);
	}

	@Test
	void sendPendingDiscardsEntriesWhenMobileNumberCannotBeResolved() {
		final var subscriber = createSubscriber(smsChannel(null));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1));
		when(outboxRepositoryMock.findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS)).thenReturn(entries);
		when(employeeServiceMock.getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe")).thenReturn(new PortalPersonData());

		service.sendPending(SUBSCRIBER_ID);

		verify(outboxRepositoryMock).findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS);
		verify(employeeServiceMock).getEmployeeByLoginName(MUNICIPALITY_ID, "joe01doe");
		verify(outboxRepositoryMock).deleteAll(entries);
		verify(messagingClientMock, never()).sendSms(any(), eq(true), any());
	}

	@Test
	void sendPendingDiscardsEntriesWhenSubscriberIsNotAnAdAccount() {
		final var subscriber = createSubscriber(smsChannel(null))
			.withIdentifier(IdentifierEmbeddable.create().withType("partyId").withValue("some-party-id"));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1));
		when(outboxRepositoryMock.findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS)).thenReturn(entries);

		service.sendPending(SUBSCRIBER_ID);

		verify(outboxRepositoryMock).findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS);
		verify(outboxRepositoryMock).deleteAll(entries);
	}

	@Test
	void sendPendingLeavesOutEntriesOlderThanMaxAge() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER));
		final var stale = createEntry(subscriber, ERRAND_ID_2).withCreated(OffsetDateTime.now().minus(MAX_AGE).minusDays(1));
		final var current = createEntry(subscriber, ERRAND_ID_1).withCreated(OffsetDateTime.now().minusDays(1));
		final var entries = List.of(stale, current);
		mockSendPending(entries, namespaceConfig(true, null));

		service.sendPending(SUBSCRIBER_ID);

		verify(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(true), smsRequestCaptor.capture());
		assertThat(smsRequestCaptor.getValue().getMessage()).isEqualTo("""
			Uppdatering i Avvikelser:
			Ärende ABC123""");
		verifySendPending(entries);
	}

	@Test
	void sendPendingDiscardsEntriesWhenAllAreOlderThanMaxAge() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1).withCreated(OffsetDateTime.now().minus(MAX_AGE).minusDays(1)));
		when(outboxRepositoryMock.findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS)).thenReturn(entries);

		service.sendPending(SUBSCRIBER_ID);

		verify(outboxRepositoryMock).findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS);
		verify(outboxRepositoryMock).deleteAll(entries);
	}

	@Test
	void sendPendingLeavesEntriesInPlaceWhenSendFails() {
		final var subscriber = createSubscriber(smsChannel(MOBILE_NUMBER));
		final var entries = List.of(createEntry(subscriber, ERRAND_ID_1));
		mockSendPending(entries, namespaceConfig(true, null));
		doThrow(new RuntimeException("send failed")).when(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(true), any());

		assertThatThrownBy(() -> service.sendPending(SUBSCRIBER_ID))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("send failed");

		verify(outboxRepositoryMock).findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS);
		verifyNamespaceConfig();
		verify(messagingClientMock).sendSms(eq(MUNICIPALITY_ID), eq(true), any());
		verify(outboxRepositoryMock, never()).deleteAll(any());
	}

	private void mockNamespaceConfig(final NamespaceConfig namespaceConfig) {
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(namespaceConfigEntity));
		when(namespaceConfigMapperMock.toNamespaceConfig(namespaceConfigEntity)).thenReturn(namespaceConfig);
	}

	private void verifyNamespaceConfig() {
		verify(namespaceConfigRepositoryMock).findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(namespaceConfigMapperMock).toNamespaceConfig(namespaceConfigEntity);
	}

	private void mockSendPending(final List<NotificationDispatchOutboxEntity> entries, final NamespaceConfig namespaceConfig) {
		when(outboxRepositoryMock.findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS)).thenReturn(entries);
		mockNamespaceConfig(namespaceConfig);
	}

	private void verifySendPending(final List<NotificationDispatchOutboxEntity> entries) {
		verify(outboxRepositoryMock).findBySubscriberIdAndChannelOrderByCreatedAsc(SUBSCRIBER_ID, SMS);
		verifyNamespaceConfig();
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

	private static NotificationChannelEmbeddable smsChannel(final String destination) {
		return NotificationChannelEmbeddable.create().withType(SMS).withDestination(destination);
	}

	private static NotificationDispatchOutboxEntity createEntry(final SubscriberEntity subscriber, final String errandId) {
		return NotificationDispatchOutboxEntity.create()
			.withSubscriber(subscriber)
			.withChannel(SMS)
			.withErrandId(errandId)
			.withErrandNumber(ERRAND_NUMBERS.get(errandId))
			.withEvents(List.of(NotificationDispatchOutboxEventEmbeddable.create().withDescription("Ärendet har uppdaterats.")));
	}

	private static NamespaceConfig namespaceConfig(final boolean smsNotificationsEnabled, final String baseUrl) {
		return NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withSmsNotificationsEnabled(smsNotificationsEnabled)
			.withBaseUrl(baseUrl);
	}
}
