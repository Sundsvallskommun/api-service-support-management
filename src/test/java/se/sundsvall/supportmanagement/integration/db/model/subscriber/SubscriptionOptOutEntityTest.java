package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class SubscriptionOptOutEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(SubscriptionOptOutEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("profile"),
			hasValidBeanEqualsExcluding("profile"),
			hasValidBeanToStringExcluding("profile")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "123e4567-e89b-12d3-a456-426614174000";
		final var profile = SubscriptionProfileEntity.create().withId("profile-id");
		final var identifier = IdentifierEmbeddable.create().withType("adAccount").withValue("anna01");
		final var created = now();

		final var entity = SubscriptionOptOutEntity.create()
			.withId(id)
			.withProfile(profile)
			.withIdentifier(identifier)
			.withCreated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getProfile()).isEqualTo(profile);
		assertThat(entity.getIdentifier()).isEqualTo(identifier);
		assertThat(entity.getCreated()).isEqualTo(created);
	}

	@Test
	void equalityFollowsTheIdOfTheProfile() {
		final var identifier = IdentifierEmbeddable.create().withType("adAccount").withValue("anna01");
		final var first = SubscriptionOptOutEntity.create().withId("id").withIdentifier(identifier)
			.withProfile(SubscriptionProfileEntity.create().withId("profile-id").withName("one"));
		final var second = SubscriptionOptOutEntity.create().withId("id").withIdentifier(identifier)
			.withProfile(SubscriptionProfileEntity.create().withId("profile-id").withName("other"));

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
		assertThat(first.toString()).contains("profileId=profile-id", "anna01");
		assertThat(first).isNotEqualTo(SubscriptionOptOutEntity.create().withId("id").withIdentifier(identifier));
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(SubscriptionOptOutEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new SubscriptionOptOutEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testOnCreate() {
		final var entity = new SubscriptionOptOutEntity();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}
}
