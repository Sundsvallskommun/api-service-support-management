package se.sundsvall.supportmanagement.api.model.subscriber;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class SubscriberTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Subscriber.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "123e4567-e89b-12d3-a456-426614174000";
		final var name = "Servicedesk-bevakning";
		final var identifier = Identifier.create().withType("adAccount").withValue("joe01doe");
		final var channels = List.of(NotificationChannel.create().withType(NotificationChannelType.EMAIL));
		final var eventFilters = List.of(EventFilter.create().withType("UPDATE").withSubtype("ATTACHMENT"));
		final var pausedFrom = now();
		final var pausedUntil = now().plusDays(7);
		final var created = now();
		final var modified = now();
		final var createdBy = Identifier.create().withType("adAccount").withValue("admin01");
		final var subscriptionCount = 3;

		final var subscriber = Subscriber.create()
			.withId(id)
			.withName(name)
			.withIdentifier(identifier)
			.withChannels(channels)
			.withEventFilters(eventFilters)
			.withPausedFrom(pausedFrom)
			.withPausedUntil(pausedUntil)
			.withCreated(created)
			.withModified(modified)
			.withCreatedBy(createdBy)
			.withSubscriptionCount(subscriptionCount);

		assertThat(subscriber.getId()).isEqualTo(id);
		assertThat(subscriber.getName()).isEqualTo(name);
		assertThat(subscriber.getIdentifier()).isEqualTo(identifier);
		assertThat(subscriber.getChannels()).isEqualTo(channels);
		assertThat(subscriber.getEventFilters()).isEqualTo(eventFilters);
		assertThat(subscriber.getPausedFrom()).isEqualTo(pausedFrom);
		assertThat(subscriber.getPausedUntil()).isEqualTo(pausedUntil);
		assertThat(subscriber.getCreated()).isEqualTo(created);
		assertThat(subscriber.getModified()).isEqualTo(modified);
		assertThat(subscriber.getCreatedBy()).isEqualTo(createdBy);
		assertThat(subscriber.getSubscriptionCount()).isEqualTo(subscriptionCount);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Subscriber.create()).hasAllNullFieldsOrProperties();
		assertThat(new Subscriber()).hasAllNullFieldsOrProperties();
	}
}
