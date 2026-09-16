package se.sundsvall.supportmanagement.api.model.metadata;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class StatementOutcomeTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void bean() {
		MatcherAssert.assertThat(StatementOutcome.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {

		// Arrange
		final var id = "id";
		final var name = "NO_RESPONSE";
		final var displayName = "displayName";
		final var sortOrder = 1;
		final var responded = false;
		final var deprecated = true;
		final var created = now();
		final var modified = now();

		// Act
		final var result = StatementOutcome.create()
			.withId(id)
			.withName(name)
			.withDisplayName(displayName)
			.withSortOrder(sortOrder)
			.withResponded(responded)
			.withDeprecated(deprecated)
			.withCreated(created)
			.withModified(modified);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getName()).isEqualTo(name);
		assertThat(result.getDisplayName()).isEqualTo(displayName);
		assertThat(result.getSortOrder()).isEqualTo(sortOrder);
		assertThat(result.getResponded()).isEqualTo(responded);
		assertThat(result.getDeprecated()).isEqualTo(deprecated);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(StatementOutcome.create()).hasAllNullFieldsOrProperties();
		assertThat(new StatementOutcome()).hasAllNullFieldsOrProperties();
	}
}
