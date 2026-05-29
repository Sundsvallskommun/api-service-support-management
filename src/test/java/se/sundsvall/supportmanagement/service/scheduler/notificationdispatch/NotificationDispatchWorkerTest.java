package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriberRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchWorkerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String ERRAND_ID = "errand-id";
	private static final String ERRAND_NUMBER = "PRH-2022-000001";
	private static final String EVENT_TYPE = "CREATE";

	@Mock
	private NotificationDispatchRepository dispatchRepositoryMock;

	@Mock
	private SubscriberRepository subscriberRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private NotificationChannelDispatcher channelDispatcherMock;

	@InjectMocks
	private NotificationDispatchWorker worker;

	@Captor
	private ArgumentCaptor<NotificationDispatchEntity> entityCaptor;

	@Captor
	private ArgumentCaptor<OffsetDateTime> offsetDateTimeCaptor;

	private NotificationDispatchEntity buildEntry(final String executingUserId) {
		return NotificationDispatchEntity.create()
			.withId("dispatch-id")
			.withErrandId(ERRAND_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withEventType(EVENT_TYPE)
			.withExecutingUserId(executingUserId);
	}

	private SubscriberEntity buildSubscriber(final String identifierValue, final String... eventFilterTypes) {
		final var filters = List.of(eventFilterTypes).stream()
			.map(t -> EventFilterEmbeddable.create().withType(t))
			.toList();
		return SubscriberEntity.create()
			.withId("subscriber-id")
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue(identifierValue))
			.withChannels(List.of(NotificationChannelEmbeddable.create()))
			.withEventFilters(filters);
	}

	@Test
	void processGroup_successfulSend_deletesEntries() {
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", EVENT_TYPE);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(ErrandEntity.create().withErrandNumber(ERRAND_NUMBER)));
		when(subscriberRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(subscriber));

		worker.processGroup(List.of(entry));

		verify(channelDispatcherMock).send(any(), any(), any(), any());
		verify(dispatchRepositoryMock).deleteAll(List.of(entry));
		verify(dispatchRepositoryMock, never()).save(any());
	}

	@Test
	void processGroup_executingUserFiltered_entriesDeleted() {
		final var entry = buildEntry("joe01doe");
		final var subscriber = buildSubscriber("joe01doe", EVENT_TYPE);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(ErrandEntity.create().withErrandNumber(ERRAND_NUMBER)));
		when(subscriberRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(subscriber));

		worker.processGroup(List.of(entry));

		verify(channelDispatcherMock, never()).send(any(), any(), any(), any());
		verify(dispatchRepositoryMock).deleteAll(List.of(entry));
	}

	@Test
	void processGroup_subscriberEventFilterMismatch_entriesDeleted() {
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", "OTHER_TYPE");
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(ErrandEntity.create().withErrandNumber(ERRAND_NUMBER)));
		when(subscriberRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(subscriber));

		worker.processGroup(List.of(entry));

		verify(channelDispatcherMock, never()).send(any(), any(), any(), any());
		verify(dispatchRepositoryMock).deleteAll(List.of(entry));
	}

	@Test
	void processGroup_channelFails_incrementsRetryCount() {
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", EVENT_TYPE);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(ErrandEntity.create().withErrandNumber(ERRAND_NUMBER)));
		when(subscriberRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(subscriber));
		doThrow(new RuntimeException("send failed")).when(channelDispatcherMock).send(any(), any(), any(), any());

		worker.processGroup(List.of(entry));

		verify(dispatchRepositoryMock, never()).deleteAll(any());
		verify(dispatchRepositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getRetryCount()).isEqualTo(1);
		assertThat(entityCaptor.getValue().getNextRetryAt()).isNotNull();
		assertThat(entityCaptor.getValue().isDeadLetter()).isFalse();
	}

	@Test
	void processGroup_channelFailsMaxRetries_marksAsDeadLetter() {
		final var entry = buildEntry("other-user").withRetryCount(2);
		final var subscriber = buildSubscriber("joe01doe", EVENT_TYPE);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(ErrandEntity.create().withErrandNumber(ERRAND_NUMBER)));
		when(subscriberRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(subscriber));
		doThrow(new RuntimeException("send failed")).when(channelDispatcherMock).send(any(), any(), any(), any());

		worker.processGroup(List.of(entry));

		verify(dispatchRepositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().isDeadLetter()).isTrue();
		assertThat(entityCaptor.getValue().getRetryCount()).isEqualTo(3);
	}

	@Test
	void processGroup_noSubscribers_deletesEntries() {
		final var entry = buildEntry("other-user");
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(ErrandEntity.create().withErrandNumber(ERRAND_NUMBER)));
		when(subscriberRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of());

		worker.processGroup(List.of(entry));

		verify(channelDispatcherMock, never()).send(any(), any(), any(), any());
		verify(dispatchRepositoryMock).deleteAll(List.of(entry));
	}

	@Test
	void processGroup_groupWithMultipleEntries_differentExecutingUsers_sendsOnce() {
		final var entryBySelf = buildEntry("joe01doe").withId("entry-1");
		final var entryByOther = buildEntry("other-user").withId("entry-2");
		final var subscriber = buildSubscriber("joe01doe", EVENT_TYPE);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(ErrandEntity.create().withErrandNumber(ERRAND_NUMBER)));
		when(subscriberRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(subscriber));

		worker.processGroup(List.of(entryBySelf, entryByOther));

		verify(channelDispatcherMock).send(any(), any(), any(), any());
		verify(dispatchRepositoryMock).deleteAll(List.of(entryBySelf, entryByOther));
	}

	@Test
	void cleanUpDeadLetters_deletesDeadLettersOlderThanRetentionPeriod() {
		final var before = now().minusDays(7);

		worker.cleanUpDeadLetters();

		final var after = now().minusDays(7);
		verify(dispatchRepositoryMock).deleteByDeadLetterTrueAndCreatedBefore(offsetDateTimeCaptor.capture());
		assertThat(offsetDateTimeCaptor.getValue()).isBetween(before, after);
	}
}
