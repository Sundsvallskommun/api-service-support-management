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

class InvestigationSectionParameterEntityTest {

	private static final String INVESTIGATION_SECTION_ENTITY = "investigationSectionEntity";

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(InvestigationSectionParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(INVESTIGATION_SECTION_ENTITY),
			hasValidBeanEqualsExcluding(INVESTIGATION_SECTION_ENTITY),
			hasValidBeanToStringExcluding(INVESTIGATION_SECTION_ENTITY)));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var investigationSectionEntity = InvestigationSectionEntity.create().withId("sectionId");
		final var key = "key";
		final var displayName = "displayName";
		final var parameterGroup = "parameterGroup";
		final var values = List.of("value");

		// Act
		final var result = InvestigationSectionParameterEntity.create()
			.withId(id)
			.withInvestigationSectionEntity(investigationSectionEntity)
			.withKey(key)
			.withDisplayName(displayName)
			.withParameterGroup(parameterGroup)
			.withValues(values);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getInvestigationSectionEntity()).isEqualTo(investigationSectionEntity);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getDisplayName()).isEqualTo(displayName);
		assertThat(result.getParameterGroup()).isEqualTo(parameterGroup);
		assertThat(result.getValues()).isEqualTo(values);
	}

	@Test
	void toStringNamesTheSectionById() {
		assertThat(InvestigationSectionParameterEntity.create().withInvestigationSectionEntity(InvestigationSectionEntity.create().withId("sectionId")).toString())
			.contains("investigationSectionEntity=sectionId");
		assertThat(InvestigationSectionParameterEntity.create().toString()).contains("investigationSectionEntity=null");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(InvestigationSectionParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new InvestigationSectionParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
