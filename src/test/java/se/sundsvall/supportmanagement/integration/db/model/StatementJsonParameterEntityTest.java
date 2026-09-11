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

	// A link carries nothing of its own beyond which two rows it joins, so its id is what identifies it. Comparing the
	// two sides would walk back into the errand they both belong to.
	private static final String[] RELATIONS = {
		"statementEntity", "jsonParameterEntity"
	};

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(StatementJsonParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(RELATIONS),
			hasValidBeanEqualsExcluding(RELATIONS),
			hasValidBeanToStringExcluding(RELATIONS)));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var owner = StatementEntity.create().withId("ownerId");
		final var jsonParameterEntity = JsonParameterEntity.create().withId("parameterId");

		// Act
		final var result = StatementJsonParameterEntity.create()
			.withId(id)
			.withStatementEntity(owner)
			.withJsonParameterEntity(jsonParameterEntity);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getStatementEntity()).isEqualTo(owner);
		assertThat(result.getJsonParameterEntity()).isEqualTo(jsonParameterEntity);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(StatementJsonParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new StatementJsonParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
