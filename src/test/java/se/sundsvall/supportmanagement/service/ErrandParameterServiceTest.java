package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

@ExtendWith(MockitoExtension.class)
class ErrandParameterServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String PARAMETER_KEY = "parameterKey";
	private static final String PARAMETER_VALUE = "parameterValue";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Captor
	private ArgumentCaptor<ErrandEntity> errandEntityArgumentCaptor;

	@InjectMocks
	private ErrandParameterService errandParameterService;

	@Test
	void updateErrandParameters() {

		// Arrange
		final var parameters = List.of(Parameter.create().withKey(PARAMETER_KEY).withValues(List.of(PARAMETER_VALUE)));
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(ErrandEntity.create().withParameters(new ArrayList<>())));
		when(errandsRepositoryMock.save(any(ErrandEntity.class))).thenReturn(ErrandEntity.create());

		// Act
		errandParameterService.updateErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, parameters);

		// Assert
		verify(errandsRepositoryMock).save(errandEntityArgumentCaptor.capture());
		final var errandEntity = errandEntityArgumentCaptor.getValue();

		assertThat(errandEntity.getParameters()).hasSize(1).allSatisfy(parameterEntity -> {
			assertThat(parameterEntity.getKey()).isEqualTo(PARAMETER_KEY);
			assertThat(parameterEntity.getValues()).containsExactly(PARAMETER_VALUE);
		});
	}

	@Test
	void readErrandParameter() {

		// Arrange
		final var spy = Mockito.spy(errandParameterService);
		final var errand = buildErrandEntity();
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));

		// Act
		final var result = spy.readErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_KEY);

		// Assert
		assertThat(result).hasSize(1).containsExactly(PARAMETER_VALUE);
		verify(spy).findExistingErrand(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(spy).findParameterEntityOrElseThrow(errand, PARAMETER_KEY);
	}

	@Test
	void findErrandParameters() {

		// Arrange
		final var spy = Mockito.spy(errandParameterService);
		final var errand = buildErrandEntity();
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));

		// Act
		final var result = spy.findErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assert
		assertThat(result).hasSize(1).isEqualTo(List.of(Parameter.create().withKey(PARAMETER_KEY).withValues(List.of(PARAMETER_VALUE))));
		verify(spy).findErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		verify(spy).findExistingErrand(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(errandsRepositoryMock, spy);
	}

	@Test
	void updateErrandParameter() {

		// Arrange
		final var spy = Mockito.spy(errandParameterService);
		final var errand = buildErrandEntity();
		final var errandParameterValues = List.of("anotherValue");

		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));
		when(errandsRepositoryMock.save(errand)).thenReturn(errand);

		// Act
		final var result = spy.updateErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_KEY, errandParameterValues);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getValues()).isEqualTo(List.of("anotherValue"));
		verify(spy).findExistingErrand(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(spy).updateErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_KEY, errandParameterValues);
		verifyNoMoreInteractions(errandsRepositoryMock, spy);
	}

	@Test
	void deleteErrandParameter() {
		// Arrange
		final var spy = Mockito.spy(errandParameterService);
		final var errand = buildErrandEntity().withParameters(new ArrayList<>(List.of(ParameterEntity.create().withKey(PARAMETER_KEY).withValues(List.of(PARAMETER_VALUE)))));
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));

		// Act
		spy.deleteErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_KEY);

		// Assert
		verify(errandsRepositoryMock).save(errandEntityArgumentCaptor.capture());
		assertThat(errandEntityArgumentCaptor.getValue().getParameters()).isEmpty();
		verifyNoMoreInteractions(errandsRepositoryMock);
	}

	@Test
	void findExistingErrand() {

		// Arrange
		final var errand = buildErrandEntity();
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));

		// Act
		final var result = errandParameterService.findExistingErrand(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isEqualTo(errand);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(errandsRepositoryMock);
	}

	@Test
	void findParameterEntityOrElseThrow_Ok() {

		// Arrange
		final var parameter = ParameterEntity.create()
			.withId(UUID.randomUUID().toString())
			.withKey(PARAMETER_KEY)
			.withValues(List.of(PARAMETER_VALUE));

		final var errand = ErrandEntity.create().withParameters(List.of(parameter));

		// Act
		final var result = errandParameterService.findParameterEntityOrElseThrow(errand, PARAMETER_KEY);

		// Assert
		assertThat(result).isEqualTo(parameter.getValues());
	}

	@Test
	void findParameterEntityOrElseThrow_Throw() {

		// Arrange
		final var errand = ErrandEntity.create()
			.withId("errandId")
			.withParameters(List.of());

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> errandParameterService.findParameterEntityOrElseThrow(errand, "parameterId"));

		// Assert
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: A parameter with key 'parameterId' could not be found in errand with id 'errandId'");

	}

}
