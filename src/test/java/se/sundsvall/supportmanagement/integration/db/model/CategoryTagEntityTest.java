package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CategoryTagEntityTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		registerValueGenerator(() -> now().plusDays(random.nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> List.of(TypeTagEntity.create().withId(random.nextLong())), List.class);
	}

	@Test
	void testBean() {
		assertThat(CategoryTagEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var created = OffsetDateTime.now().minusDays(1);
		final var displayName = "displayName";
		final var id = 1L;
		final var modified = OffsetDateTime.now();
		final var municipalityId = "municipalityId";
		final var name = "name";
		final var namespace = "namespace";
		final var typeTags = List.of(TypeTagEntity.create());

		final var entity = CategoryTagEntity.create()
			.withCreated(created)
			.withDisplayName(displayName)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace)
			.withTypeTags(typeTags);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getTypeTags()).isEqualTo(typeTags);
	}

	@Test
	void testOnCreate() {
		final var entity = CategoryTagEntity.create();
		entity.onCreate();

		Assertions.assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		Assertions.assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void testOnUpdate() {
		final var entity = CategoryTagEntity.create();
		entity.onUpdate();

		Assertions.assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		Assertions.assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CategoryTagEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new CategoryTagEntity()).hasAllNullFieldsOrProperties();
	}
}