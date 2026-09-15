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

class InvestigationJsonParameterEntityTest {

	// The investigation holds the parameter in turn, so comparing it would walk back into the investigation.
	private static final String OWNER = "investigationEntity";

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(InvestigationJsonParameterEntity.class, allOf(
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
		final var owner = InvestigationEntity.create().withId("ownerId");
		final var key = "investigationForm";
		final var schemaId = "schemaId";
		final var value = "{\"answer\":\"pending\"}";
		final var version = 3L;

		// Act
		final var result = InvestigationJsonParameterEntity.create()
			.withId(id)
			.withInvestigationEntity(owner)
			.withKey(key)
			.withSchemaId(schemaId)
			.withValue(value)
			.withVersion(version);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getInvestigationEntity()).isEqualTo(owner);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getSchemaId()).isEqualTo(schemaId);
		assertThat(result.getValue()).isEqualTo(value);
		assertThat(result.getVersion()).isEqualTo(version);
	}

	@Test
	void toStringNamesTheOwnerByItsIdOnly() {
		assertThat(InvestigationJsonParameterEntity.create().withInvestigationEntity(InvestigationEntity.create().withId("ownerId")).toString()).endsWith("investigationEntity=ownerId}");
		assertThat(InvestigationJsonParameterEntity.create().toString()).endsWith("investigationEntity=null}");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(InvestigationJsonParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new InvestigationJsonParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
