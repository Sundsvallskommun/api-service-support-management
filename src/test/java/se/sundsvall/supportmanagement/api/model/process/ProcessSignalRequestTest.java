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

class ProcessSignalRequestTest {

	@Test
	void bean() {
		MatcherAssert.assertThat(ProcessSignalRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {
		assertThat(ProcessSignalRequest.create().withSignal("granskning-godkand").getSignal()).isEqualTo("granskning-godkand");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ProcessSignalRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new ProcessSignalRequest()).hasAllNullFieldsOrProperties();
	}
}
