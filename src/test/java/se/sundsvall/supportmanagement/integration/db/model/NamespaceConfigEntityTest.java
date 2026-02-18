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

class NamespaceConfigEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(NamespaceConfigEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = 1L;
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var value1 = NamespaceConfigValueEmbeddable.create();
		final var value2 = NamespaceConfigValueEmbeddable.create();
		final var entity = NamespaceConfigEntity.create()
			.withId(id)
			.withCreated(created)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withValue(value1)
			.withValue(value2);

		assertThat(entity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getValues()).isEqualTo(List.of(value1, value2));
	}

	@Test
	void hasValidBuilderMethodsWithList() {
		final var id = 1L;
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var values = List.of(NamespaceConfigValueEmbeddable.create());
		final var entity = NamespaceConfigEntity.create()
			.withId(id)
			.withCreated(created)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withValues(values);

		assertThat(entity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getValues()).isEqualTo(values);
	}

	@Test
	void testOnCreate() {
		final var entity = NamespaceConfigEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "values").satisfies(e -> assertThat(e.getValues()).isEmpty());
	}

	@Test
	void testOnUpdate() {
		final var entity = NamespaceConfigEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified", "values").satisfies(e -> assertThat(e.getValues()).isEmpty());
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NamespaceConfigEntity.create()).hasAllNullFieldsOrPropertiesExcept("values").satisfies(e -> assertThat(e.getValues()).isEmpty());
		assertThat(new NamespaceConfigEntity()).hasAllNullFieldsOrPropertiesExcept("values").satisfies(e -> assertThat(e.getValues()).isEmpty());
	}
}
