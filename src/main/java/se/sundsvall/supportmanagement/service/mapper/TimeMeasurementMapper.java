package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.errand.TimeMeasurement;
import se.sundsvall.supportmanagement.integration.db.model.TimeMeasurementEntity;

import static java.util.Collections.emptyList;

public final class TimeMeasurementMapper {

	private TimeMeasurementMapper() {
		// Prevent instantiation
	}

	public static List<TimeMeasurement> toTimeMeasurements(final List<TimeMeasurementEntity> timeMeasures) {

		return Optional.ofNullable(timeMeasures)
			.orElse(emptyList()).stream()
			.map(entity -> new TimeMeasurement()
				.withStartTime(entity.getStartTime())
				.withStopTime(entity.getStopTime())
				.withDescription(entity.getDescription())
				.withStatus(entity.getStatus())
				.withAdministrator(entity.getAdministrator()))
			.toList();
	}

}
