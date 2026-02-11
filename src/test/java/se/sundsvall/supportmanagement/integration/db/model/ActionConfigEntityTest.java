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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ActionConfigEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ActionConfigEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var name = "name";
		final var active = true;
		final var displayValue = "displayValue";
		final var conditions = List.of(ActionConfigConditionEntity.create());
		final var parameters = List.of(ActionConfigParameterEntity.create());
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();

		final var entity = ActionConfigEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withName(name)
			.withActive(active)
			.withDisplayValue(displayValue)
			.withConditions(conditions)
			.withParameters(parameters)
			.withCreated(created)
			.withModified(modified);

		assertThat(entity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getActive()).isEqualTo(active);
		assertThat(entity.getDisplayValue()).isEqualTo(displayValue);
		assertThat(entity.getConditions()).isEqualTo(conditions);
		assertThat(entity.getParameters()).isEqualTo(parameters);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testOnCreate() {
		final var entity = ActionConfigEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("active", "conditions", "parameters", "created")
			.satisfies(e -> {
				assertThat(e.getConditions()).isEmpty();
				assertThat(e.getParameters()).isEmpty();
			});
	}

	@Test
	void testOnUpdate() {
		final var entity = ActionConfigEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("active", "conditions", "parameters", "modified")
			.satisfies(e -> {
				assertThat(e.getConditions()).isEmpty();
				assertThat(e.getParameters()).isEmpty();
			});
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ActionConfigEntity.create()).hasAllNullFieldsOrPropertiesExcept("active", "conditions", "parameters")
			.satisfies(e -> {
				assertThat(e.getConditions()).isEmpty();
				assertThat(e.getParameters()).isEmpty();
			});
		assertThat(new ActionConfigEntity()).hasAllNullFieldsOrPropertiesExcept("active", "conditions", "parameters")
			.satisfies(e -> {
				assertThat(e.getConditions()).isEmpty();
				assertThat(e.getParameters()).isEmpty();
			});
	}
}
