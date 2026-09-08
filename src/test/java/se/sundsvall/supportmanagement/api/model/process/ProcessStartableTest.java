package se.sundsvall.supportmanagement.api.model.process;

import java.util.List;
import java.util.stream.Stream;
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

class ProcessStartableTest {

	@Test
	void bean() {
		MatcherAssert.assertThat(ProcessStartable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {
		final var startable = ProcessStartable.create()
			.withStatus(AVAILABLE)
			.withProcessKeys(List.of("alkt-tillsyn"));

		assertThat(startable.getStatus()).isEqualTo("AVAILABLE");
		assertThat(startable.getProcessKeys()).containsExactly("alkt-tillsyn");
	}

	/**
	 * The status leaves this service as a string so that a value added later does not break a client that generated an
	 * enum from the schema, while the builder still takes the enum so nothing outside the set can be published.
	 */
	@Test
	void theStatusIsCarriedAsAStringOfTheEnumName() {
		assertThat(ProcessStartable.create().withStatus(null).getStatus()).isNull();

		assertThat(Stream.of(ProcessStartability.values())
			.map(value -> ProcessStartable.create().withStatus(value).getStatus()))
			.containsExactly(Stream.of(ProcessStartability.values()).map(Enum::name).toArray(String[]::new));
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ProcessStartable.create()).hasAllNullFieldsOrProperties();
		assertThat(new ProcessStartable()).hasAllNullFieldsOrProperties();
	}
}
