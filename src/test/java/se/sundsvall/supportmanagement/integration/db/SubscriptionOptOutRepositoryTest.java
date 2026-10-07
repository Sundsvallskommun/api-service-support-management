package se.sundsvall.supportmanagement.integration.db;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionOptOutEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class SubscriptionOptOutRepositoryTest {

	@Autowired
	private SubscriptionOptOutRepository subscriptionOptOutRepository;

	@Autowired
	private SubscriptionProfileRepository subscriptionProfileRepository;

	@Autowired
	private SubscriberRepository subscriberRepository;

	private SubscriberEntity subscriber;
	private SubscriptionProfileEntity profile;

	@BeforeEach
	void setUp() {
		profile = subscriptionProfileRepository.saveAndFlush(SubscriptionProfileEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace-opt-out-test")
			.withName("opt-out-test")
			.withEventFilters(new ArrayList<>(List.of(EventFilterEmbeddable.create().withType("UPDATE"))))
			.withChannels(new ArrayList<>(List.of(NotificationChannelType.EMAIL))));

		subscriber = subscriberRepository.saveAndFlush(SubscriberEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace-opt-out-test")
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("optout01")));
	}

	@Test
	void saveAndFind() {

		// Act
		final var saved = subscriptionOptOutRepository.saveAndFlush(SubscriptionOptOutEntity.create().withSubscriber(subscriber).withProfile(profile));

		// Assert
		assertThat(saved.getId()).isNotBlank();
		assertThat(saved.getCreated()).isNotNull();
		assertThat(subscriptionOptOutRepository.existsBySubscriberIdAndProfileId(subscriber.getId(), profile.getId())).isTrue();
		assertThat(subscriptionOptOutRepository.existsBySubscriberIdAndProfileId(subscriber.getId(), "other-profile")).isFalse();
		assertThat(subscriptionOptOutRepository.findAllByProfileId(profile.getId()))
			.extracting(optOut -> optOut.getSubscriber().getId())
			.containsExactly(subscriber.getId());
	}

	@Test
	void deleteBySubscriberIdAndProfileId() {

		// Arrange
		subscriptionOptOutRepository.saveAndFlush(SubscriptionOptOutEntity.create().withSubscriber(subscriber).withProfile(profile));

		// Act
		subscriptionOptOutRepository.deleteBySubscriberIdAndProfileId(subscriber.getId(), profile.getId());
		subscriptionOptOutRepository.flush();

		// Assert
		assertThat(subscriptionOptOutRepository.existsBySubscriberIdAndProfileId(subscriber.getId(), profile.getId())).isFalse();
	}

	@Test
	void deletingProfileCascadesToItsOptOuts() {

		// Arrange
		final var saved = subscriptionOptOutRepository.saveAndFlush(SubscriptionOptOutEntity.create().withSubscriber(subscriber).withProfile(profile));

		// Act
		subscriptionProfileRepository.deleteById(profile.getId());
		subscriptionProfileRepository.flush();

		// Assert
		assertThat(subscriptionOptOutRepository.existsById(saved.getId())).isFalse();
	}
}
