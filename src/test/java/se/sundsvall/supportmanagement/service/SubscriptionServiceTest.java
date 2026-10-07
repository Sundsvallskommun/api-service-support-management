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
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.model.subscription.Subscription;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTarget;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionOptOutRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionOptOutEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

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
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

	private static final String IDENTIFIER_TYPE = "adAccount";
	private static final String IDENTIFIER_VALUE = "joe01doe";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String SUBSCRIBER_ID = "subscriber-1";
	private static final String ERRAND_ID = "errand-1";
	private static final String PROFILE_ID = "profile-1";
	private static final DbSubscriptionTargetType DB_ERRAND = DbSubscriptionTargetType.ERRAND;
	private static final DbSubscriptionTargetType DB_NAMESPACE = DbSubscriptionTargetType.NAMESPACE;

	@Mock
	private SubscriberService subscriberServiceMock;

	@Mock
	private SubscriptionProfileService subscriptionProfileServiceMock;

	@Mock
	private SubscriptionRepository subscriptionRepositoryMock;

	@Mock
	private SubscriptionOptOutRepository subscriptionOptOutRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	@InjectMocks
	private SubscriptionService service;

	@Captor
	private ArgumentCaptor<SubscriptionEntity> entityCaptor;

	@Captor
	private ArgumentCaptor<SubscriptionOptOutEntity> optOutCaptor;

	@BeforeEach
	void setUpIdentity() {
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(IDENTIFIER_VALUE));
	}

	@AfterEach
	void clearIdentity() {
		Identifier.remove();
	}

	@Test
	void findSubscriptions() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
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
		verifyNoInteractions(errandsRepositoryMock);
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
		verifyNoInteractions(subscriptionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void createErrandSubscriptionHappyPath() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var errand = new ErrandEntity().withId(ERRAND_ID);
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID));

		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileIsNull(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(false);
		final var newId = randomUUID().toString();
		when(subscriptionRepositoryMock.saveAndFlush(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.<SubscriptionEntity>getArgument(0).withId(newId));

		final var result = service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto);

		assertThat(result).isEqualTo(newId);
		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileIsNull(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID);
		verify(subscriptionRepositoryMock).saveAndFlush(entityCaptor.capture());
		final var saved = entityCaptor.getValue();
		assertThat(saved.getSubscriber()).isSameAs(subscriber);
		assertThat(saved.getErrand()).isSameAs(errand);
		assertThat(saved.getTargetType()).isEqualTo(DB_ERRAND);
		assertThat(saved.getCreatedBy().getType()).isEqualTo("adAccount");
		assertThat(saved.getCreatedBy().getValue()).isEqualTo("joe01doe");
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void createNamespaceSubscriptionHappyPath() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE));

		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIsNullAndProfileIsNull(SUBSCRIBER_ID, DB_NAMESPACE)).thenReturn(false);
		when(subscriptionRepositoryMock.saveAndFlush(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.<SubscriptionEntity>getArgument(0).withId("new"));

		service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIsNullAndProfileIsNull(SUBSCRIBER_ID, DB_NAMESPACE);
		verify(subscriptionRepositoryMock).saveAndFlush(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrand()).isNull();
		assertThat(entityCaptor.getValue().getTargetType()).isEqualTo(DB_NAMESPACE);
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock);
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
		verifyNoInteractions(subscriptionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void createErrandSubscriptionErrandNotFound() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionRepositoryMock, never()).saveAndFlush(any());
		verifyNoMoreInteractions(subscriberServiceMock, errandsRepositoryMock);
		verifyNoInteractions(subscriptionRepositoryMock);
	}

	@Test
	void createErrandSubscriptionConflict() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var errand = new ErrandEntity().withId(ERRAND_ID);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileIsNull(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(true);

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(CONFLICT);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileIsNull(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID);
		verify(subscriptionRepositoryMock, never()).saveAndFlush(any());
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void createNamespaceSubscriptionConflict() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIsNullAndProfileIsNull(SUBSCRIBER_ID, DB_NAMESPACE)).thenReturn(true);

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(CONFLICT);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIsNullAndProfileIsNull(SUBSCRIBER_ID, DB_NAMESPACE);
		verify(subscriptionRepositoryMock, never()).saveAndFlush(any());
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock);
	}

	@Test
	void createNamespaceSubscriptionWithProfile() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var profile = SubscriptionProfileEntity.create().withId(PROFILE_ID);
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE))
			.withProfileId(PROFILE_ID);

		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID)).thenReturn(profile);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIsNullAndProfileId(SUBSCRIBER_ID, DB_NAMESPACE, PROFILE_ID)).thenReturn(false);
		when(subscriptionRepositoryMock.saveAndFlush(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.<SubscriptionEntity>getArgument(0).withId("new"));

		service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(subscriptionProfileServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIsNullAndProfileId(SUBSCRIBER_ID, DB_NAMESPACE, PROFILE_ID);
		verify(subscriptionRepositoryMock).saveAndFlush(entityCaptor.capture());
		// Choosing the profile again clears an earlier opt-out
		verify(subscriptionOptOutRepositoryMock).deleteBySubscriberIdAndProfileId(SUBSCRIBER_ID, PROFILE_ID);
		assertThat(entityCaptor.getValue().getProfile()).isSameAs(profile);
		assertThat(entityCaptor.getValue().getTargetType()).isEqualTo(DB_NAMESPACE);
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionProfileServiceMock, subscriptionRepositoryMock, subscriptionOptOutRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock);
	}

	@Test
	void createErrandSubscriptionWithProfileConflict() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var errand = new ErrandEntity().withId(ERRAND_ID);
		final var profile = SubscriptionProfileEntity.create().withId(PROFILE_ID);
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID))
			.withProfileId(PROFILE_ID);

		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));
		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID)).thenReturn(profile);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID, PROFILE_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("with profile:'%s'".formatted(PROFILE_ID))
			.extracting("status").isEqualTo(CONFLICT);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionProfileServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID, PROFILE_ID);
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionProfileServiceMock, subscriptionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void createSubscriptionWithProfileAndEventFiltersIsRejected() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE))
			.withProfileId(PROFILE_ID)
			.withEventFilters(List.of(EventFilter.create().withType("UPDATE")));

		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verifyNoMoreInteractions(subscriberServiceMock);
		verifyNoInteractions(subscriptionProfileServiceMock, subscriptionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void createSubscriptionWithUnknownProfile() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE))
			.withProfileId(PROFILE_ID);

		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "profile missing"));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verify(subscriptionProfileServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID);
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionProfileServiceMock);
		verifyNoInteractions(subscriptionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void createSubscriptionErrandTypeMissingId() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verifyNoMoreInteractions(subscriberServiceMock);
		verifyNoInteractions(subscriptionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void createSubscriptionNamespaceTypeWithErrandId() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);

		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE).withId(ERRAND_ID));

		assertThatThrownBy(() -> service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(BAD_REQUEST);

		verify(subscriberServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verifyNoMoreInteractions(subscriberServiceMock);
		verifyNoInteractions(subscriptionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void createErrandSubscriptionRaceTranslatesDbViolationToConflict() {
		// Precheck reports no duplicate (false), but saveAndFlush throws DataIntegrityViolationException
		// â€” simulates the TOCTOU race past rejectDuplicate.
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var errand = new ErrandEntity().withId(ERRAND_ID);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileIsNull(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(false);
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
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIsNullAndProfileIsNull(SUBSCRIBER_ID, DB_NAMESPACE)).thenReturn(false);
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
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(SubscriberEntity.create().withId(SUBSCRIBER_ID)
			.withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE)));
		when(subscriptionRepositoryMock.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));

		service.deleteSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, "sub-1");

		verify(subscriptionRepositoryMock).findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionRepositoryMock).delete(entity);
		verifyNoMoreInteractions(subscriptionRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock, subscriptionOptOutRepositoryMock);
	}

	@Test
	void deleteProfileSubscriptionRecordsOptOut() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var profile = SubscriptionProfileEntity.create().withId(PROFILE_ID);
		final var entity = SubscriptionEntity.create().withId("sub-1").withSubscriber(subscriber).withTargetType(DB_NAMESPACE).withProfile(profile);
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));
		when(subscriptionOptOutRepositoryMock.existsBySubscriberIdAndProfileId(SUBSCRIBER_ID, PROFILE_ID)).thenReturn(false);

		service.deleteSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, "sub-1");

		verify(subscriptionOptOutRepositoryMock).existsBySubscriberIdAndProfileId(SUBSCRIBER_ID, PROFILE_ID);
		verify(subscriptionOptOutRepositoryMock).save(optOutCaptor.capture());
		assertThat(optOutCaptor.getValue().getSubscriber()).isSameAs(subscriber);
		assertThat(optOutCaptor.getValue().getProfile()).isSameAs(profile);
		verify(subscriptionRepositoryMock).findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionRepositoryMock).delete(entity);
		verifyNoMoreInteractions(subscriptionRepositoryMock, subscriptionOptOutRepositoryMock);
	}

	@Test
	void deleteProfileSubscriptionAlreadyOptedOut() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var entity = SubscriptionEntity.create().withId("sub-1").withSubscriber(subscriber).withTargetType(DB_NAMESPACE)
			.withProfile(SubscriptionProfileEntity.create().withId(PROFILE_ID));
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));
		when(subscriptionOptOutRepositoryMock.existsBySubscriberIdAndProfileId(SUBSCRIBER_ID, PROFILE_ID)).thenReturn(true);

		service.deleteSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, "sub-1");

		verify(subscriptionOptOutRepositoryMock).existsBySubscriberIdAndProfileId(SUBSCRIBER_ID, PROFILE_ID);
		verify(subscriptionRepositoryMock).delete(entity);
		verify(subscriptionOptOutRepositoryMock, never()).save(any());
	}

	@Test
	void deleteErrandProfileSubscriptionRecordsNoOptOut() {
		// A profile subscription for a single errand is not a membership the sync manages, so leaving it needs no record
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var entity = SubscriptionEntity.create().withId("sub-1").withSubscriber(subscriber).withTargetType(DB_ERRAND)
			.withProfile(SubscriptionProfileEntity.create().withId(PROFILE_ID));
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));

		service.deleteSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, "sub-1");

		verify(subscriptionRepositoryMock).delete(entity);
		verifyNoInteractions(subscriptionOptOutRepositoryMock);
	}

	@Test
	void deleteSubscriptionNotFound() {
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(SubscriberEntity.create().withId(SUBSCRIBER_ID)
			.withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE)));
		when(subscriptionRepositoryMock.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, "sub-1"))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(subscriptionRepositoryMock).findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId("sub-1", SUBSCRIBER_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionRepositoryMock, never()).delete(any(SubscriptionEntity.class));
		verifyNoMoreInteractions(subscriptionRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock);
	}

	@Test
	void autoSubscribeErrandAssigneeWhenNoAssignedUser() {
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);

		service.autoSubscribeErrandAssignee(errand);

		verifyNoInteractions(subscriberServiceMock, subscriptionRepositoryMock, errandsRepositoryMock);
	}

	@Test
	void autoSubscribeErrandAssigneeWhenSubscriptionAlreadyExists() {
		final var assignedUserId = "joe01doe";
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withAssignedUserId(assignedUserId);
		when(subscriberServiceMock.findOrCreateSubscriberForAssignee(MUNICIPALITY_ID, NAMESPACE, assignedUserId)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileIsNull(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(true);

		service.autoSubscribeErrandAssignee(errand);

		verify(subscriberServiceMock).findOrCreateSubscriberForAssignee(MUNICIPALITY_ID, NAMESPACE, assignedUserId);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileIsNull(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID);
		verify(subscriptionRepositoryMock, never()).save(any());
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock);
	}

	@Test
	void autoSubscribeErrandAssigneeCreatesSubscriberAndSubscription() {
		final var assignedUserId = "joe01doe";
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID).withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withAssignedUserId(assignedUserId);
		when(subscriberServiceMock.findOrCreateSubscriberForAssignee(MUNICIPALITY_ID, NAMESPACE, assignedUserId)).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileIsNull(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(false);
		when(subscriptionRepositoryMock.save(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.<SubscriptionEntity>getArgument(0).withId("new-sub-id"));

		service.autoSubscribeErrandAssignee(errand);

		verify(subscriberServiceMock).findOrCreateSubscriberForAssignee(MUNICIPALITY_ID, NAMESPACE, assignedUserId);
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileIsNull(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID);
		verify(subscriptionRepositoryMock).save(entityCaptor.capture());
		final var saved = entityCaptor.getValue();
		assertThat(saved.getSubscriber()).isSameAs(subscriber);
		assertThat(saved.getErrand()).isSameAs(errand);
		assertThat(saved.getTargetType()).isEqualTo(DB_ERRAND);
		verifyNoMoreInteractions(subscriberServiceMock, subscriptionRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock);
	}

	@Test
	void handleAutoSubscribeEventDelegatesAndSwallowsExceptions() {
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withAssignedUserId("joe01doe");
		final var event = new AutoSubscribeEvent(errand);
		doThrow(new RuntimeException("boom")).when(subscriberServiceMock).findOrCreateSubscriberForAssignee(any(), any(), any());

		service.handleAutoSubscribeEvent(event);

		verify(subscriberServiceMock).findOrCreateSubscriberForAssignee(MUNICIPALITY_ID, NAMESPACE, "joe01doe");
		verifyNoMoreInteractions(subscriberServiceMock);
		// The errand was not just created, so the reporter is not considered
		verifyNoInteractions(subscriptionRepositoryMock, errandsRepositoryMock, namespaceConfigServiceMock);
	}

	@Test
	void handleAutoSubscribeEventForCreatedErrandLooksUpTheReporterProfile() {
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withReporterUserId("rep01usr");
		when(namespaceConfigServiceMock.findReporterProfileId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		service.handleAutoSubscribeEvent(new AutoSubscribeEvent(errand, true));

		verify(namespaceConfigServiceMock).findReporterProfileId(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(subscriberServiceMock, subscriptionRepositoryMock);
	}

	@Test
	void handleAutoSubscribeEventSwallowsReporterFailures() {
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withReporterUserId("rep01usr");
		when(namespaceConfigServiceMock.findReporterProfileId(NAMESPACE, MUNICIPALITY_ID)).thenThrow(new RuntimeException("boom"));

		service.handleAutoSubscribeEvent(new AutoSubscribeEvent(errand, true));

		verify(namespaceConfigServiceMock).findReporterProfileId(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(subscriberServiceMock, subscriptionRepositoryMock);
	}

	@Test
	void autoSubscribeReporterWithoutReporter() {
		service.autoSubscribeReporter(new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE));

		verifyNoInteractions(namespaceConfigServiceMock, subscriptionProfileServiceMock, subscriberServiceMock, subscriptionRepositoryMock);
	}

	@Test
	void autoSubscribeReporterInNamespaceWithoutReporterProfile() {
		// Most namespaces have no reporter profile, and their reporters are then left alone
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withReporterUserId("rep01usr");
		when(namespaceConfigServiceMock.findReporterProfileId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		service.autoSubscribeReporter(errand);

		verify(namespaceConfigServiceMock).findReporterProfileId(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(subscriptionProfileServiceMock, subscriberServiceMock, subscriptionRepositoryMock);
	}

	@Test
	void autoSubscribeReporterCreatesProfileSubscription() {
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withReporterUserId("rep01usr");
		final var profile = SubscriptionProfileEntity.create().withId(PROFILE_ID);
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID);
		when(namespaceConfigServiceMock.findReporterProfileId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(PROFILE_ID));
		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID)).thenReturn(profile);
		when(subscriberServiceMock.findOrCreateSubscriber(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER_TYPE, "rep01usr")).thenReturn(subscriber);
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID, PROFILE_ID)).thenReturn(false);

		service.autoSubscribeReporter(errand);

		verify(subscriptionRepositoryMock).save(entityCaptor.capture());
		final var saved = entityCaptor.getValue();
		assertThat(saved.getSubscriber()).isSameAs(subscriber);
		assertThat(saved.getErrand()).isSameAs(errand);
		assertThat(saved.getProfile()).isSameAs(profile);
		assertThat(saved.getTargetType()).isEqualTo(DB_ERRAND);
		verify(namespaceConfigServiceMock).findReporterProfileId(NAMESPACE, MUNICIPALITY_ID);
		verify(subscriptionProfileServiceMock).findEntity(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID);
		verify(subscriberServiceMock).findOrCreateSubscriber(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER_TYPE, "rep01usr");
		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID, PROFILE_ID);
		verifyNoMoreInteractions(subscriptionRepositoryMock);
	}

	@Test
	void autoSubscribeReporterWhenAlreadySubscribed() {
		final var errand = new ErrandEntity().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withReporterUserId("rep01usr");
		when(namespaceConfigServiceMock.findReporterProfileId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(PROFILE_ID));
		when(subscriptionProfileServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID)).thenReturn(SubscriptionProfileEntity.create().withId(PROFILE_ID));
		when(subscriberServiceMock.findOrCreateSubscriber(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER_TYPE, "rep01usr")).thenReturn(SubscriberEntity.create().withId(SUBSCRIBER_ID));
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID, PROFILE_ID)).thenReturn(true);

		service.autoSubscribeReporter(errand);

		verify(subscriptionRepositoryMock).existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileId(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID, PROFILE_ID);
		verify(subscriptionRepositoryMock, never()).save(any());
	}

	@Test
	void createSubscriptionDoesNotRequireAccessToTheErrand() {
		// Subscribing a colleague is a supported workflow, so creation deliberately does not check errand access.
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID)
			.withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue("someone-else"));
		final var errand = new ErrandEntity().withId(ERRAND_ID);
		final var dto = Subscription.create().withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID));

		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));
		when(subscriptionRepositoryMock.existsBySubscriberIdAndTargetTypeAndErrandIdAndProfileIsNull(SUBSCRIBER_ID, DB_ERRAND, ERRAND_ID)).thenReturn(false);
		when(subscriptionRepositoryMock.saveAndFlush(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.<SubscriptionEntity>getArgument(0).withId("new-id"));

		assertThat(service.createSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, dto)).isEqualTo("new-id");
	}

	@Test
	void listingAnotherSubscribersSubscriptionsIsRefused() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID)
			.withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue("someone-else"));
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);

		assertThatThrownBy(() -> service.findSubscriptions(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(FORBIDDEN);

		verifyNoInteractions(subscriptionRepositoryMock);
	}

	@Test
	void deletingAnotherSubscribersSubscriptionIsRefused() {
		final var subscriber = SubscriberEntity.create().withId(SUBSCRIBER_ID)
			.withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue("someone-else"));
		when(subscriberServiceMock.findEntity(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);

		assertThatThrownBy(() -> service.deleteSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, "subscription-1"))
			.isInstanceOf(Problem.class)
			.extracting("status").isEqualTo(FORBIDDEN);

		verifyNoInteractions(subscriptionRepositoryMock);
	}
}
