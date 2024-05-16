package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;

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

import se.sundsvall.supportmanagement.api.model.parameter.ErrandParameter;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ParameterRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

@ExtendWith(MockitoExtension.class)
class ErrandParameterServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String PARAMETER_NAME = "parameterName";
	private static final String PARAMETER_VALUE = "parameterValue";
	private static final String PARAMETER_ID = "parameterId";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ParameterRepository parameterRepositoryMock;

	@Captor
	private ArgumentCaptor<ErrandEntity> errandEntityArgumentCaptor;

	@Captor
	private ArgumentCaptor<ParameterEntity> parameterEntityArgumentCaptor;

	@InjectMocks
	private ErrandParameterService errandParameterService;

	@Test
	void createErrandParameter() {
		var parameter = ErrandParameter.create()
			.withValue(PARAMETER_VALUE)
			.withName(PARAMETER_NAME);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(ErrandEntity.create()));

		errandParameterService.createErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, parameter);

		verify(errandsRepositoryMock).save(errandEntityArgumentCaptor.capture());

		final var errandEntity = errandEntityArgumentCaptor.getValue();
		assertThat(errandEntity.getParameters()).hasSize(1).allSatisfy(parameterEntity -> {
			assertThat(parameterEntity.getName()).isEqualTo(PARAMETER_NAME);
			assertThat(parameterEntity.getValue()).isEqualTo(PARAMETER_VALUE);
		});
	}

	@Test
	void readErrandParameter() {
		var spy = Mockito.spy(errandParameterService);
		var errand = buildErrandEntity();

		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));

		var result = spy.readErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_ID);

		assertThat(result).satisfies(parameter -> {
			assertThat(parameter.getId()).isEqualTo(PARAMETER_ID);
			assertThat(parameter.getName()).isEqualTo(PARAMETER_NAME);
			assertThat(parameter.getValue()).isEqualTo(PARAMETER_VALUE);
		});
		verify(spy).findExistingErrand(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(spy).findParameterEntityOrElseThrow(errand, PARAMETER_ID);
	}

	@Test
	void findErrandParameters() {
		var spy = Mockito.spy(errandParameterService);
		var errand = buildErrandEntity();
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));

		var result = spy.findErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		assertThat(result.getErrandParameters()).hasSize(1).allSatisfy(parameter -> {
			assertThat(parameter.getId()).isEqualTo(PARAMETER_ID);
			assertThat(parameter.getName()).isEqualTo(PARAMETER_NAME);
			assertThat(parameter.getValue()).isEqualTo(PARAMETER_VALUE);
		});
		verify(spy).findErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		verify(spy).findExistingErrand(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(errandsRepositoryMock, spy);
	}

	@Test
	void updateErrandParameter() {
		var spy = Mockito.spy(errandParameterService);
		var errand = buildErrandEntity();
		var errandParameter = ErrandParameter.create()
			.withValue("anotherValue")
			.withName("anotherName");

		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));
		when(parameterRepositoryMock.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(spy.findParameterEntityOrElseThrow(errand, PARAMETER_ID))
			.thenReturn(ParameterEntity.create()
				.withId(PARAMETER_ID)
				.withName(PARAMETER_NAME)
				.withValue(PARAMETER_VALUE));

		var result = spy.updateErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_ID, errandParameter);

		assertThat(result).satisfies(parameter -> {
			assertThat(parameter.getId()).isEqualTo(PARAMETER_ID);
			assertThat(parameter.getName()).isEqualTo("anotherName");
			assertThat(parameter.getValue()).isEqualTo("anotherValue");
		});
		verify(spy).findExistingErrand(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(spy).findParameterEntityOrElseThrow(errand, PARAMETER_ID);
		verify(spy).updateErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_ID, errandParameter);

		verify(parameterRepositoryMock).save(parameterEntityArgumentCaptor.capture());
		var parameterEntity = parameterEntityArgumentCaptor.getValue();
		assertThat(parameterEntity).satisfies(parameter -> {
			assertThat(parameter.getId()).isEqualTo(PARAMETER_ID);
			assertThat(parameter.getName()).isEqualTo("anotherName");
			assertThat(parameter.getValue()).isEqualTo("anotherValue");
		});

		verifyNoMoreInteractions(errandsRepositoryMock, parameterRepositoryMock, spy);
	}

	@Test
	void deleteErrandParameter() {
		var spy = Mockito.spy(errandParameterService);
		var errand = buildErrandEntity();
		var parameter = errand.getParameters().getFirst();
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));
		when(spy.findParameterEntityOrElseThrow(errand, PARAMETER_ID)).thenReturn(parameter);

		spy.deleteErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_ID);

		verify(errandsRepositoryMock).save(errandEntityArgumentCaptor.capture());
		assertThat(errandEntityArgumentCaptor.getValue().getParameters()).isEmpty();
		verify(parameterRepositoryMock).delete(parameter);
		verifyNoMoreInteractions(errandsRepositoryMock, parameterRepositoryMock);
	}

	@Test
	void findExistingErrand() {
		var errand = buildErrandEntity();
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(errand));

		var result = errandParameterService.findExistingErrand(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);

		assertThat(result).isEqualTo(errand);
		verify(errandsRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(errandsRepositoryMock);
		verifyNoInteractions(parameterRepositoryMock);
	}

	@Test
	void findParameterEntityOrElseThrow_Ok() {
		var parameter = ParameterEntity.create()
			.withId(UUID.randomUUID().toString())
			.withValue(PARAMETER_VALUE)
			.withName(PARAMETER_NAME);
		var errand = ErrandEntity.create()
			.withParameters(List.of(parameter));

		var result = errandParameterService.findParameterEntityOrElseThrow(errand, parameter.getId());

		assertThat(result.getName()).isEqualTo(PARAMETER_NAME);
		assertThat(result.getValue()).isEqualTo(PARAMETER_VALUE);
	}

	@Test
	void findParameterEntityOrElseThrow_Throw() {
		var errand = ErrandEntity.create()
			.withId("errandId")
			.withParameters(List.of());

		final var exception = assertThrows(ThrowableProblem.class, () -> errandParameterService.findParameterEntityOrElseThrow(errand, "parameterId"));

		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: A parameter with id 'parameterId' could not be found in errand with id 'errandId'");

	}


}
