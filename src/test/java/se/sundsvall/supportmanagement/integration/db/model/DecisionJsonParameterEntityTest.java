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

class DecisionJsonParameterEntityTest {

	// The decision holds the parameter in turn, so comparing it would walk back into the decision.
	private static final String OWNER = "decisionEntity";

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(DecisionJsonParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(OWNER),
			hasValidBeanEqualsExcluding(OWNER),
			hasValidBeanToStringExcluding(OWNER)));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var owner = DecisionEntity.create().withId("ownerId");
		final var key = "decisionForm";
		final var schemaId = "schemaId";
		final var value = "{\"answer\":\"pending\"}";
		final var version = 3L;

		// Act
		final var result = DecisionJsonParameterEntity.create()
			.withId(id)
			.withDecisionEntity(owner)
			.withKey(key)
			.withSchemaId(schemaId)
			.withValue(value)
			.withVersion(version);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getDecisionEntity()).isEqualTo(owner);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getSchemaId()).isEqualTo(schemaId);
		assertThat(result.getValue()).isEqualTo(value);
		assertThat(result.getVersion()).isEqualTo(version);
	}

	@Test
	void toStringNamesTheOwnerByItsIdOnly() {
		assertThat(DecisionJsonParameterEntity.create().withDecisionEntity(DecisionEntity.create().withId("ownerId")).toString()).endsWith("decisionEntity=ownerId}");
		assertThat(DecisionJsonParameterEntity.create().toString()).endsWith("decisionEntity=null}");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(DecisionJsonParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new DecisionJsonParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
