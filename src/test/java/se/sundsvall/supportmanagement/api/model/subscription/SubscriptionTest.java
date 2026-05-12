package se.sundsvall.supportmanagement.api.model.subscription;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class SubscriptionTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Subscription.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "123e4567-e89b-12d3-a456-426614174000";
		final var target = SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId("b82bd8ac-1507-4d9a-958d-369261eecc15");
		final var eventFilters = List.of(EventFilter.create().withType("UPDATE"));
		final var expiresAt = now().plusDays(14);
		final var created = now();
		final var createdBy = Identifier.create().withType("adAccount").withValue("joe01doe");

		final var subscription = Subscription.create()
			.withId(id)
			.withTarget(target)
			.withEventFilters(eventFilters)
			.withExpiresAt(expiresAt)
			.withCreated(created)
			.withCreatedBy(createdBy);

		assertThat(subscription.getId()).isEqualTo(id);
		assertThat(subscription.getTarget()).isEqualTo(target);
		assertThat(subscription.getEventFilters()).isEqualTo(eventFilters);
		assertThat(subscription.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(subscription.getCreated()).isEqualTo(created);
		assertThat(subscription.getCreatedBy()).isEqualTo(createdBy);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Subscription.create()).hasAllNullFieldsOrProperties();
		assertThat(new Subscription()).hasAllNullFieldsOrProperties();
	}
}
