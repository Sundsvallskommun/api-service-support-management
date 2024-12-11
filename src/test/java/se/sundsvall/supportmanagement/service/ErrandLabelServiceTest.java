package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@ExtendWith(MockitoExtension.class)
class ErrandLabelServiceTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "municipalityId";

	private static final String ERRAND_ID = "errandId";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@InjectMocks
	private ErrandLabelService errandLabelService;

	@Test
	void getErrandLabels() {
		// Arrange
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(new ErrandEntity().withLabels(List.of("label1", "label2"))));

		// Act
		final List<String> labels = errandLabelService.getErrandLabels(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assertions
		assertThat(labels).containsExactly("label1", "label2");

		// Assert
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void createErrandLabels() {
		// Arrange
		final var errandEntity = new ErrandEntity();
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errandEntity));

		// Act
		errandLabelService.createErrandLabels(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, List.of("label1", "label2"));

		// Assert
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).save(errandEntity);
	}

	@Test
	void deleteErrandLabel() {
		// Arrange
		final var errandEntity = new ErrandEntity();
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errandEntity));

		// Act
		errandLabelService.deleteErrandLabel(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assert
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).save(errandEntity);
	}

	@Test
	void updateErrandLabel() {
		// Arrange
		final var errandEntity = new ErrandEntity();
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errandEntity));

		// Act
		errandLabelService.updateErrandLabel(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, List.of("label1", "label2"));

		// Assert
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).save(errandEntity);
	}

	@Test
	void getErrandEntityThrowsExceptionWhenNotFound() {
		// Arrange
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(RuntimeException.class, () -> errandLabelService.getErrandLabels(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

}
