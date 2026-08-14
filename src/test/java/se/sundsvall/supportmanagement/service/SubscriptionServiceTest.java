package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.subscription.Subscription;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTarget;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String SUBSCRIBER_ID = "subscriber-1";
	private static final String ERRAND_ID = "errand-1";
	private static final DbSubscriptionTargetType DB_ERRAND = DbSubscriptionTargetType.ERRAND;
	private static final DbSubscriptionTargetType DB_NAMESPACE = DbSubscriptionTargetType.NAMESPACE;

	@Mock
	private SubscriberService subscriberServiceMock;

	@Mock
	private SubscriptionRepository subscriptionRepositoryMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@InjectMocks
	private SubscriptionService service;

	@Captor
	private ArgumentCaptor<SubscriptionEntity> entityCaptor;

	@BeforeEach
	void setExecutingUser() {
		se.sundsvall.dept44.support.Identifier.set(
			se.sundsvall.dept44.support.Identifier.create().withType(AD_ACCOUNT).withValue("joe01doe"));
	}

	@AfterEach
	void clearExecutingUser() {
		se.sundsvall.dept44.support.Identifier.remove();
	}

	@Test
	void findSubscriptions() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		final var sub = SubscriptionEntity.create().withId("sub-1").withSubscriber(subscriber).withTargetType(DB_NAMESPACE);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.findAllBySubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(List.of(sub));

		final var result = service.findSubscriptions(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getTarget().getType()).isEqualTo(SubscriptionTargetType.NAMESPACE);
		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(subscriptionRepositoryMock).findAllBySubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock);
		verifyNoInteractions(accessControlServiceMock);
	}

	@Test
	void findSubscriptionsSubscriberNotFound() {
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID))
			.thenThrow(Problem.valueOf(NOT_FOUND, "subscriber missing"));

		assertThatThrownBy(() -> service.findSubscriptions(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verifyNoMoreInteractions(subscriberServiceMock);
		verifyNoInteractions(subscriptionRepositoryMock, accessControlServiceMock);
	}

	@Test
	void createErrandSubscriptionHappyPath() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		final var errand = new ErrandEntity().withId(ERRAND_ID);
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID));

		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR)).thenReturn(errand);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(false);
		final var newId = randomUUID().toString();
		when(subscriptionRepositoryMock.saveAndFlush(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.<SubscriptionEntity>getArgument(0).withId(newId));

		final var result = service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto);

		assertThat(result).isEqualTo(newId);
		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID);
		verify(subscriptionRepositoryMock).saveAndFlush(entityCaptor.capture());
		final var saved = entityCaptor.getValue();
		assertThat(saved.getSubscriber()).isSameAs(subscriber);
		assertThat(saved.getErrand()).isSameAs(errand);
		assertThat(saved.getTargetType()).isEqualTo(DB_ERRAND);
		assertThat(saved.getCreatedBy().getType()).isEqualTo("adAccount");
		assertThat(saved.getCreatedBy().getValue()).isEqualTo("joe01doe");
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock, accessControlServiceMock);
	}

	@Test
	void createNamespaceSubscriptionHappyPath() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE));

		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIsNull(SUBSCRIBER_ID, DB_NAMESPACE)).thenReturn(false);
		when(subscriptionRepositoryMock.saveAndFlush(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.<SubscriptionEntity>getArgument(0).withId("new"));

		service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIsNull(SUBSCRIBER_ID, DB_NAMESPACE);
		verify(subscriptionRepositoryMock).saveAndFlush(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrand()).isNull();
		assertThat(entityCaptor.getValue().getTargetType()).isEqualTo(DB_NAMESPACE);
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock);
		verifyNoInteractions(accessControlServiceMock);
	}

	@Test
	void createSubscriptionSubscriberNotFound() {
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID))
			.thenThrow(Problem.valueOf(NOT_FOUND, "subscriber missing"));

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verifyNoMoreInteractions(subscriberServiceMock);
		verifyNoInteractions(subscriptionRepositoryMock, accessControlServiceMock);
	}

	@Test
	void createErrandSubscriptionErrandNotFound() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR))
			.thenThrow(Problem.valueOf(NOT_FOUND, "Errand not found"));

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR);
		verify(subscriptionRepositoryMock, never()).saveAndFlush(any());
		verifyNoMoreInteractions(subscriberServiceMock, accessControlServiceMock);
		verifyNoInteractions(subscriptionRepositoryMock);
	}

	@Test
	void createErrandSubscriptionConflict() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		final var errand = new ErrandEntity().withId(ERRAND_ID);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR)).thenReturn(errand);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(true);

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(CONFLICT);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID);
		verify(subscriptionRepositoryMock, never()).saveAndFlush(any());
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock, accessControlServiceMock);
	}

	@Test
	void createNamespaceSubscriptionConflict() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIsNull(SUBSCRIBER_ID, DB_NAMESPACE)).thenReturn(true);

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(CONFLICT);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIsNull(SUBSCRIBER_ID, DB_NAMESPACE);
		verify(subscriptionRepositoryMock, never()).saveAndFlush(any());
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock);
		verifyNoInteractions(accessControlServiceMock);
	}

	@Test
	void createSubscriptionErrandTypeMissingId() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verifyNoMoreInteractions(subscriberServiceMock);
		verifyNoInteractions(subscriptionRepositoryMock, accessControlServiceMock);
	}

	@Test
	void createSubscriptionNamespaceTypeWithErrandId() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE).withId(ERRAND_ID));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verifyNoMoreInteractions(subscriberServiceMock);
		verifyNoInteractions(subscriptionRepositoryMock, accessControlServiceMock);
	}

	@Test
	void createErrandSubscriptionRaceTranslatesDbViolationToConflict() {
		// Precheck reports no duplicate (false), but saveAndFlush throws DataIntegrityViolationException
		// — simulates the TOCTOU race past rejectDuplicate.
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		final var errand = new ErrandEntity().withId(ERRAND_ID);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR)).thenReturn(errand);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(false);
		when(subscriptionRepositoryMock.saveAndFlush(any(SubscriptionEntity.class)))
			.thenThrow(new org.springframework.dao.DataIntegrityViolationException("uq_subscription_subscriber_target_errand"));

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(CONFLICT);
	}

	@Test
	void createNamespaceSubscriptionRaceTranslatesDbViolationToConflict() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIsNull(SUBSCRIBER_ID, DB_NAMESPACE)).thenReturn(false);
		when(subscriptionRepositoryMock.saveAndFlush(any(SubscriptionEntity.class)))
			.thenThrow(new org.springframework.dao.DataIntegrityViolationException("uq_subscription_subscriber_target_errand"));

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(CONFLICT);
	}

	@Test
	void deleteSubscription() {
		final var entity = SubscriptionEntity.create().withId("sub-1");
		when(subscriptionRepositoryMock.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));

		service.deleteSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, "sub-1");

		verify(subscriptionRepositoryMock).findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionRepositoryMock).delete(entity);
		verifyNoMoreInteractions(subscriptionRepositoryMock);
		verifyNoInteractions(subscriberServiceMock, accessControlServiceMock);
	}

	@Test
	void deleteSubscriptionNotFound() {
		when(subscriptionRepositoryMock.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, "sub-1"))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriptionRepositoryMock).findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionRepositoryMock, never()).delete(any(SubscriptionEntity.class));
		verifyNoMoreInteractions(subscriptionRepositoryMock);
		verifyNoInteractions(subscriberServiceMock, accessControlServiceMock);
	}

	@Test
	void autoSubscribeErrandAssigneeWhenNoAssignedUser() {
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);

		service.autoSubscribeErrandAssignee(errand);

		verifyNoInteractions(subscriberServiceMock, subscriptionRepositoryMock, accessControlServiceMock);
	}

	@Test
	void autoSubscribeErrandAssigneeWhenSubscriptionAlreadyExists() {
		final var assignedUserId = "joe01doe";
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withAssignedUserId(assignedUserId);
		when(subscriberServiceMock.findOrCreateSubscriberForAssignee(MUNICIPALITY_ID, NAMESPACE, assignedUserId)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(true);

		service.autoSubscribeErrandAssignee(errand);

		verify(subscriberServiceMock).findOrCreateSubscriberForAssignee(MUNICIPALITY_ID, NAMESPACE, assignedUserId);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID);
		verify(subscriptionRepositoryMock, never()).save(any());
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock);
		verifyNoInteractions(accessControlServiceMock);
	}

	@Test
	void autoSubscribeErrandAssigneeCreatesSubscriberAndSubscription() {
		final var assignedUserId = "joe01doe";
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withAssignedUserId(assignedUserId);
		when(subscriberServiceMock.findOrCreateSubscriberForAssignee(MUNICIPALITY_ID, NAMESPACE, assignedUserId)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(false);
		when(subscriptionRepositoryMock.save(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.<SubscriptionEntity>getArgument(0).withId("new-sub-id"));

		service.autoSubscribeErrandAssignee(errand);

		verify(subscriberServiceMock).findOrCreateSubscriberForAssignee(MUNICIPALITY_ID, NAMESPACE, assignedUserId);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID);
		verify(subscriptionRepositoryMock).save(entityCaptor.capture());
		final var saved = entityCaptor.getValue();
		assertThat(saved.getSubscriber()).isSameAs(subscriber);
		assertThat(saved.getErrand()).isSameAs(errand);
		assertThat(saved.getTargetType()).isEqualTo(DB_ERRAND);
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock);
		verifyNoInteractions(accessControlServiceMock);
	}

	@Test
	void handleAutoSubscribeEventDelegatesAndSwallowsExceptions() {
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withAssignedUserId("joe01doe");
		final var event = new AutoSubscribeEvent(errand);
		doThrow(new RuntimeException("boom")).when(subscriberServiceMock).findOrCreateSubscriberForAssignee(any(), any(), any());

		service.handleAutoSubscribeEvent(event);

		verify(subscriberServiceMock).findOrCreateSubscriberForAssignee(MUNICIPALITY_ID, NAMESPACE, "joe01doe");
		verifyNoMoreInteractions(subscriberServiceMock);
		verifyNoInteractions(subscriptionRepositoryMock, accessControlServiceMock);
	}

	@Test
	void createErrandSubscriptionOnInaccessibleErrandIsRejected() {
		// Subscribing must not be usable to reach an errand the caller cannot read.
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID));

		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR))
			.thenThrow(Problem.valueOf(UNAUTHORIZED, "Errand not accessible by user 'rob01rep'"));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(UNAUTHORIZED);

		verify(subscriptionRepositoryMock, never()).saveAndFlush(any());
	}
}
