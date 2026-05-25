package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
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

class StatusEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(StatusEntity.class, allOf(
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
		final var externalDisplayName = "externalDisplayName";
		final var id = "6a5b8c9d-1234-5678-abcd-ef0123456789";
		final var modified = OffsetDateTime.now();
		final var municipalityId = "municipalityId";
		final var name = "name";
		final var namespace = "namespace";
		final var sortOrder = 5;
		final var deprecated = true;

		final var entity = StatusEntity.create()
			.withCreated(created)
			.withDeprecated(deprecated)
			.withDisplayName(displayName)
			.withExternalDisplayName(externalDisplayName)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace)
			.withSortOrder(sortOrder);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.isDeprecated()).isEqualTo(deprecated);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getExternalDisplayName()).isEqualTo(externalDisplayName);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getSortOrder()).isEqualTo(sortOrder);
	}

	@Test
	void testOnCreate() {
		final var entity = StatusEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "deprecated");
	}

	@Test
	void testOnUpdate() {
		final var entity = StatusEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified", "deprecated");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(StatusEntity.create()).hasAllNullFieldsOrPropertiesExcept("deprecated");
		assertThat(new StatusEntity()).hasAllNullFieldsOrPropertiesExcept("deprecated");
	}
}
