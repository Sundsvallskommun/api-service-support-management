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
import static org.hamcrest.core.AllOf.allOf;

class ParameterEntityTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(ParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("errandEntity"),
			hasValidBeanEqualsExcluding("errandEntity"),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = "id";
		final var displayName = "displayName";
		final var parameterGroup = "parameterGroup";
		final var key = "key";
		final var values = List.of("value");
		final var errandEntity = ErrandEntity.create().withId("id");

		final var parameterEntity = ParameterEntity.create()
			.withId(id)
			.withDisplayName(displayName)
			.withParameterGroup(parameterGroup)
			.withErrandEntity(errandEntity)
			.withKey(key)
			.withValues(values);

		assertThat(parameterEntity).hasNoNullFieldsOrProperties();
		assertThat(parameterEntity.getKey()).isEqualTo(key);
		assertThat(parameterEntity.getDisplayName()).isEqualTo(displayName);
		assertThat(parameterEntity.getParameterGroup()).isEqualTo(parameterGroup);
		assertThat(parameterEntity.getValues()).isEqualTo(values);
		assertThat(parameterEntity.getId()).isEqualTo(id);
		assertThat(parameterEntity.getErrandEntity()).isEqualTo(errandEntity);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ParameterEntity()).hasAllNullFieldsOrProperties();
	}
}
