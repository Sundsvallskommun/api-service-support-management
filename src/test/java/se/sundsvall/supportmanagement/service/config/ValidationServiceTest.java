package se.sundsvall.supportmanagement.service.config;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.Validation;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.service.mapper.ValidationMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.STATUS;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ValidationRepository validationRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Mock
	private ValidationMapper mapperMock;

	@Captor
	private ArgumentCaptor<ValidationEntity> entityCaptor;

	@InjectMocks
	private ValidationService service;

	@Test
	void findAll() {
		final var entities = List.of(ValidationEntity.create().withType(STATUS).withValidated(true));
		final var validations = List.of(Validation.create().withType(STATUS).withValidated(true));

		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(validationRepositoryMock.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(entities);
		when(mapperMock.toValidations(entities)).thenReturn(validations);

		final var result = service.findAll(NAMESPACE, MUNICIPALITY_ID);

		assertThat(result).isSameAs(validations);
		verify(namespaceConfigRepositoryMock).existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(validationRepositoryMock).findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(mapperMock).toValidations(same(entities));
		verifyNoMoreInteractions(validationRepositoryMock, namespaceConfigRepositoryMock, mapperMock);
	}

	@Test
	void findAllWhenNamespaceConfigIsMissing() {
		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.findAll(NAMESPACE, MUNICIPALITY_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessageContaining("No config found in namespace 'namespace' for municipality '2281'");

		verifyNoInteractions(validationRepositoryMock, mapperMock);
	}

	@Test
	void updateExistingValidation() {
		final var entity = ValidationEntity.create().withId(1L).withType(STATUS).withValidated(false);
		final var request = Validation.create().withValidated(true);
		final var validation = Validation.create().withType(STATUS).withValidated(true);

		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(validationRepositoryMock.findByNamespaceAndMunicipalityIdAndType(NAMESPACE, MUNICIPALITY_ID, STATUS)).thenReturn(Optional.of(entity));
		when(validationRepositoryMock.save(any())).thenReturn(entity);
		when(mapperMock.toValidation(entity)).thenReturn(validation);

		final var result = service.update(NAMESPACE, MUNICIPALITY_ID, STATUS, request);

		assertThat(result).isSameAs(validation);
		verify(validationRepositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getId()).isEqualTo(1L);
		assertThat(entityCaptor.getValue().isValidated()).isTrue();
		verify(mapperMock, never()).toEntity(any(), any(), any(), anyBoolean());
	}

	@Test
	void updateCreatesValidationWhenMissing() {
		final var createdEntity = ValidationEntity.create().withType(STATUS).withValidated(true);
		final var request = Validation.create().withValidated(true);
		final var validation = Validation.create().withType(STATUS).withValidated(true);

		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(validationRepositoryMock.findByNamespaceAndMunicipalityIdAndType(NAMESPACE, MUNICIPALITY_ID, STATUS)).thenReturn(Optional.empty());
		when(mapperMock.toEntity(NAMESPACE, MUNICIPALITY_ID, STATUS, true)).thenReturn(createdEntity);
		when(validationRepositoryMock.save(any())).thenReturn(createdEntity);
		when(mapperMock.toValidation(createdEntity)).thenReturn(validation);

		final var result = service.update(NAMESPACE, MUNICIPALITY_ID, STATUS, request);

		assertThat(result).isSameAs(validation);
		verify(mapperMock).toEntity(NAMESPACE, MUNICIPALITY_ID, STATUS, true);
		verify(validationRepositoryMock).save(same(createdEntity));
	}

	@Test
	void updateWhenNamespaceConfigIsMissing() {
		final var request = Validation.create().withValidated(true);

		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.update(NAMESPACE, MUNICIPALITY_ID, STATUS, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(validationRepositoryMock, mapperMock);
	}
}
