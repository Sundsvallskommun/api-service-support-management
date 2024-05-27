package se.sundsvall.supportmanagement.service.scheduler.supensions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.ErrandService;

@ExtendWith(MockitoExtension.class)
class SuspensionWorkerTest {

	@Mock
	private ErrandsRepository errandsRepository;

	@Mock
	private ErrandService errandService;

	@InjectMocks
	private SuspensionWorker suspensionWorker;

	@Captor
	private ArgumentCaptor<Errand> errandCaptor;

	@Test
	void cleanUpSuspensions() {

		// Arrange
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		// When the previous time measurement is stopped and the current time measurement is started
		final var previousStatus = "previousStatus";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(namespace)
			.withId("id")
			.withMunicipalityId(municipalityId)
			.withSuspendedFrom(OffsetDateTime.now().minusDays(1))
			.withSuspendedTo(OffsetDateTime.now().minusHours(1))
			.withPreviousStatus(previousStatus)
			.withMunicipalityId(municipalityId);

		when(errandsRepository.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));
		// Act
		suspensionWorker.cleanUpSuspensions();

		// Assert
		verify(errandsRepository).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verify(errandService).updateErrand(eq(namespace), eq(municipalityId), eq(errandEntity.getId()), errandCaptor.capture());
		final var errand = errandCaptor.getValue();
		assertThat(errand).isNotNull();
		assertThat(errand.getSuspension()).isNotNull();
		assertThat(errand.getStatus()).isEqualTo(previousStatus);

		verifyNoMoreInteractions(errandsRepository, errandService);
	}

	@Test
	void cleanUpSuspensionsNoSuspensions() {

		// Arrange
		when(errandsRepository.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of());
		// Act
		suspensionWorker.cleanUpSuspensions();

		// Assert
		verify(errandsRepository).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verifyNoMoreInteractions(errandsRepository, errandService);
	}

	@Test
	void cleanUpSuspensionsNoTimeMeasurements() {

		// Arrange

		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(namespace)
			.withId("id")
			.withMunicipalityId(municipalityId)
			.withSuspendedFrom(OffsetDateTime.now().minusDays(1))
			.withSuspendedTo(OffsetDateTime.now().minusHours(1))
			.withMunicipalityId(municipalityId);

		when(errandsRepository.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));

		// Act
		suspensionWorker.cleanUpSuspensions();

		// Assert
		verify(errandsRepository).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verify(errandService).updateErrand(eq(namespace), eq(municipalityId), eq(errandEntity.getId()), errandCaptor.capture());
		final var errand = errandCaptor.getValue();
		assertThat(errand).isNotNull();
		assertThat(errand.getSuspension()).isNotNull();
		assertThat(errand.getStatus()).isNull();

		verifyNoMoreInteractions(errandsRepository, errandService);
	}

}
