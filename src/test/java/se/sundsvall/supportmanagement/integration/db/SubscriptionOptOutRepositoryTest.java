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

	private static final String AD_ACCOUNT = "adAccount";
	private static final String USER = "optout01";

	@Autowired
	private SubscriptionOptOutRepository subscriptionOptOutRepository;

	@Autowired
	private SubscriptionProfileRepository subscriptionProfileRepository;

	private SubscriptionProfileEntity profile;

	@BeforeEach
	void setUp() {
		profile = subscriptionProfileRepository.saveAndFlush(SubscriptionProfileEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace-opt-out-test")
			.withName("opt-out-test")
			.withEventFilters(new ArrayList<>(List.of(EventFilterEmbeddable.create().withType("UPDATE"))))
			.withChannels(new ArrayList<>(List.of(NotificationChannelType.EMAIL))));
	}

	private SubscriptionOptOutEntity optOut() {
		return SubscriptionOptOutEntity.create()
			.withProfile(profile)
			.withIdentifier(IdentifierEmbeddable.create().withType(AD_ACCOUNT).withValue(USER));
	}

	@Test
	void saveAndFind() {

		// Act
		final var saved = subscriptionOptOutRepository.saveAndFlush(optOut());

		// Assert
		assertThat(saved.getId()).isNotBlank();
		assertThat(saved.getCreated()).isNotNull();
		assertThat(subscriptionOptOutRepository.existsByProfileIdAndIdentifierTypeAndIdentifierValue(profile.getId(), AD_ACCOUNT, USER)).isTrue();
		assertThat(subscriptionOptOutRepository.existsByProfileIdAndIdentifierTypeAndIdentifierValue(profile.getId(), AD_ACCOUNT, "someone-else")).isFalse();
		assertThat(subscriptionOptOutRepository.existsByProfileIdAndIdentifierTypeAndIdentifierValue("other-profile", AD_ACCOUNT, USER)).isFalse();
		assertThat(subscriptionOptOutRepository.findAllByProfileId(profile.getId()))
			.extracting(found -> found.getIdentifier().getValue())
			.containsExactly(USER);
	}

	@Test
	void deleteByProfileIdAndIdentifier() {

		// Arrange
		subscriptionOptOutRepository.saveAndFlush(optOut());

		// Act
		subscriptionOptOutRepository.deleteByProfileIdAndIdentifierTypeAndIdentifierValue(profile.getId(), AD_ACCOUNT, USER);
		subscriptionOptOutRepository.flush();

		// Assert
		assertThat(subscriptionOptOutRepository.existsByProfileIdAndIdentifierTypeAndIdentifierValue(profile.getId(), AD_ACCOUNT, USER)).isFalse();
	}

	@Test
	void deletingProfileCascadesToItsOptOuts() {

		// Arrange
		final var saved = subscriptionOptOutRepository.saveAndFlush(optOut());

		// Act
		subscriptionProfileRepository.deleteById(profile.getId());
		subscriptionProfileRepository.flush();

		// Assert
		assertThat(subscriptionOptOutRepository.existsById(saved.getId())).isFalse();
	}
}
