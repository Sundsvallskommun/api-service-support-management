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

	// The section holds the parameter in turn, so comparing it would walk back into the section.
	private static final String OWNER = "investigationSectionEntity";

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(InvestigationSectionJsonParameterEntity.class, allOf(
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
		final var owner = InvestigationSectionEntity.create().withId("ownerId");
		final var key = "sectionForm";
		final var schemaId = "schemaId";
		final var value = "{\"answer\":\"pending\"}";
		final var version = 3L;

		// Act
		final var result = InvestigationSectionJsonParameterEntity.create()
			.withId(id)
			.withInvestigationSectionEntity(owner)
			.withKey(key)
			.withSchemaId(schemaId)
			.withValue(value)
			.withVersion(version);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getInvestigationSectionEntity()).isEqualTo(owner);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getSchemaId()).isEqualTo(schemaId);
		assertThat(result.getValue()).isEqualTo(value);
		assertThat(result.getVersion()).isEqualTo(version);
	}

	@Test
	void toStringNamesTheOwnerByItsIdOnly() {
		assertThat(InvestigationSectionJsonParameterEntity.create().withInvestigationSectionEntity(InvestigationSectionEntity.create().withId("ownerId")).toString())
			.endsWith("investigationSectionEntity=ownerId}");
		assertThat(InvestigationSectionJsonParameterEntity.create().toString()).endsWith("investigationSectionEntity=null}");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(InvestigationSectionJsonParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new InvestigationSectionJsonParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
