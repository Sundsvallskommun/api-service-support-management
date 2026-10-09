package se.sundsvall.supportmanagement.api.model.process;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class ProcessErrorTest {

	@Test
	void bean() {
		MatcherAssert.assertThat(ProcessError.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {
		final var error = ProcessError.create()
			.withCode("INCIDENT")
			.withMessage("Timeout against Employee after 30 s");

		assertThat(error.getCode()).isEqualTo("INCIDENT");
		assertThat(error.getMessage()).isEqualTo("Timeout against Employee after 30 s");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ProcessError.create()).hasAllNullFieldsOrProperties();
		assertThat(new ProcessError()).hasAllNullFieldsOrProperties();
	}
}
