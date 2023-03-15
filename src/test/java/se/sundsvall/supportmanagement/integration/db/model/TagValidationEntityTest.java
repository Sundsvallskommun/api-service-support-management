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
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.integration.db.model.enums.TagType;

class TagValidationEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(TagValidationEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var created = OffsetDateTime.now().minusDays(1);
		final var id = 1L;
		final var modified = OffsetDateTime.now();
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var type = TagType.TYPE;
		final var validated = true;

		final var entity = TagValidationEntity.create()
			.withCreated(created)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withType(type)
			.withValidated(validated);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getType()).isEqualTo(type);
		assertThat(entity.isValidated()).isEqualTo(validated);
	}

	@Test
	void testOnCreate() {
		final var entity = TagValidationEntity.create();
		entity.onCreate();

		Assertions.assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		Assertions.assertThat(entity).hasAllNullFieldsOrPropertiesExcept("validated", "created");
	}

	@Test
	void testOnUpdate() {
		final var entity = TagValidationEntity.create();
		entity.onUpdate();

		Assertions.assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		Assertions.assertThat(entity).hasAllNullFieldsOrPropertiesExcept("validated", "modified");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(TagValidationEntity.create()).hasAllNullFieldsOrPropertiesExcept("validated").hasFieldOrPropertyWithValue("validated", false);
		assertThat(new TagValidationEntity()).hasAllNullFieldsOrPropertiesExcept("validated").hasFieldOrPropertyWithValue("validated", false);

	}
}
