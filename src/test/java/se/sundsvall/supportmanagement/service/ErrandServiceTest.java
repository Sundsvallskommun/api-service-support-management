package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrand;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;

@ExtendWith(MockitoExtension.class)
class ErrandServiceTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "municipalityId";

	private static final String ERRAND_ID = "errandId";

	private static final String EVENT_LOG_CREATE_ERRAND = "Ärendet har skapats.";

	private static final String EVENT_LOG_UPDATE_ERRAND = "Ärendet har uppdaterats.";

	private static final String EVENT_LOG_DELETE_ERRAND = "Ärendet har raderats.";

	@Mock
	private ErrandNumberGeneratorService stringGeneratorServiceMock;

	@Mock
	private ErrandsRepository errandRepositoryMock;

	@Mock
	private ContactReasonRepository contactReasonRepositoryMock;

	@Mock
	private RevisionService revisionServiceMock;

	@Mock
	private Revision currentRevisionMock;

	@Mock
	private Revision previousRevisionMock;

	@Mock
	private EventService eventServiceMock;

	@Spy
	private FilterSpecificationConverter filterSpecificationConverterSpy;

	@InjectMocks
	private ErrandService service;

	@Captor
	private ArgumentCaptor<Specification<ErrandEntity>> specificationCaptor;

	@Test
	void createErrand() {
		// Setup
		final var errand = buildErrand();

		// Mock
		when(errandRepositoryMock.save(any(ErrandEntity.class))).thenReturn(ErrandEntity.create().withId(ERRAND_ID));
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(new RevisionResult(null, currentRevisionMock));
		when(stringGeneratorServiceMock.generateErrandNumber(any(String.class), any(String.class))).thenReturn("KC-23090001");
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		final var result = service.createErrand(NAMESPACE, MUNICIPALITY_ID, errand);

		// Assertions and verifications
		assertThat(result).isEqualTo(ERRAND_ID);

		verify(errandRepositoryMock).save(any(ErrandEntity.class));
		verify(revisionServiceMock).createErrandRevision(any(ErrandEntity.class));
		verify(eventServiceMock).createErrandEvent(eq(CREATE), eq(EVENT_LOG_CREATE_ERRAND), any(ErrandEntity.class), eq(currentRevisionMock), eq(null));
	}

	@Test
	void findErrandWithMatches() {
		// Setup
		final Specification<ErrandEntity> filter = filterSpecificationConverterSpy.convert("id: 'uuid'");
		final var sort = Sort.by(DESC, "attribute.1", "attribute.2");
		final Pageable pageable = PageRequest.of(1, 2, sort);

		// Mock
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(buildErrandEntity(), buildErrandEntity()), pageable, 2L));

		// Call
		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, filter, pageable);

		// Assertions and verifications
		assertThat(matches.getContent()).isNotEmpty().hasSize(2);
		assertThat(matches.getNumberOfElements()).isEqualTo(2);
		assertThat(matches.getTotalElements()).isEqualTo(4);
		assertThat(matches.getTotalPages()).isEqualTo(2);
		assertThat(matches.getPageable()).usingRecursiveComparison().isEqualTo(pageable);
		assertThat(matches.getSort()).usingRecursiveComparison().isEqualTo(sort);

		verify(errandRepositoryMock).findAll(specificationCaptor.capture(), eq(pageable));
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));

		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));
	}

	@Test
	void findErrandWithoutMatches() {
		// Setup
		final Specification<ErrandEntity> filter = filterSpecificationConverterSpy.convert("id: 'uuid'");
		final var sort = Sort.by(DESC, "attribute.1", "attribute.2");
		final Pageable pageable = PageRequest.of(3, 7, sort);

		// Mock
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(emptyList()));

		// Call
		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, filter, pageable);

		// Assertions and verifications
		assertThat(matches.getContent()).isEmpty();
		assertThat(matches.getNumberOfElements()).isZero();
		assertThat(matches.getTotalElements()).isZero();
		assertThat(matches.getTotalPages()).isZero();
		assertThat(matches.getPageable()).usingRecursiveComparison().isEqualTo(pageable);
		assertThat(matches.getSort()).usingRecursiveComparison().isEqualTo(sort);

		verify(errandRepositoryMock).findAll(specificationCaptor.capture(), eq(pageable));
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));

		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));
	}

	@Test
	void readExistingErrand() {
		// Setup
		final var entity = buildErrandEntity();

		// Mock
		when(errandRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandRepositoryMock.getReferenceById(ERRAND_ID)).thenReturn(entity);

		// Call
		final var response = service.readErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ERRAND_ID);
		assertThat(response.getActiveNotifications()).hasSize(1);

		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandRepositoryMock).getReferenceById(ERRAND_ID);
	}

	@Test
	void readNonExistingErrand() {
		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void updateExistingErrand() {
		// Setup
		final var entity = buildErrandEntity();

		// Mock
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandRepositoryMock.getReferenceById(ERRAND_ID)).thenReturn(entity);
		when(errandRepositoryMock.save(entity)).thenReturn(entity);
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		// Call
		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, buildErrand());

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ERRAND_ID);
		assertThat(response.getSuspension()).extracting("suspendedFrom", "suspendedTo").containsExactlyInAnyOrder(entity.getSuspendedFrom(), entity.getSuspendedTo());

		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandRepositoryMock).getReferenceById(ERRAND_ID);
		verify(errandRepositoryMock).save(entity);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, currentRevisionMock, previousRevisionMock);
	}

	@Test
	@DisplayName("Verification that an update with no change to the errand (hence no creation of a new revision) doesn't create a log event")
	void updateExistingErrandWhenCreateRevisionReturnsNull() {
		// Setup
		final var entity = buildErrandEntity();

		// Mock
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandRepositoryMock.getReferenceById(ERRAND_ID)).thenReturn(entity);
		when(errandRepositoryMock.save(entity)).thenReturn(entity);
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		// Call
		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, buildErrand());

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ERRAND_ID);

		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandRepositoryMock).getReferenceById(ERRAND_ID);
		verify(errandRepositoryMock).save(entity);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(revisionServiceMock, never()).getErrandRevisionByVersion(any(), any(), any(), anyInt());
		verify(eventServiceMock, never()).createErrandEvent(any(), any(), any(), any(), any());
	}

	@Test
	void updateNonExistingErrand() {
		// Call
		final var errand = Errand.create();
		final var exception = assertThrows(ThrowableProblem.class, () -> service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, errand));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void deleteExistingErrand() {
		// Setup
		final var entity = buildErrandEntity();

		// Mock
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandRepositoryMock.getReferenceById(ERRAND_ID)).thenReturn(entity);
		when(revisionServiceMock.getLatestErrandRevision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(currentRevisionMock);

		// Call
		service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assertions and verifications
		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandRepositoryMock).deleteById(ERRAND_ID);
		verify(eventServiceMock).createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, currentRevisionMock, null, false);
	}

	@Test
	void deleteNonExistingErrand() {
		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandRepositoryMock, revisionServiceMock, eventServiceMock);
	}
}
