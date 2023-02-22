package se.sundsvall.supportmanagement.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrand;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.zalando.problem.ThrowableProblem;

import com.turkraft.springfilter.boot.FilterSpecification;

import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@ExtendWith(MockitoExtension.class)
class ErrandServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ID = "errandId";

	@Mock
	private ErrandsRepository repositoryMock;

	@InjectMocks
	private ErrandService service;

	@Captor
	private ArgumentCaptor<Specification<ErrandEntity>> specificationCaptor;

	@Test
	void createErrand() {
		// Setup
		final var errand = buildErrand();

		// Mock
		when(repositoryMock.save(any(ErrandEntity.class))).thenReturn(ErrandEntity.create().withId(ID));

		// Call
		final var result = service.createErrand(NAMESPACE, MUNICIPALITY_ID, errand);

		// Assertions and verifications
		assertThat(result).isEqualTo(ID);

		verify(repositoryMock).save(any(ErrandEntity.class));
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void findErrandWithMatches() {
		// Setup
		final Specification<ErrandEntity> filter = new FilterSpecification<>("id: 'uuid'");
		final var sort = Sort.by(DESC, "attribute.1", "attribute.2");
		final Pageable pageable = PageRequest.of(1, 2, sort);

		// Mock
		when(repositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(buildErrandEntity(), buildErrandEntity()), pageable, 2L));
		when(repositoryMock.count(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(10L);

		// Call
		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, filter, pageable);

		// Assertions and verifications
		assertThat(matches.getContent()).isNotEmpty().hasSize(2);
		assertThat(matches.getNumberOfElements()).isEqualTo(2);
		assertThat(matches.getTotalElements()).isEqualTo(10);
		assertThat(matches.getTotalPages()).isEqualTo(5);
		assertThat(matches.getPageable()).usingRecursiveComparison().isEqualTo(pageable);
		assertThat(matches.getSort()).usingRecursiveComparison().isEqualTo(sort);

		verify(repositoryMock).findAll(specificationCaptor.capture(), eq(pageable));
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));

		verify(repositoryMock).count(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));

		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void findErrandWithoutMatches() {
		// Setup
		final Specification<ErrandEntity> filter = new FilterSpecification<>("id: 'uuid'");
		final var sort = Sort.by(DESC, "attribute.1", "attribute.2");
		final Pageable pageable = PageRequest.of(3, 7, sort);

		// Mock
		when(repositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(emptyList()));

		// Call
		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, filter, pageable);

		// Assertions and verifications
		assertThat(matches.getContent()).isEmpty();
		assertThat(matches.getNumberOfElements()).isZero();
		assertThat(matches.getTotalElements()).isZero();
		assertThat(matches.getTotalPages()).isZero();
		assertThat(matches.getPageable()).usingRecursiveComparison().isEqualTo(pageable);
		assertThat(matches.getSort()).usingRecursiveComparison().isEqualTo(sort);

		verify(repositoryMock).findAll(specificationCaptor.capture(), eq(pageable));
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));

		verify(repositoryMock).count(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));

		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void readExistingErrand() {
		// Setup
		final var entity = buildErrandEntity();

		// Mock
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(repositoryMock.getReferenceById(ID)).thenReturn(entity);

		// Call
		final var response = service.readErrand(NAMESPACE, MUNICIPALITY_ID, ID);

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ID);

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ID, NAMESPACE, MUNICIPALITY_ID);
		verify(repositoryMock).getReferenceById(ID);
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void readNonExistingErrand() {
		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrand(NAMESPACE, MUNICIPALITY_ID, ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void updateExistingErrand() {
		// Setup
		final var entity = buildErrandEntity();

		// Mock
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(repositoryMock.getReferenceById(ID)).thenReturn(entity);
		when(repositoryMock.save(entity)).thenReturn(entity);

		// Call
		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ID, buildErrand());

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ID);

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ID, NAMESPACE, MUNICIPALITY_ID);
		verify(repositoryMock).getReferenceById(ID);
		verify(repositoryMock).save(entity);
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void updateNonExistingErrand() {
		// Call
		final var errand = Errand.create();
		final var exception = assertThrows(ThrowableProblem.class, () -> service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ID, errand));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void deleteExistingErrand() {
		// Mock
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		// Call
		service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ID);

		// Assertions and verifications
		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ID, NAMESPACE, MUNICIPALITY_ID);
		verify(repositoryMock).deleteById(ID);
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void deleteNonExistingErrand() {
		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(repositoryMock);
	}
}
