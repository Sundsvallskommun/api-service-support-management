package se.sundsvall.supportmanagement.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ResolutionCodeRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.model.enums.EntityType.RESOLUTION_CODE;
import static se.sundsvall.supportmanagement.model.enums.ProtectedResource.ERRAND;

@ExtendWith(MockitoExtension.class)
class ResolutionValidatorTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "a0000000-0000-0000-0000-000000000001";

	@Mock
	private MetadataService metadataServiceMock;

	@Mock
	private ResolutionCodeRepository resolutionCodeRepositoryMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@InjectMocks
	private ResolutionValidator validator;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(metadataServiceMock, resolutionCodeRepositoryMock, accessControlServiceMock);
	}

	@Test
	void acceptsActiveCode() {

		// Arrange
		when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(true);
		when(resolutionCodeRepositoryMock.existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "SOLVED")).thenReturn(true);

		// Act & Assert
		assertThat(validator.isValid("SOLVED", NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).isTrue();

		verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
		verify(resolutionCodeRepositoryMock).existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "SOLVED");
		verifyNoInteractions(accessControlServiceMock);
	}

	@Test
	void rejectsUnknownOrDeprecatedCodeOnNewErrand() {

		// Arrange
		when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(true);
		when(resolutionCodeRepositoryMock.existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "LEGACY")).thenReturn(false);

		// Act & Assert
		assertThat(validator.isValid("LEGACY", NAMESPACE, MUNICIPALITY_ID, null)).isFalse();

		verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
		verify(resolutionCodeRepositoryMock).existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "LEGACY");
		verifyNoInteractions(accessControlServiceMock);
	}

	@Test
	void rejectsUnknownOrDeprecatedCodeTheErrandDoesNotHold() {

		// Arrange
		when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(true);
		when(resolutionCodeRepositoryMock.existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "LEGACY")).thenReturn(false);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ERRAND, RW)).thenReturn(ErrandEntity.create().withResolution("SOLVED"));

		// Act & Assert
		assertThat(validator.isValid("LEGACY", NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).isFalse();

		verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
		verify(resolutionCodeRepositoryMock).existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "LEGACY");
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ERRAND, RW);
	}

	/**
	 * An errand that is missing or out of the caller's reach counts as not holding the resolution, so that the verdict
	 * tells nothing about it.
	 */
	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {
		"NOT_FOUND", "FORBIDDEN"
	})
	void rejectsCodeWhenErrandIsMissingOrOutOfReach(final HttpStatus status) {

		// Arrange
		when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(true);
		when(resolutionCodeRepositoryMock.existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "LEGACY")).thenReturn(false);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ERRAND, RW)).thenThrow(Problem.valueOf(status, "refused"));

		// Act & Assert
		assertThat(validator.isValid("LEGACY", NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).isFalse();

		verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
		verify(resolutionCodeRepositoryMock).existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "LEGACY");
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ERRAND, RW);
	}

	@Test
	void acceptsDeprecatedCodeTheErrandAlreadyHolds() {

		// Arrange
		when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(true);
		when(resolutionCodeRepositoryMock.existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "LEGACY")).thenReturn(false);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ERRAND, RW)).thenReturn(ErrandEntity.create().withResolution("LEGACY"));

		// Act & Assert
		assertThat(validator.isValid("LEGACY", NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).isTrue();

		verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
		verify(resolutionCodeRepositoryMock).existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(NAMESPACE, MUNICIPALITY_ID, "LEGACY");
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ERRAND, RW);
	}

	@Test
	void acceptsAnythingWhenValidationIsOff() {

		// Arrange
		when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(false);

		// Act & Assert
		assertThat(validator.isValid("ANYTHING", NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).isTrue();

		verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
		verifyNoInteractions(resolutionCodeRepositoryMock, accessControlServiceMock);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void leavesAbsentOrClearedResolutionAlone(final String resolution) {

		// Act & Assert
		assertThat(validator.isValid(resolution, NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).isTrue();

		verifyNoInteractions(metadataServiceMock, resolutionCodeRepositoryMock, accessControlServiceMock);
	}
}
