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

class ProcessStartRequestTest {

	@Test
	void bean() {
		MatcherAssert.assertThat(ProcessStartRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {
		assertThat(ProcessStartRequest.create().withProcessKey("alkt-tillsyn").getProcessKey()).isEqualTo("alkt-tillsyn");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ProcessStartRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new ProcessStartRequest()).hasAllNullFieldsOrProperties();
	}
}
