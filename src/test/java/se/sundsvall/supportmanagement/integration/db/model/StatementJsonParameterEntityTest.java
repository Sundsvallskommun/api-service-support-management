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

class StatementJsonParameterEntityTest {

	// The statement holds the parameter in turn, so comparing it would walk back into the statement.
	private static final String OWNER = "statementEntity";

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(StatementJsonParameterEntity.class, allOf(
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
		final var owner = StatementEntity.create().withId("ownerId");
		final var key = "responseForm";
		final var schemaId = "schemaId";
		final var value = "{\"answer\":\"pending\"}";
		final var version = 3L;

		// Act
		final var result = StatementJsonParameterEntity.create()
			.withId(id)
			.withStatementEntity(owner)
			.withKey(key)
			.withSchemaId(schemaId)
			.withValue(value)
			.withVersion(version);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getStatementEntity()).isEqualTo(owner);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getSchemaId()).isEqualTo(schemaId);
		assertThat(result.getValue()).isEqualTo(value);
		assertThat(result.getVersion()).isEqualTo(version);
	}

	@Test
	void toStringNamesTheOwnerByItsIdOnly() {
		assertThat(StatementJsonParameterEntity.create().withStatementEntity(StatementEntity.create().withId("ownerId").withTitle("title")))
			.hasToString("StatementJsonParameterEntity{id='null', key='null', schemaId='null', value='null', version=null, statementEntity=ownerId}");
		assertThat(StatementJsonParameterEntity.create().toString()).endsWith("statementEntity=null}");
	}

	/**
	 * Verifies that a parameter of a statement is never equal to one of a decision carrying the same values.
	 */
	@Test
	void isNotEqualToAParameterOfAnotherKindOfOwner() {

		// Arrange
		final var statementParameter = StatementJsonParameterEntity.create().withId("id").withKey("key");
		final var decisionParameter = DecisionJsonParameterEntity.create().withId("id").withKey("key");

		// Act & Assert
		assertThat(statementParameter)
			.isEqualTo(statementParameter)
			.isNotEqualTo(decisionParameter)
			.isNotEqualTo(null);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(StatementJsonParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new StatementJsonParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
