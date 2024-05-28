package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;

import java.time.OffsetDateTime;
import java.util.Random;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TimeMeasurementEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(TimeMeasurementEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = 1L;
		final var startTime = now();
		final var stopTime = now();
		final var description = "description";
		final var status = "status";
		final var administrator = "administrator";

		// Act
		final var result = TimeMeasurementEntity.create()
			.withId(id)
			.withStartTime(startTime)
			.withStopTime(stopTime)
			.withDescription(description)
			.withStatus(status)
			.withAdministrator(administrator);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getStartTime()).isEqualTo(startTime);
		assertThat(result.getStopTime()).isEqualTo(stopTime);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getStatus()).isEqualTo(status);
		assertThat(result.getAdministrator()).isEqualTo(administrator);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(TimeMeasurementEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new TimeMeasurementEntity()).hasAllNullFieldsOrProperties();
	}

}
