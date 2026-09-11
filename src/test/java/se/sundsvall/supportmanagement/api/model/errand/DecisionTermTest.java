package se.sundsvall.supportmanagement.api.model.errand;

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

class DecisionTermTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);

	}

	@Test
	void bean() {
		MatcherAssert.assertThat(DecisionTerm.class, allOf(
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
		final var sortOrder = 1;
		final var category = "category";
		final var text = "text";

		// Act
		final var result = DecisionTerm.create()
			.withId(id)
			.withSortOrder(sortOrder)
			.withCategory(category)
			.withText(text);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getSortOrder()).isEqualTo(sortOrder);
		assertThat(result.getCategory()).isEqualTo(category);
		assertThat(result.getText()).isEqualTo(text);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(DecisionTerm.create()).hasAllNullFieldsOrProperties();
		assertThat(new DecisionTerm()).hasAllNullFieldsOrProperties();
	}
}
