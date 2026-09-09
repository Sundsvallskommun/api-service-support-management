package se.sundsvall.supportmanagement.api.model.process;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static se.sundsvall.supportmanagement.api.model.process.ProcessStartability.AVAILABLE;

class ErrandProcessesTest {

	@Test
	void bean() {
		MatcherAssert.assertThat(ErrandProcesses.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {
		final var startable = ProcessStartable.create().withStatus(AVAILABLE);
		final var process = ErrandProcess.create().withId("id");

		final var processes = ErrandProcesses.create()
			.withStartable(startable)
			.withProcesses(List.of(process));

		assertThat(processes.getStartable()).isEqualTo(startable);
		assertThat(processes.getProcesses()).containsExactly(process);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ErrandProcesses.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandProcesses()).hasAllNullFieldsOrProperties();
	}
}
