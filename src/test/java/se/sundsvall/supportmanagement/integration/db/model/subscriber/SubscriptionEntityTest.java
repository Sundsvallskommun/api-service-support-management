package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

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

class SubscriptionEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(SubscriptionEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("subscriber", "errand"),
			hasValidBeanEqualsExcluding("subscriber", "errand"),
			hasValidBeanToStringExcluding("subscriber", "errand")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "123e4567-e89b-12d3-a456-426614174000";
		final var subscriber = SubscriberEntity.create().withId("subscriber-id");
		final var targetType = SubscriptionTargetType.ERRAND;
		final var errand = ErrandEntity.create().withId("errand-id");
		final var eventFilters = List.of(EventFilterEmbeddable.create().withType("UPDATE").withSubtype("ATTACHMENT"));
		final var expiresAt = now().plusDays(7);
		final var created = now().minusDays(1);
		final var createdBy = IdentifierEmbeddable.create().withType("adAccount").withValue("admin01");

		final var entity = SubscriptionEntity.create()
			.withId(id)
			.withSubscriber(subscriber)
			.withTargetType(targetType)
			.withErrand(errand)
			.withEventFilters(eventFilters)
			.withExpiresAt(expiresAt)
			.withCreated(created)
			.withCreatedBy(createdBy);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getSubscriber()).isEqualTo(subscriber);
		assertThat(entity.getTargetType()).isEqualTo(targetType);
		assertThat(entity.getErrand()).isEqualTo(errand);
		assertThat(entity.getEventFilters()).isEqualTo(eventFilters);
		assertThat(entity.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getCreatedBy()).isEqualTo(createdBy);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(SubscriptionEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new SubscriptionEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testOnCreate() {
		final var entity = new SubscriptionEntity();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}
}
