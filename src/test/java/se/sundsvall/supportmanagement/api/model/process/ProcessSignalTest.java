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

class ProcessSignalTest {

	@Test
	void bean() {
		MatcherAssert.assertThat(ProcessSignal.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {
		final var signal = ProcessSignal.create()
			.withName("granskning-godkand")
			.withLabel("Godkänn granskning");

		assertThat(signal.getName()).isEqualTo("granskning-godkand");
		assertThat(signal.getLabel()).isEqualTo("Godkänn granskning");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ProcessSignal.create()).hasAllNullFieldsOrProperties();
		assertThat(new ProcessSignal()).hasAllNullFieldsOrProperties();
	}
}
