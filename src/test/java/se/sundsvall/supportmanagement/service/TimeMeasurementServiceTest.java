package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.TimeMeasureEntity;

@ExtendWith(MockitoExtension.class)
class TimeMeasurementServiceTest {

	@Mock
	private ErrandsRepository repository;

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

		final var timeMeasurements = List.of(new TimeMeasureEntity()
			.withStartTime(startTime)
			.withStopTime(stopTime)
			.withDescription(description)
			.withAdministrator(administrator)
			.withStatus(status));

		when(repository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)).thenReturn(Optional.of(new ErrandEntity().withTimeMeasures(timeMeasurements)));

		// Act
		final var result = timeMeasurementService.getErrandTimeMeasurements(namespace, municipalityId, errandId);

		// Assert
		verify(repository).findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId);
		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getStartTime()).isEqualTo(startTime);
		assertThat(result.getFirst().getStopTime()).isEqualTo(stopTime);
		assertThat(result.getFirst().getDescription()).isEqualTo(description);
		assertThat(result.getFirst().getAdministrator()).isEqualTo(administrator);
		assertThat(result.getFirst().getStatus()).isEqualTo(status);
	}

	@Test
	void getErrandTimeMeasurementsErrandNotFound() {

		// Arrange
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var errandId = "errandId";

		when(repository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> timeMeasurementService.getErrandTimeMeasurements(namespace, municipalityId, errandId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: An errand with id '" + errandId + "' could not be found in namespace '" + namespace + "' for municipality with id '" + municipalityId + "'");
	}

	@Test
	void getErrandTimeMeasurementsEmptyList() {
		// Arrange
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var errandId = "errandId";

		when(repository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)).thenReturn(Optional.of(new ErrandEntity()));

		// Act
		final var result = timeMeasurementService.getErrandTimeMeasurements(namespace, municipalityId, errandId);

		// Assert
		verify(repository).findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId);
		assertThat(result).isEmpty();
	}

}
