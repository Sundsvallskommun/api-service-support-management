package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

class ParameterEntityTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(ParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToStringExcluding("errandEntity")));

	}

	@Test
	void hasValidBuilderMethods() {

		final var id = "id";
		final var value = List.of("value");
		final var errandEntity = ErrandEntity.create().withId("id");

		final var parameterEntity = ParameterEntity.create()
			.withId(id)
			.withErrandEntity(errandEntity)
			.withValues(value);

		assertThat(parameterEntity).hasNoNullFieldsOrProperties();
		assertThat(parameterEntity.getValues()).isEqualTo(value);
		assertThat(parameterEntity.getId()).isEqualTo(id);
		assertThat(parameterEntity.getErrandEntity()).isEqualTo(errandEntity);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ParameterEntity()).hasAllNullFieldsOrProperties();
	}

}
