package se.sundsvall.supportmanagement.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ResolutionCodeRepository;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolutionValidatorTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ResolutionCodeRepository resolutionCodeRepositoryMock;

	@InjectMocks
	private ResolutionValidator validator;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(resolutionCodeRepositoryMock);
	}

	@Test
	void acceptsActiveCode() {

		// Arrange
		when(resolutionCodeRepositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(resolutionCodeRepositoryMock.existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "SOLVED")).thenReturn(true);

		// Act & Assert
		assertThatCode(() -> validator.validate("SOLVED", NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();

		verify(resolutionCodeRepositoryMock).existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(resolutionCodeRepositoryMock).existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "SOLVED");
	}

	@Test
	void rejectsUnknownOrDeprecatedCode() {

		// Arrange
		when(resolutionCodeRepositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(resolutionCodeRepositoryMock.existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "LEGACY")).thenReturn(false);

		// Act & Assert
		assertThatThrownBy(() -> validator.validate("LEGACY", NAMESPACE, MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Request: 'LEGACY' is not a valid resolution for namespace 'namespace' and municipality with id '2281'");

		verify(resolutionCodeRepositoryMock).existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(resolutionCodeRepositoryMock).existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "LEGACY");
	}

	@Test
	void acceptsAnythingInNamespaceWithoutCodes() {

		// Arrange
		when(resolutionCodeRepositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		// Act & Assert
		assertThatCode(() -> validator.validate("ANYTHING", NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();

		verify(resolutionCodeRepositoryMock).existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void leavesAbsentOrClearedResolutionAlone(final String resolution) {

		// Act & Assert
		assertThatCode(() -> validator.validate(resolution, NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();

		verifyNoInteractions(resolutionCodeRepositoryMock);
	}
}
