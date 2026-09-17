package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
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
import static org.hamcrest.Matchers.allOf;

class StatementOutcomeEntityTest {

	// Primitive booleans are false rather than null on a fresh bean, so they are named wherever the absence of everything
	// else is asserted.
	private static final String[] PRIMITIVES = {
		"responded", "deprecated"
	};

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(StatementOutcomeEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var name = "NO_RESPONSE";
		final var displayName = "displayName";
		final var sortOrder = 1;
		final var responded = true;
		final var deprecated = true;
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var created = now();
		final var modified = now();

		// Act
		final var result = StatementOutcomeEntity.create()
			.withId(id)
			.withName(name)
			.withDisplayName(displayName)
			.withSortOrder(sortOrder)
			.withResponded(responded)
			.withDeprecated(deprecated)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withCreated(created)
			.withModified(modified);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getName()).isEqualTo(name);
		assertThat(result.getDisplayName()).isEqualTo(displayName);
		assertThat(result.getSortOrder()).isEqualTo(sortOrder);
		assertThat(result.isResponded()).isEqualTo(responded);
		assertThat(result.isDeprecated()).isEqualTo(deprecated);
		assertThat(result.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.getNamespace()).isEqualTo(namespace);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void onCreateSetsCreated() {
		final var entity = StatementOutcomeEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", PRIMITIVES[0], PRIMITIVES[1]);
	}

	@Test
	void onUpdateSetsModified() {
		final var entity = StatementOutcomeEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified", PRIMITIVES[0], PRIMITIVES[1]);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(StatementOutcomeEntity.create()).hasAllNullFieldsOrPropertiesExcept(PRIMITIVES);
		assertThat(new StatementOutcomeEntity()).hasAllNullFieldsOrPropertiesExcept(PRIMITIVES);
	}
}
