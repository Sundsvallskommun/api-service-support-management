package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;

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

class SubscriberEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(SubscriberEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("subscriptions"),
			hasValidBeanEqualsExcluding("subscriptions"),
			hasValidBeanToStringExcluding("subscriptions")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "123e4567-e89b-12d3-a456-426614174000";
		final var municipalityId = "2281";
		final var namespace = "MY_NAMESPACE";
		final var name = "Servicedesk-bevakning";
		final var identifier = IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe");
		final var channels = List.of(NotificationChannelEmbeddable.create().withType(NotificationChannelType.EMAIL).withDestination("user@example.com"));
		final var eventFilters = List.of(EventFilterEmbeddable.create().withType("UPDATE").withSubtype("ATTACHMENT"));
		final var pausedFrom = now().plusDays(1);
		final var pausedUntil = now().plusDays(7);
		final var created = now().minusDays(1);
		final var modified = now();
		final var createdBy = IdentifierEmbeddable.create().withType("adAccount").withValue("admin01");
		final var subscriptions = List.of(SubscriptionEntity.create().withId("sub-1"));

		final var entity = SubscriberEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withName(name)
			.withIdentifier(identifier)
			.withChannels(channels)
			.withEventFilters(eventFilters)
			.withPausedFrom(pausedFrom)
			.withPausedUntil(pausedUntil)
			.withCreated(created)
			.withModified(modified)
			.withCreatedBy(createdBy)
			.withSubscriptions(subscriptions);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getIdentifier()).isEqualTo(identifier);
		assertThat(entity.getChannels()).isEqualTo(channels);
		assertThat(entity.getEventFilters()).isEqualTo(eventFilters);
		assertThat(entity.getPausedFrom()).isEqualTo(pausedFrom);
		assertThat(entity.getPausedUntil()).isEqualTo(pausedUntil);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getCreatedBy()).isEqualTo(createdBy);
		assertThat(entity.getSubscriptions()).isEqualTo(subscriptions);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(SubscriberEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new SubscriberEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testOnCreate() {
		final var entity = new SubscriberEntity();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void testOnUpdate() {
		final var entity = new SubscriberEntity();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}
}
