package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

class CategoryEntityTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		registerValueGenerator(() -> now().plusDays(random.nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> List.of(TypeEntity.create().withId(random.nextLong())), List.class);
	}

	@Test
	void testBean() {
		assertThat(CategoryEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void equalsAndHashCodeTest() {
		final var category = new CategoryEntity();
		final var category2 = new CategoryEntity();

		assertThat(category).isEqualTo(category2).hasSameHashCodeAs(category2);
	}

	@Test
	void hasValidBuilderMethods() {

		final var created = OffsetDateTime.now().minusDays(1);
		final var displayName = "displayName";
		final var id = "6a5b8c9d-1234-5678-abcd-ef0123456789";
		final var modified = OffsetDateTime.now();
		final var municipalityId = "municipalityId";
		final var name = "name";
		final var namespace = "namespace";
		final var sortOrder = 5;
		final var types = List.of(TypeEntity.create());
		final var deprecated = true;

		final var entity = CategoryEntity.create()
			.withCreated(created)
			.withDeprecated(deprecated)
			.withDisplayName(displayName)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace)
			.withSortOrder(sortOrder)
			.withTypes(types);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.isDeprecated()).isEqualTo(deprecated);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getSortOrder()).isEqualTo(sortOrder);
		assertThat(entity.getTypes()).isEqualTo(types);
	}

	@Test
	void testOnCreate() {
		final var entity = CategoryEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "deprecated");
	}

	@Test
	void testOnUpdate() {
		final var entity = CategoryEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified", "deprecated");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CategoryEntity.create()).hasAllNullFieldsOrPropertiesExcept("deprecated");
		assertThat(new CategoryEntity()).hasAllNullFieldsOrPropertiesExcept("deprecated");
	}
}
