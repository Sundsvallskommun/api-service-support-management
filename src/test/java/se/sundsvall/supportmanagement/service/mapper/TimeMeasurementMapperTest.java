package se.sundsvall.supportmanagement.service.mapper;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.integration.db.model.TimeMeasureEntity;

class TimeMeasurementMapperTest {

	@Test
	void toTimeMeasurements() {
		// Arrange
		final var startTime = now();
		final var stopTime = now().plusDays(1);
		final var description = "description";
		final var administrator = "administrator";
		final var status = "status";

		final var timeMeasure = new TimeMeasureEntity()
			.withStartTime(startTime)
			.withStopTime(stopTime)
			.withDescription(description)
			.withStatus(status)
			.withAdministrator(administrator);
		final var timeMeasures = List.of(timeMeasure);

		// Act
		final var result = TimeMeasurementMapper.toTimeMeasurements(timeMeasures);

		// Assert
		assertThat(result).hasSize(1);
		assertThat(result.getFirst()).hasNoNullFieldsOrProperties();
		assertThat(result.getFirst().getStartTime()).isEqualTo(startTime);
		assertThat(result.getFirst().getStopTime()).isEqualTo(stopTime);
		assertThat(result.getFirst().getDescription()).isEqualTo(description);
		assertThat(result.getFirst().getAdministrator()).isEqualTo(administrator);
		assertThat(result.getFirst().getStatus()).isEqualTo(status);

	}

	@Test
	void toTimeMeasurementsEmptyList() {
		// Arrange
		final List<TimeMeasureEntity> timeMeasures = List.of();

		// Act
		final var result = TimeMeasurementMapper.toTimeMeasurements(timeMeasures);

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void toTimeMeasurementsNullList() {

		// Act
		final var result = TimeMeasurementMapper.toTimeMeasurements(null);

		// Assert
		assertThat(result).isEmpty();
	}

}
