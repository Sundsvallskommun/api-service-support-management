package se.sundsvall.supportmanagement.api.model.parameter;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ErrandParametersTest {

	@Test
	void testBean() {
		assertThat(ErrandParameters.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void hasValidBuilderMethods() {

		var errandParameter = ErrandParameter.create();
		final var errandParameters = ErrandParameters.create()
			.withErrandParameters(List.of(errandParameter));

		Assertions.assertThat(errandParameters).hasNoNullFieldsOrProperties();
		Assertions.assertThat(errandParameters.getErrandParameters()).hasSize(1).containsExactly(errandParameter);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		Assertions.assertThat(ErrandParameters.create()).hasAllNullFieldsOrProperties();
		Assertions.assertThat(new ErrandParameters()).hasAllNullFieldsOrProperties();
	}

}
