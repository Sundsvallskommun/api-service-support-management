package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.time.OffsetDateTime;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TypeTagEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(TypeTagEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("categoryTagEntity"),
			hasValidBeanEqualsExcluding("categoryTagEntity"),
			hasValidBeanToStringExcluding("categoryTagEntity")));
	}

	@Test
	void hasValidBuilderMethods() {

		final var created = OffsetDateTime.now().minusDays(1);
		final var displayName = "displayName";
		final var escalationEmail = "escalationEmail";
		final var id = 1L;
		final var modified = OffsetDateTime.now();
		final var name = "name";
		final var categoryTagEntity = CategoryTagEntity.create();

		final var entity = TypeTagEntity.create()
			.withCategoryTagEntity(categoryTagEntity)
			.withCreated(created)
			.withDisplayName(displayName)
			.withEscalationEmail(escalationEmail)
			.withId(id)
			.withModified(modified)
			.withName(name);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCategoryTagEntity()).isEqualTo(categoryTagEntity);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getEscalationEmail()).isEqualTo(escalationEmail);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getName()).isEqualTo(name);
	}

	@Test
	void testOnCreate() {
		final var entity = TypeTagEntity.create();
		entity.onCreate();

		Assertions.assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		Assertions.assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void testOnUpdate() {
		final var entity = TypeTagEntity.create();
		entity.onUpdate();

		Assertions.assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		Assertions.assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(TypeTagEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new TypeTagEntity()).hasAllNullFieldsOrProperties();
	}
}
