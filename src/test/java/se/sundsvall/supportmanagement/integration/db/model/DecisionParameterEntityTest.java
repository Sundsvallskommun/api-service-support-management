package se.sundsvall.supportmanagement.integration.db.model;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;

class DecisionParameterEntityTest {

	private static final String DECISION_ENTITY = "decisionEntity";

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(DecisionParameterEntity.class, allOf(
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
		final var key = "key";
		final var displayName = "displayName";
		final var parameterGroup = "parameterGroup";
		final var values = List.of("value");

		// Act
		final var result = DecisionParameterEntity.create()
			.withId(id)
			.withDecisionEntity(decisionEntity)
			.withKey(key)
			.withDisplayName(displayName)
			.withParameterGroup(parameterGroup)
			.withValues(values);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getDecisionEntity()).isEqualTo(decisionEntity);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getDisplayName()).isEqualTo(displayName);
		assertThat(result.getParameterGroup()).isEqualTo(parameterGroup);
		assertThat(result.getValues()).isEqualTo(values);
	}

	@Test
	void toStringNamesTheDecisionById() {
		assertThat(DecisionParameterEntity.create().withDecisionEntity(DecisionEntity.create().withId("decisionId")).toString()).contains("decisionEntity=decisionId");
		assertThat(DecisionParameterEntity.create().toString()).contains("decisionEntity=null");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(DecisionParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new DecisionParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
