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

class InvestigationSectionJsonParameterEntityTest {

	// A link carries nothing of its own beyond which two rows it joins, so its id is what identifies it. Comparing the
	// two sides would walk back into the errand they both belong to.
	private static final String[] RELATIONS = {
		"investigationSectionEntity", "jsonParameterEntity"
	};

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(InvestigationSectionJsonParameterEntity.class, allOf(
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
		final var owner = InvestigationSectionEntity.create().withId("ownerId");
		final var jsonParameterEntity = JsonParameterEntity.create().withId("parameterId");

		// Act
		final var result = InvestigationSectionJsonParameterEntity.create()
			.withId(id)
			.withInvestigationSectionEntity(owner)
			.withJsonParameterEntity(jsonParameterEntity);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getInvestigationSectionEntity()).isEqualTo(owner);
		assertThat(result.getJsonParameterEntity()).isEqualTo(jsonParameterEntity);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(InvestigationSectionJsonParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new InvestigationSectionJsonParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
