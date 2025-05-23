package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LabelEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LabelEntity.create().withId(new Random().nextLong()), LabelEntity.class);
	}

	@Test
	void testBean() {
		assertThat(LabelEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToStringExcluding("parent")));
	}

	@Test
	void hasValidBuilderMethods() {

		final var created = OffsetDateTime.now().minusDays(1);
		final var id = 1L;
		final var modified = OffsetDateTime.now();
		final var municipalityId = "municipalityId";
		final var jsonStructure = "jsonStructure";
		final var namespace = "namespace";

		final var entity = LabelEntity.create()
			.withCreated(created)
			.withJsonStructure(jsonStructure)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getJsonStructure()).isEqualTo(jsonStructure);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
	}

	@Test
	void testOnCreate() {
		final var entity = LabelEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void testOnUpdate() {
		final var entity = LabelEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LabelEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new LabelEntity()).hasAllNullFieldsOrProperties();
	}
}
