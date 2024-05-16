package se.sundsvall.supportmanagement.api.model.parameter;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ErrandParameterTest {

	@Test
	void testBean() {
		assertThat(ErrandParameter.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = "id";
		final var name = "name";
		final var value = "value";

		final var errandParameter = ErrandParameter.create()
			.withId(id)
			.withName(name)
			.withValue(value);

		Assertions.assertThat(errandParameter).hasNoNullFieldsOrProperties();
		Assertions.assertThat(errandParameter.getName()).isEqualTo(name);
		Assertions.assertThat(errandParameter.getValue()).isEqualTo(value);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		Assertions.assertThat(ErrandParameter.create()).hasAllNullFieldsOrProperties();
		Assertions.assertThat(new ErrandParameter()).hasAllNullFieldsOrProperties();
	}
	
}
