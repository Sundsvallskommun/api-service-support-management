package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

class TypeEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(TypeEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("categoryEntity"),
			hasValidBeanEqualsExcluding("categoryEntity"),
			hasValidBeanToStringExcluding("categoryEntity")));
	}

	@Test
	void hasValidBuilderMethods() {

		final var created = OffsetDateTime.now().minusDays(1);
		final var displayName = "displayName";
		final var escalationEmail = "escalationEmail";
		final var id = 1L;
		final var modified = OffsetDateTime.now();
		final var name = "name";
		final var categoryEntity = CategoryEntity.create();

		final var entity = TypeEntity.create()
			.withCategoryEntity(categoryEntity)
			.withCreated(created)
			.withDisplayName(displayName)
			.withEscalationEmail(escalationEmail)
			.withId(id)
			.withModified(modified)
			.withName(name);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCategoryEntity()).isEqualTo(categoryEntity);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getEscalationEmail()).isEqualTo(escalationEmail);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getName()).isEqualTo(name);
	}

	@Test
	void testOnCreate() {
		final var entity = TypeEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void testOnUpdate() {
		final var entity = TypeEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(TypeEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new TypeEntity()).hasAllNullFieldsOrProperties();
	}
}
