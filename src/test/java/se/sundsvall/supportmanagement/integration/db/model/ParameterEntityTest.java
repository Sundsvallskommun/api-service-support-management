package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ParameterEntityTest {

	@Test
	void testBean() {
		assertThat(ParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = "id";
		final var name = "name";
		final var value = List.of("value");

		final var parameterEntity = ParameterEntity.create()
			.withId(id)
			.withValues(value);

		Assertions.assertThat(parameterEntity).hasNoNullFieldsOrProperties();
		Assertions.assertThat(parameterEntity.getValues()).isEqualTo(value);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		Assertions.assertThat(ParameterEntity.create()).hasAllNullFieldsOrProperties();
		Assertions.assertThat(new ParameterEntity()).hasAllNullFieldsOrProperties();
	}

}
