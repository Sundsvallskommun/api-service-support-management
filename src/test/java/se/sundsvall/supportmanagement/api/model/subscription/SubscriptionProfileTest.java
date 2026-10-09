package se.sundsvall.supportmanagement.api.model.subscription;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class SubscriptionProfileTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(SubscriptionProfile.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "123e4567-e89b-12d3-a456-426614174000";
		final var name = "Mejl om nya ärenden";
		final var description = "Används av enhetschefer";
		final var eventFilters = List.of(EventFilter.create().withType("CREATE").withSubtype("ERRAND"));
		final var channels = List.of(NotificationChannelType.EMAIL);
		final var created = now().minusDays(1);
		final var modified = now();

		final var profile = SubscriptionProfile.create()
			.withId(id)
			.withName(name)
			.withDescription(description)
			.withEventFilters(eventFilters)
			.withChannels(channels)
			.withCreated(created)
			.withModified(modified);

		assertThat(profile).hasNoNullFieldsOrProperties();
		assertThat(profile.getId()).isEqualTo(id);
		assertThat(profile.getName()).isEqualTo(name);
		assertThat(profile.getDescription()).isEqualTo(description);
		assertThat(profile.getEventFilters()).isEqualTo(eventFilters);
		assertThat(profile.getChannels()).isEqualTo(channels);
		assertThat(profile.getCreated()).isEqualTo(created);
		assertThat(profile.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(SubscriptionProfile.create()).hasAllNullFieldsOrProperties();
		assertThat(new SubscriptionProfile()).hasAllNullFieldsOrProperties();
	}
}
