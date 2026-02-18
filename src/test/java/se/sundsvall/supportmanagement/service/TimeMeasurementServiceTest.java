package se.sundsvall.supportmanagement.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.TimeMeasurementEntity;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeMeasurementServiceTest {

	@Mock
	private AccessControlService accessControlServiceMock;

	@InjectMocks
	private TimeMeasurementService timeMeasurementService;

	@Test
	void getErrandTimeMeasurements() {

		// Arrange
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var errandId = "errandId";

		final var startTime = OffsetDateTime.now();
		final var stopTime = OffsetDateTime.now().plusHours(1);
		final var description = "description";
		final var administrator = "administrator";
		final var status = "SUSPENDED";

		final var timeMeasurements = List.of(new TimeMeasurementEntity()
			.withStartTime(startTime)
			.withStopTime(stopTime)
			.withDescription(description)
			.withAdministrator(administrator)
			.withStatus(status));

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(ErrandEntity.create().withTimeMeasures(timeMeasurements));

		// Act
		final var result = timeMeasurementService.getErrandTimeMeasurements(namespace, municipalityId, errandId);

		// Assert
		verify(accessControlServiceMock).getErrand(namespace, municipalityId, errandId, false, R, RW);
		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getStartTime()).isEqualTo(startTime);
		assertThat(result.getFirst().getStopTime()).isEqualTo(stopTime);
		assertThat(result.getFirst().getDescription()).isEqualTo(description);
		assertThat(result.getFirst().getAdministrator()).isEqualTo(administrator);
		assertThat(result.getFirst().getStatus()).isEqualTo(status);
	}

	@Test
	void getErrandTimeMeasurementsEmptyList() {
		// Arrange
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var errandId = "errandId";

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(ErrandEntity.create());

		// Act
		final var result = timeMeasurementService.getErrandTimeMeasurements(namespace, municipalityId, errandId);

		// Assert
		verify(accessControlServiceMock).getErrand(namespace, municipalityId, errandId, false, R, RW);
		assertThat(result).isEmpty();
	}
}
