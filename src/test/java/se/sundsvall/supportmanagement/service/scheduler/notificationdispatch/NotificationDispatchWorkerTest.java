package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.service.AccessControlService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
	private static final String DESCRIPTION = "Bilaga har skapats";
	private static final String SUB_TYPE = "ATTACHMENT";

	@Mock
	private NotificationDispatchRepository dispatchRepositoryMock;

	@Mock
	private SubscriptionRepository subscriptionRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private NotificationChannelDispatcher channelDispatcherMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@InjectMocks
	private NotificationDispatchWorker worker;

	@Captor
	private ArgumentCaptor<List<NotificationDispatchEntity>> eventsCaptor;

	@Captor
	private ArgumentCaptor<OffsetDateTime> offsetDateTimeCaptor;

	private static NotificationDispatchEntity buildEntry(final String executingUserId) {
		return NotificationDispatchEntity.create()
			.withId("dispatch-id")
			.withErrandId(ERRAND_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withEventType(EVENT_TYPE)
			.withDescription(DESCRIPTION)
			.withSubType(SUB_TYPE)
			.withExecutingUserId(executingUserId);
	}

	private static SubscriberEntity buildSubscriber(final String identifierValue, final List<EventFilterEmbeddable> eventFilters) {
		return SubscriberEntity.create()
			.withId("subscriber-id")
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue(identifierValue))
			.withChannels(List.of(NotificationChannelEmbeddable.create()))
			.withEventFilters(eventFilters);
	}

	private static SubscriptionEntity buildSubscription(final SubscriberEntity subscriber, final List<EventFilterEmbeddable> eventFilters) {
		return SubscriptionEntity.create()
			.withId("subscription-id")
			.withSubscriber(subscriber)
			.withTargetType(DbSubscriptionTargetType.ERRAND)
			.withEventFilters(eventFilters);
	}

	private static EventFilterEmbeddable filter(final String type, final String subtype) {
		return EventFilterEmbeddable.create().withType(type).withSubtype(subtype);
	}

	private void mockDispatchOf(final SubscriptionEntity... subscriptions) {
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(ErrandEntity.create().withErrandNumber(ERRAND_NUMBER)));
		when(subscriptionRepositoryMock.findAllActiveForErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any())).thenReturn(List.of(subscriptions));
		// Only reached once there is a subscriber to evaluate, so stubbing it without one would be unused
		if (subscriptions.length > 0) {
			mockErrandReachable(true);
		}
	}

	private void mockErrandReachable(final boolean reachable) {
		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any(), any())).thenReturn((root, query, cb) -> cb.conjunction());
		when(errandsRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any()))
			.thenReturn(reachable ? Optional.of(ErrandEntity.create()) : Optional.empty());
	}

	@Test
	void processGroupSendsEventsAndDeletesEntries() {

		// Arrange
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", List.of(filter(EVENT_TYPE, null)));
		mockDispatchOf(buildSubscription(subscriber, null));

		// Act
		worker.processGroup(List.of(entry));

		// Assert
		verify(channelDispatcherMock).send(ERRAND_ID, ERRAND_NUMBER, subscriber, List.of(entry));
		verify(dispatchRepositoryMock).deleteAll(List.of(entry));
	}

	@Test
	void processGroupSendsEveryEventTheSubscriberWantsInOneCall() {

		// Arrange
		final var created = buildEntry("other-user").withId("entry-1");
		final var updated = buildEntry("other-user").withId("entry-2").withEventType("UPDATE");
		final var subscriber = buildSubscriber("joe01doe", null);
		mockDispatchOf(buildSubscription(subscriber, null));

		// Act
		worker.processGroup(List.of(created, updated));

		// Assert — one delivery carrying both events, not one delivery per event
		verify(channelDispatcherMock).send(eq(ERRAND_ID), eq(ERRAND_NUMBER), eq(subscriber), eventsCaptor.capture());
		assertThat(eventsCaptor.getValue()).containsExactly(created, updated);
	}

	@Test
	void processGroupSkipsEventsCausedByTheSubscriberThemselves() {

		// Arrange
		final var bySelf = buildEntry("joe01doe").withId("entry-1");
		final var byOther = buildEntry("other-user").withId("entry-2");
		final var subscriber = buildSubscriber("joe01doe", null);
		mockDispatchOf(buildSubscription(subscriber, null));

		// Act
		worker.processGroup(List.of(bySelf, byOther));

		// Assert
		verify(channelDispatcherMock).send(ERRAND_ID, ERRAND_NUMBER, subscriber, List.of(byOther));
		verify(dispatchRepositoryMock).deleteAll(List.of(bySelf, byOther));
	}

	@Test
	void processGroupWithoutAnyWantedEventSendsNothing() {

		// Arrange
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", List.of(filter("OTHER_TYPE", null)));
		mockDispatchOf(buildSubscription(subscriber, null));

		// Act
		worker.processGroup(List.of(entry));

		// Assert
		verify(channelDispatcherMock, never()).send(any(), any(), any(), any());
		verify(dispatchRepositoryMock).deleteAll(List.of(entry));
	}

	@Test
	void processGroupWithoutSubscriptionsDeletesEntries() {

		// Arrange
		final var entry = buildEntry("other-user");
		mockDispatchOf();

		// Act
		worker.processGroup(List.of(entry));

		// Assert
		verify(channelDispatcherMock, never()).send(any(), any(), any(), any());
		verify(dispatchRepositoryMock).deleteAll(List.of(entry));
	}

	@Test
	void processGroupSkipsSubscriberThatCanNoLongerReachTheErrand() {

		// Arrange
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", null);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(ErrandEntity.create().withErrandNumber(ERRAND_NUMBER)));
		when(subscriptionRepositoryMock.findAllActiveForErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any()))
			.thenReturn(List.of(buildSubscription(subscriber, null)));
		mockErrandReachable(false);
		final var identifierCaptor = ArgumentCaptor.forClass(Identifier.class);

		// Act
		worker.processGroup(List.of(entry));

		// Assert - access is evaluated as the subscriber, since the scheduler runs without an identifier of its own
		verify(accessControlServiceMock).withAccessControl(eq(NAMESPACE), eq(MUNICIPALITY_ID), identifierCaptor.capture(), eq(ProtectedResource.NOTIFICATION), eq(LR));
		assertThat(identifierCaptor.getValue()).extracting(Identifier::getType, Identifier::getValue).containsExactly(Identifier.Type.AD_ACCOUNT, "joe01doe");
		verify(channelDispatcherMock, never()).send(any(), any(), any(), any());
		// The group is still cleared - an unreachable errand is not a failure to retry
		verify(dispatchRepositoryMock).deleteAll(List.of(entry));
	}

	@Test
	void processGroupWithoutFiltersSendsEverything() {

		// Arrange
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", List.of());
		mockDispatchOf(buildSubscription(subscriber, List.of()));

		// Act
		worker.processGroup(List.of(entry));

		// Assert
		verify(channelDispatcherMock).send(ERRAND_ID, ERRAND_NUMBER, subscriber, List.of(entry));
	}

	@Test
	void processGroupLetsSubscriptionFiltersOverrideSubscriberFilters() {

		// Arrange — the subscriber-level filter would reject the event, the subscription-level one accepts it
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", List.of(filter("OTHER_TYPE", null)));
		mockDispatchOf(buildSubscription(subscriber, List.of(filter(EVENT_TYPE, SUB_TYPE))));

		// Act
		worker.processGroup(List.of(entry));

		// Assert
		verify(channelDispatcherMock).send(ERRAND_ID, ERRAND_NUMBER, subscriber, List.of(entry));
	}

	@Test
	void processGroupMatchesSubtypeWhenFilterDeclaresOne() {

		// Arrange — same event type, but the filter is narrowed to another subtype
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", List.of(filter(EVENT_TYPE, "OTHER_SUB_TYPE")));
		mockDispatchOf(buildSubscription(subscriber, null));

		// Act
		worker.processGroup(List.of(entry));

		// Assert
		verify(channelDispatcherMock, never()).send(any(), any(), any(), any());
	}

	@Test
	void processGroupSendsOnceToSubscriberCoveredByMultipleSubscriptions() {

		// Arrange — the same subscriber reaches this errand through both a namespace and an errand subscription
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", null);
		final var namespaceSubscription = buildSubscription(subscriber, null)
			.withId("subscription-namespace")
			.withTargetType(DbSubscriptionTargetType.NAMESPACE);
		mockDispatchOf(namespaceSubscription, buildSubscription(subscriber, null));

		// Act
		worker.processGroup(List.of(entry));

		// Assert
		verify(channelDispatcherMock).send(ERRAND_ID, ERRAND_NUMBER, subscriber, List.of(entry));
	}

	@Test
	void processGroupUnionsFiltersAcrossSubscriptions() {

		// Arrange — each subscription accepts one of the two events
		final var created = buildEntry("other-user").withId("entry-1");
		final var updated = buildEntry("other-user").withId("entry-2").withEventType("UPDATE");
		final var subscriber = buildSubscriber("joe01doe", null);
		final var namespaceSubscription = buildSubscription(subscriber, List.of(filter("UPDATE", null)))
			.withId("subscription-namespace")
			.withTargetType(DbSubscriptionTargetType.NAMESPACE);
		mockDispatchOf(namespaceSubscription, buildSubscription(subscriber, List.of(filter(EVENT_TYPE, null))));

		// Act
		worker.processGroup(List.of(created, updated));

		// Assert
		verify(channelDispatcherMock).send(eq(ERRAND_ID), eq(ERRAND_NUMBER), eq(subscriber), eventsCaptor.capture());
		assertThat(eventsCaptor.getValue()).containsExactly(created, updated);
	}

	@Test
	void processGroupSkipsEntriesOlderThanMaxAge() {

		// Arrange
		ReflectionTestUtils.setField(worker, "maxAge", Duration.ofDays(30));
		final var stale = buildEntry("other-user").withId("entry-1").withCreated(now().minusDays(31));
		final var fresh = buildEntry("other-user").withId("entry-2").withCreated(now().minusDays(29));
		final var subscriber = buildSubscriber("joe01doe", null);
		mockDispatchOf(buildSubscription(subscriber, null));

		// Act
		worker.processGroup(List.of(stale, fresh));

		// Assert — the stale entry is never sent, but is still cleaned up with the rest of the group
		verify(channelDispatcherMock).send(ERRAND_ID, ERRAND_NUMBER, subscriber, List.of(fresh));
		verify(dispatchRepositoryMock).deleteAll(List.of(stale, fresh));
	}

	@Test
	void processGroupWithOnlyStaleEntriesSendsNothingAndDeletesThem() {

		// Arrange
		ReflectionTestUtils.setField(worker, "maxAge", Duration.ofDays(30));
		final var stale = buildEntry("other-user").withCreated(now().minusDays(31));
		final var subscriber = buildSubscriber("joe01doe", null);
		mockDispatchOf(buildSubscription(subscriber, null));

		// Act
		worker.processGroup(List.of(stale));

		// Assert
		verify(channelDispatcherMock, never()).send(any(), any(), any(), any());
		verify(dispatchRepositoryMock).deleteAll(List.of(stale));
	}

	@Test
	void processGroupWithUnknownErrandSendsWithoutErrandNumber() {

		// Arrange
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", null);
		when(errandsRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());
		when(subscriptionRepositoryMock.findAllActiveForErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any()))
			.thenReturn(List.of(buildSubscription(subscriber, null)));
		mockErrandReachable(true);

		// Act
		worker.processGroup(List.of(entry));

		// Assert
		verify(channelDispatcherMock).send(ERRAND_ID, null, subscriber, List.of(entry));
	}

	@Test
	void processGroupPropagatesFailureAndKeepsEntries() {

		// Arrange
		final var entry = buildEntry("other-user");
		final var subscriber = buildSubscriber("joe01doe", null);
		mockDispatchOf(buildSubscription(subscriber, null));
		doThrow(new RuntimeException("boom")).when(channelDispatcherMock).send(any(), any(), any(), any());

		// Act + Assert — the failure must reach the caller so the transaction rolls back and the group is retried later
		assertThatThrownBy(() -> worker.processGroup(List.of(entry)))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("boom");
		verify(dispatchRepositoryMock, never()).deleteAll(any());
	}

	@Test
	void fetchProcessableAppliesTransactionBuffer() {

		// Arrange
		ReflectionTestUtils.setField(worker, "transactionBuffer", Duration.ofSeconds(30));
		final var before = now().minusSeconds(30);

		// Act
		worker.fetchProcessable();

		// Assert
		final var after = now().minusSeconds(30);
		verify(dispatchRepositoryMock).findProcessable(offsetDateTimeCaptor.capture());
		assertThat(offsetDateTimeCaptor.getValue()).isBetween(before, after);
	}
}
