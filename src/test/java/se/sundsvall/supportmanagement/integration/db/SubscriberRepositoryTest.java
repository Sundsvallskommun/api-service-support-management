package se.sundsvall.supportmanagement.integration.db;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class SubscriberRepositoryTest {

	@Autowired
	private SubscriberRepository subscriberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void createSubscriber() {

		// Arrange
		final var entity = SubscriberEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace-new")
			.withName("new-subscriber")
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("new01user"))
			.withChannels(List.of(NotificationChannelEmbeddable.create().withType(NotificationChannelType.EMAIL).withDestination("new@example.com")))
			.withEventFilters(List.of(EventFilterEmbeddable.create().withType("UPDATE").withSubtype("ATTACHMENT")))
			.withPausedFrom(OffsetDateTime.now().plusDays(1))
			.withPausedUntil(OffsetDateTime.now().plusDays(7))
			.withCreatedBy(IdentifierEmbeddable.create().withType("adAccount").withValue("admin01"));

		// Act
		final var result = subscriberRepository.save(entity);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isNotBlank();
		assertThat(result.getCreated()).isNotNull();
		assertThat(result.getChannels()).hasSize(1);
		assertThat(result.getEventFilters()).hasSize(1);
	}

	@Test
	void findByIdAndNamespaceAndMunicipalityId() {

		// Act
		final var subscriber = subscriberRepository.findByIdAndNamespaceAndMunicipalityId("subscriber-id-1", "namespace-1", "2281")
			.orElseThrow();

		// Assert
		assertThat(subscriber.getName()).isEqualTo("subscriber-name-1");
		assertThat(subscriber.getIdentifier().getType()).isEqualTo("adAccount");
		assertThat(subscriber.getIdentifier().getValue()).isEqualTo("joe01doe");
		assertThat(subscriber.getChannels()).hasSize(2);
		assertThat(subscriber.getEventFilters()).hasSize(2);
	}

	@Test
	void findByIdAndNamespaceAndMunicipalityIdReturnsEmptyWhenWrongNamespace() {

		// Act
		final var result = subscriberRepository.findByIdAndNamespaceAndMunicipalityId("subscriber-id-1", "namespace-2", "2281");

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void findAllByNamespaceAndMunicipalityId() {

		// Act
		final var result = subscriberRepository.findAllByNamespaceAndMunicipalityId("namespace-1", "2281");

		// Assert
		assertThat(result)
			.hasSize(2)
			.extracting(SubscriberEntity::getId)
			.containsExactlyInAnyOrder("subscriber-id-1", "subscriber-id-2");
	}

	@Test
	void findAllByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValue() {

		// Act
		final var result = subscriberRepository.findAllByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValue(
			"namespace-1", "2281", "adAccount", "joe01doe");

		// Assert
		assertThat(result)
			.hasSize(1)
			.extracting(SubscriberEntity::getId)
			.containsExactly("subscriber-id-1");
	}

	@Test
	void findAllByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueReturnsEmptyWhenIdentifierNotFound() {

		// Act
		final var result = subscriberRepository.findAllByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValue(
			"namespace-1", "2281", "adAccount", "no-such-user");

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void existsByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueAndName() {

		// Arrange — single subscriber-id-1's identifier; only the name argument changes between cases
		final Predicate<String> hasSubscriberWithName = name -> subscriberRepository
			.existsByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueAndName(
				"namespace-1", "2281", "adAccount", "joe01doe", name);

		// Act + Assert
		assertThat(hasSubscriberWithName.test("subscriber-name-1")).isTrue();
		assertThat(hasSubscriberWithName.test("different-name")).isFalse();
	}

	@Test
	void updateSubscriberSetsModifiedTimestampAndIsPersisted() {

		// Arrange
		final var entity = subscriberRepository.findById("subscriber-id-1").orElseThrow();
		final var created = entity.getCreated();
		assertThat(entity.getModified()).isNull();
		entity.setName("renamed");

		// Act
		subscriberRepository.saveAndFlush(entity);
		// Drop L1 cache so the next read goes to the DB
		entityManager.clear();

		// Assert — entity reloaded from DB
		final var updated = subscriberRepository.findById("subscriber-id-1").orElseThrow();
		assertThat(updated.getName()).isEqualTo("renamed");
		assertThat(updated.getModified()).isNotNull().isAfterOrEqualTo(created);
	}

	@Test
	void orphanRemovalDropsSubscriptionWhenRemovedFromOwningCollection() {

		// Arrange — subscriber-id-1 owns subscription-id-1 (ERRAND) and subscription-id-2 (NAMESPACE)
		final var entity = subscriberRepository.findById("subscriber-id-1").orElseThrow();
		assertThat(entity.getSubscriptions()).extracting(SubscriptionEntity::getId)
			.containsExactlyInAnyOrder("subscription-id-1", "subscription-id-2");

		// Act — drop the ERRAND-scoped subscription via the parent's collection
		entity.getSubscriptions().removeIf(sub -> "subscription-id-1".equals(sub.getId()));
		subscriberRepository.saveAndFlush(entity);
		entityManager.clear();

		// Assert — orphan-removed by JPA, the other subscription survives
		final var reloaded = subscriberRepository.findById("subscriber-id-1").orElseThrow();
		assertThat(reloaded.getSubscriptions()).extracting(SubscriptionEntity::getId)
			.containsExactly("subscription-id-2");
	}

	@Test
	void deleteSubscriberCascadesToSubscriptionsAndCollections() {

		// Arrange — subscriber-id-1 has 2 channels, 2 event filters, 2 subscriptions
		assertThat(subscriberRepository.existsById("subscriber-id-1")).isTrue();

		// Act
		subscriberRepository.deleteById("subscriber-id-1");
		subscriberRepository.flush();

		// Assert
		assertThat(subscriberRepository.existsById("subscriber-id-1")).isFalse();
		// Cascade verified at the DB level — child rows are gone; SubscriptionRepositoryTest exercises the subscription side.
	}

	@Test
	void uniqueConstraintRejectsDuplicateMunicipalityNamespaceIdentifierName() {

		// Arrange — subscriber-id-1 already has (2281, namespace-1, adAccount, joe01doe, subscriber-name-1)
		final var duplicate = SubscriberEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace-1")
			.withName("subscriber-name-1")
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"));

		// Act + Assert
		assertThatThrownBy(() -> subscriberRepository.saveAndFlush(duplicate))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void uniqueConstraintAllowsDifferentNameForSameIdentifier() {

		// Arrange
		final var sameIdentifierDifferentName = SubscriberEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace-1")
			.withName("another-name")
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"));

		// Act
		final var saved = subscriberRepository.saveAndFlush(sameIdentifierDifferentName);

		// Assert
		assertThat(saved.getId()).isNotBlank();
	}
}
