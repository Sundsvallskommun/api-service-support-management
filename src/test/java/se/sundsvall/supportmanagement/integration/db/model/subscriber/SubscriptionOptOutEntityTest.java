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
			hasValidBeanHashCodeExcluding("subscriber", "profile"),
			hasValidBeanEqualsExcluding("subscriber", "profile"),
			hasValidBeanToStringExcluding("subscriber", "profile")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "123e4567-e89b-12d3-a456-426614174000";
		final var subscriber = SubscriberEntity.create().withId("subscriber-id");
		final var profile = SubscriptionProfileEntity.create().withId("profile-id");
		final var created = now();

		final var entity = SubscriptionOptOutEntity.create()
			.withId(id)
			.withSubscriber(subscriber)
			.withProfile(profile)
			.withCreated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getSubscriber()).isEqualTo(subscriber);
		assertThat(entity.getProfile()).isEqualTo(profile);
		assertThat(entity.getCreated()).isEqualTo(created);
	}

	@Test
	void equalityFollowsTheIdsOfSubscriberAndProfile() {
		final var first = SubscriptionOptOutEntity.create().withId("id")
			.withSubscriber(SubscriberEntity.create().withId("subscriber-id").withName("one"))
			.withProfile(SubscriptionProfileEntity.create().withId("profile-id"));
		final var second = SubscriptionOptOutEntity.create().withId("id")
			.withSubscriber(SubscriberEntity.create().withId("subscriber-id").withName("other"))
			.withProfile(SubscriptionProfileEntity.create().withId("profile-id"));

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
		assertThat(first.toString()).contains("subscriberId=subscriber-id", "profileId=profile-id");
		assertThat(first).isNotEqualTo(SubscriptionOptOutEntity.create().withId("id"));
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
