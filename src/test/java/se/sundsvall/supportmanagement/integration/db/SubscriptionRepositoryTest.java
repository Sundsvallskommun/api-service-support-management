package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberSubscriptionCount;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class SubscriptionRepositoryTest {

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private SubscriberRepository subscriberRepository;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Test
	void createSubscription() {

		// Arrange
		final var subscriber = subscriberRepository.findById("subscriber-id-1").orElseThrow();
		final var errand = errandsRepository.findById("ERRAND_ID-3").orElseThrow();

		final var entity = SubscriptionEntity.create()
			.withSubscriber(subscriber)
			.withTargetType(DbSubscriptionTargetType.ERRAND)
			.withErrand(errand)
			.withEventFilters(List.of(EventFilterEmbeddable.create().withType("UPDATE").withSubtype("STATUS")))
			.withCreatedBy(IdentifierEmbeddable.create().withType("adAccount").withValue("admin01"));

		// Act
		final var result = subscriptionRepository.save(entity);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isNotBlank();
		assertThat(result.getCreated()).isNotNull();
		assertThat(result.getEventFilters()).hasSize(1);
	}

	@Test
	void findAllBySubscriberIdAndNamespaceAndMunicipalityId() {

		// Act
		final var result = subscriptionRepository.findAllBySubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(
			"subscriber-id-1", "namespace-1", "2281");

		// Assert
		assertThat(result)
			.hasSize(2)
			.extracting(SubscriptionEntity::getId)
			.containsExactlyInAnyOrder("subscription-id-1", "subscription-id-2");
	}

	@Test
	void findByIdAndSubscriberIdAndNamespaceAndMunicipalityId() {

		// Act
		final var subscription = subscriptionRepository.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(
			"subscription-id-1", "subscriber-id-1", "namespace-1", "2281")
			.orElseThrow();

		// Assert
		assertThat(subscription.getTargetType()).isEqualTo(DbSubscriptionTargetType.ERRAND);
		assertThat(subscription.getErrand().getId()).isEqualTo("ERRAND_ID-1");
		assertThat(subscription.getEventFilters()).hasSize(1);
	}

	@Test
	void findByIdAndSubscriberIdAndNamespaceAndMunicipalityIdReturnsEmptyWhenWrongSubscriber() {

		// Act
		final var result = subscriptionRepository.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(
			"subscription-id-1", "subscriber-id-2", "namespace-1", "2281");

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void existsBySubscriberIdAndTargetTypeAndErrandId() {

		// Act + Assert
		assertThat(subscriptionRepository.existsBySubscriberIdAndTargetTypeAndErrandId(
			"subscriber-id-1", DbSubscriptionTargetType.ERRAND, "ERRAND_ID-1")).isTrue();
		assertThat(subscriptionRepository.existsBySubscriberIdAndTargetTypeAndErrandId(
			"subscriber-id-1", DbSubscriptionTargetType.ERRAND, "ERRAND_ID-2")).isFalse();
	}

	@Test
	void existsBySubscriberIdAndTargetTypeAndErrandIsNull() {

		// Act + Assert — subscriber-id-1 has a NAMESPACE-scoped subscription
		assertThat(subscriptionRepository.existsBySubscriberIdAndTargetTypeAndErrandIsNull(
			"subscriber-id-1", DbSubscriptionTargetType.NAMESPACE)).isTrue();
		// subscriber-id-2 only has an ERRAND subscription
		assertThat(subscriptionRepository.existsBySubscriberIdAndTargetTypeAndErrandIsNull(
			"subscriber-id-2", DbSubscriptionTargetType.NAMESPACE)).isFalse();
	}

	@Test
	void deletingSubscriberCascadesToItsSubscriptions() {

		// Arrange — subscriber-id-1 owns subscription-id-1 (ERRAND) and subscription-id-2 (NAMESPACE)
		assertThat(subscriptionRepository.existsById("subscription-id-1")).isTrue();
		assertThat(subscriptionRepository.existsById("subscription-id-2")).isTrue();

		// Act
		subscriberRepository.deleteById("subscriber-id-1");
		subscriberRepository.flush();

		// Assert — both subscriptions removed by FK cascade
		assertThat(subscriptionRepository.existsById("subscription-id-1")).isFalse();
		assertThat(subscriptionRepository.existsById("subscription-id-2")).isFalse();
		// Subscriber-id-2's subscription is untouched
		assertThat(subscriptionRepository.existsById("subscription-id-3")).isTrue();
	}

	@Test
	void deletingErrandCascadesToItsSubscriptions() {

		// Arrange — use an isolated fixture so the test does not depend on ERRAND_ID-1's heavy testdata graph
		final var fixture = createIsolatedCascadeFixture();

		// Act
		errandsRepository.deleteById(fixture.errandId);
		errandsRepository.flush();

		// Assert
		assertThat(subscriptionRepository.existsById(fixture.errandSubscriptionId)).isFalse();          // FK cascade removed it
		assertThat(subscriptionRepository.existsById(fixture.namespaceSubscriptionId)).isTrue();        // no errand FK → untouched
		assertThat(subscriberRepository.existsById(fixture.subscriberId)).isTrue();                     // subscriber not affected
	}

	/**
	 * Creates an isolated errand + subscriber with one ERRAND-scoped and one NAMESPACE-scoped subscription.
	 * Returns just the ids needed for assertions, keeping the cascade test focused on what it verifies.
	 */
	private CascadeFixture createIsolatedCascadeFixture() {
		final var errand = errandsRepository.saveAndFlush(ErrandEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace-cascade-test")
			.withErrandNumber("KC-99990001"));

		final var subscriber = subscriberRepository.saveAndFlush(SubscriberEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace-cascade-test")
			.withName("cascade-test-subscriber")
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("cascade01")));

		final var errandSubscription = subscriptionRepository.saveAndFlush(SubscriptionEntity.create()
			.withSubscriber(subscriber)
			.withTargetType(DbSubscriptionTargetType.ERRAND)
			.withErrand(errand));

		final var namespaceSubscription = subscriptionRepository.saveAndFlush(SubscriptionEntity.create()
			.withSubscriber(subscriber)
			.withTargetType(DbSubscriptionTargetType.NAMESPACE));

		return new CascadeFixture(errand.getId(), subscriber.getId(), errandSubscription.getId(), namespaceSubscription.getId());
	}

	private record CascadeFixture(String errandId, String subscriberId, String errandSubscriptionId, String namespaceSubscriptionId) {}

	@Test
	void deletingSubscriptionDoesNotDeleteSubscriberOrErrand() {

		// Act
		subscriptionRepository.deleteById("subscription-id-1");
		subscriptionRepository.flush();

		// Assert
		assertThat(subscriptionRepository.existsById("subscription-id-1")).isFalse();
		assertThat(subscriberRepository.existsById("subscriber-id-1")).isTrue();
		assertThat(errandsRepository.existsById("ERRAND_ID-1")).isTrue();
	}

	@Test
	void savingSubscriptionWithNonExistentErrandFailsFkConstraint() {

		// Arrange — a detached errand reference pointing to an id that does not exist in the database.
		// No cascade is configured from Subscription to Errand, so Hibernate uses the id as-is and the
		// DB FK constraint must fire.
		final var subscriber = subscriberRepository.findById("subscriber-id-1").orElseThrow();
		final var detachedErrand = ErrandEntity.create()
			.withId("does-not-exist")
			.withErrandNumber("KC-NOPE-0001");
		final var subscription = SubscriptionEntity.create()
			.withSubscriber(subscriber)
			.withTargetType(DbSubscriptionTargetType.ERRAND)
			.withErrand(detachedErrand);

		// Act + Assert
		assertThatThrownBy(() -> subscriptionRepository.saveAndFlush(subscription))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void countBySubscriberId() {

		// Act + Assert — fixture: subscriber-id-1 has 2 subscriptions, subscriber-id-2 has 1, subscriber-id-3 has 0
		assertThat(subscriptionRepository.countBySubscriberId("subscriber-id-1")).isEqualTo(2);
		assertThat(subscriptionRepository.countBySubscriberId("subscriber-id-2")).isEqualTo(1);
		assertThat(subscriptionRepository.countBySubscriberId("subscriber-id-3")).isZero();
		assertThat(subscriptionRepository.countBySubscriberId("does-not-exist")).isZero();
	}

	@Test
	void countBySubscriberIdInReturnsCountsForGivenSubscribers() {

		// Act — covers a subscriber with multiple subs and a subscriber with one sub
		final var result = subscriptionRepository.countBySubscriberIdIn(List.of("subscriber-id-1", "subscriber-id-2"));

		// Assert
		assertThat(result)
			.extracting(SubscriberSubscriptionCount::subscriberId, SubscriberSubscriptionCount::count)
			.containsExactlyInAnyOrder(
				tuple("subscriber-id-1", 2L),
				tuple("subscriber-id-2", 1L));
	}

	@Test
	void countBySubscriberIdInSkipsSubscribersWithoutSubscriptions() {

		// Act — subscriber-id-3 has no subscriptions, must not show up in the result
		final var result = subscriptionRepository.countBySubscriberIdIn(List.of("subscriber-id-1", "subscriber-id-3"));

		// Assert
		assertThat(result)
			.extracting(SubscriberSubscriptionCount::subscriberId, SubscriberSubscriptionCount::count)
			.containsExactly(tuple("subscriber-id-1", 2L));
	}

	@Test
	void countBySubscriberIdInWithEmptyListReturnsEmpty() {

		// Act
		final var result = subscriptionRepository.countBySubscriberIdIn(List.of());

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void countBySubscriberIdInWithUnknownIdsReturnsEmpty() {

		// Act
		final var result = subscriptionRepository.countBySubscriberIdIn(List.of("does-not-exist-1", "does-not-exist-2"));

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void createNamespaceScopedSubscriptionHasNoErrand() {

		// Arrange
		final var subscriber = SubscriberEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace-1")
			.withName("ns-scope-owner")
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("ns01user"));
		final var savedSubscriber = subscriberRepository.save(subscriber);

		final var subscription = SubscriptionEntity.create()
			.withSubscriber(savedSubscriber)
			.withTargetType(DbSubscriptionTargetType.NAMESPACE);

		// Act
		final var result = subscriptionRepository.save(subscription);

		// Assert
		assertThat(result.getId()).isNotBlank();
		assertThat(result.getErrand()).isNull();
		assertThat(result.getTargetType()).isEqualTo(DbSubscriptionTargetType.NAMESPACE);
	}
}
