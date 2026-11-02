package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class SubscriptionProfileEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(SubscriptionProfileEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "123e4567-e89b-12d3-a456-426614174000";
		final var municipalityId = "2281";
		final var namespace = "MY_NAMESPACE";
		final var name = "Mejl om nya ärenden";
		final var description = "Används av enhetschefer";
		final var eventFilters = List.of(EventFilterEmbeddable.create().withType("CREATE").withSubtype("ERRAND"));
		final var channels = List.of(NotificationChannelType.EMAIL);
		final var created = now().minusDays(1);
		final var modified = now();

		final var entity = SubscriptionProfileEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withName(name)
			.withDescription(description)
			.withEventFilters(eventFilters)
			.withChannels(channels)
			.withCreated(created)
			.withModified(modified);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getDescription()).isEqualTo(description);
		assertThat(entity.getEventFilters()).isEqualTo(eventFilters);
		assertThat(entity.getChannels()).isEqualTo(channels);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(SubscriptionProfileEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new SubscriptionProfileEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testOnCreate() {
		final var entity = new SubscriptionProfileEntity();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void testOnUpdate() {
		final var entity = new SubscriptionProfileEntity();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}
}
