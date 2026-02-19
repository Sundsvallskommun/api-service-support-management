package se.sundsvall.supportmanagement.api.model.errand;

import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class TimeMeasurementTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void bean() {
		MatcherAssert.assertThat(TimeMeasurement.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {

		// Arrange
		final var startTime = now();
		final var stopTime = now().plusHours(1);
		final var description = "description";
		final var administrator = "administrator";
		final var status = "status";

		// Act
		final var timeMeasurement = TimeMeasurement.create()
			.withStartTime(startTime)
			.withStopTime(stopTime)
			.withDescription(description)
			.withAdministrator(administrator)
			.withStatus(status);

		// Assert
		assertThat(timeMeasurement.getStartTime()).isEqualTo(startTime);
		assertThat(timeMeasurement.getStopTime()).isEqualTo(stopTime);
		assertThat(timeMeasurement.getDescription()).isEqualTo(description);
		assertThat(timeMeasurement.getAdministrator()).isEqualTo(administrator);
		assertThat(timeMeasurement.getStatus()).isEqualTo(status);

	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(TimeMeasurement.create()).hasAllNullFieldsOrProperties();
		assertThat(new TimeMeasurement()).hasAllNullFieldsOrProperties();
	}

}
