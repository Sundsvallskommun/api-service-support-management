package se.sundsvall.supportmanagement.integration.db.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;

class DecisionTermEntityTest {

	private static final String DECISION_ENTITY = "decisionEntity";

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(DecisionTermEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(DECISION_ENTITY),
			hasValidBeanEqualsExcluding(DECISION_ENTITY),
			hasValidBeanToStringExcluding(DECISION_ENTITY)));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var decisionEntity = DecisionEntity.create().withId("decisionId");
		final var sortOrder = 1;
		final var category = "category";
		final var text = "text";

		// Act
		final var result = DecisionTermEntity.create()
			.withId(id)
			.withDecisionEntity(decisionEntity)
			.withSortOrder(sortOrder)
			.withCategory(category)
			.withText(text);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getDecisionEntity()).isEqualTo(decisionEntity);
		assertThat(result.getSortOrder()).isEqualTo(sortOrder);
		assertThat(result.getCategory()).isEqualTo(category);
		assertThat(result.getText()).isEqualTo(text);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(DecisionTermEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new DecisionTermEntity()).hasAllNullFieldsOrProperties();
	}
}
