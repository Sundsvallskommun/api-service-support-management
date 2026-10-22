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

class InvestigationParameterEntityTest {

	private static final String INVESTIGATION_ENTITY = "investigationEntity";

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(InvestigationParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(INVESTIGATION_ENTITY),
			hasValidBeanEqualsExcluding(INVESTIGATION_ENTITY),
			hasValidBeanToStringExcluding(INVESTIGATION_ENTITY)));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var investigationEntity = InvestigationEntity.create().withId("investigationId");
		final var key = "key";
		final var displayName = "displayName";
		final var parameterGroup = "parameterGroup";
		final var values = List.of("value");

		// Act
		final var result = InvestigationParameterEntity.create()
			.withId(id)
			.withInvestigationEntity(investigationEntity)
			.withKey(key)
			.withDisplayName(displayName)
			.withParameterGroup(parameterGroup)
			.withValues(values);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getInvestigationEntity()).isEqualTo(investigationEntity);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getDisplayName()).isEqualTo(displayName);
		assertThat(result.getParameterGroup()).isEqualTo(parameterGroup);
		assertThat(result.getValues()).isEqualTo(values);
	}

	@Test
	void toStringNamesTheInvestigationById() {
		assertThat(InvestigationParameterEntity.create().withInvestigationEntity(InvestigationEntity.create().withId("investigationId")).toString())
			.contains("investigationEntity=investigationId");
		assertThat(InvestigationParameterEntity.create().toString()).contains("investigationEntity=null");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(InvestigationParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new InvestigationParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
