package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
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
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.distinct;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
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

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.Metadata;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.eventlog.EventlogClient;

@ExtendWith(MockitoExtension.class)
class ErrandServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String REVISION_ID = "revisionId";
	private static final String OWNER = "SupportManagement";
	private static final String SOURCE_TYPE = Errand.class.getSimpleName();
	private static final String CREATE_MESSAGE = "Ärendet har skapats.";
	private static final String UPDATE_MESSAGE = "Ärendet har uppdaterats.";
	private static final String DELETE_MESSAGE = "Ärendet har raderats.";

	@Mock
	private ErrandsRepository errandRepositoryMock;

	@Mock
	private RevisionService revisionServiceMock;

	@Mock
	private Revision currentRevisionMock;

	@Mock
	private Revision previousRevisionMock;

	@Mock
	private EventlogClient eventLogClientMock;

	@InjectMocks
	private ErrandService service;

	@Captor
	private ArgumentCaptor<Specification<ErrandEntity>> specificationCaptor;

	@Captor
	private ArgumentCaptor<Event> eventCaptor;

	@Test
	void createErrand() {
		// Setup
		final var errand = buildErrand();

		// Mock
		when(errandRepositoryMock.save(any(ErrandEntity.class))).thenReturn(ErrandEntity.create().withId(ERRAND_ID));
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(currentRevisionMock);
		when(currentRevisionMock.getId()).thenReturn(REVISION_ID);

		// Call
		final var result = service.createErrand(NAMESPACE, MUNICIPALITY_ID, errand);

		// Assertions and verifications
		assertThat(result).isEqualTo(ERRAND_ID);

		verify(errandRepositoryMock).save(any(ErrandEntity.class));
		verify(revisionServiceMock).createErrandRevision(any(ErrandEntity.class));
		verify(eventLogClientMock).createEvent(eq(ERRAND_ID), eventCaptor.capture());

		final var event = eventCaptor.getValue();
		assertThat(event.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(event.getExpires()).isNull();
		assertThat(event.getHistoryReference()).isEqualTo(REVISION_ID);
		assertThat(event.getMessage()).isEqualTo(CREATE_MESSAGE);
		assertThat(event.getMetadata()).isNotNull()
			.extracting(
				Metadata::getKey,
				Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple("CurrentVersion", "0"),
				tuple("CurrentRevision", REVISION_ID));
		assertThat(event.getOwner()).isEqualTo(OWNER);
		assertThat(event.getSourceType()).isEqualTo(SOURCE_TYPE);
		assertThat(event.getType()).isEqualTo(CREATE);
	}

	@Test
	void findErrandWithMatches() {
		// Setup
		final Specification<ErrandEntity> filter = new FilterSpecification<>("id: 'uuid'");
		final var sort = Sort.by(DESC, "attribute.1", "attribute.2");
		final Pageable pageable = PageRequest.of(1, 2, sort);

		// Mock
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(buildErrandEntity(), buildErrandEntity()), pageable, 2L));
		when(errandRepositoryMock.count(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(10L);

		// Call
		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, filter, pageable);

		// Assertions and verifications
		assertThat(matches.getContent()).isNotEmpty().hasSize(2);
		assertThat(matches.getNumberOfElements()).isEqualTo(2);
		assertThat(matches.getTotalElements()).isEqualTo(10);
		assertThat(matches.getTotalPages()).isEqualTo(5);
		assertThat(matches.getPageable()).usingRecursiveComparison().isEqualTo(pageable);
		assertThat(matches.getSort()).usingRecursiveComparison().isEqualTo(sort);

		verify(errandRepositoryMock).findAll(specificationCaptor.capture(), eq(pageable));
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(distinct().and(withNamespace(NAMESPACE)).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));

		verify(errandRepositoryMock).count(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(distinct().and(withNamespace(NAMESPACE)).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));
	}

	@Test
	void findErrandWithoutMatches() {
		// Setup
		final Specification<ErrandEntity> filter = new FilterSpecification<>("id: 'uuid'");
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
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(distinct().and(withNamespace(NAMESPACE)).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));

		verify(errandRepositoryMock).count(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(distinct().and(withNamespace(NAMESPACE)).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));
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
		final var currentRevision = "currentRevision";
		final var currentVersion = 1;

		// Mock
		when(errandRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandRepositoryMock.getReferenceById(ERRAND_ID)).thenReturn(entity);
		when(errandRepositoryMock.save(entity)).thenReturn(entity);
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(currentRevisionMock);
		when(currentRevisionMock.getId()).thenReturn(currentRevision);
		when(currentRevisionMock.getVersion()).thenReturn(currentVersion);
		when(revisionServiceMock.getErrandRevisionByVersion(ERRAND_ID, currentVersion - 1)).thenReturn(previousRevisionMock);
		when(previousRevisionMock.getId()).thenReturn(REVISION_ID);
		when(previousRevisionMock.getVersion()).thenReturn(currentVersion - 1);

		// Call
		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, buildErrand());

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ERRAND_ID);

		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandRepositoryMock).getReferenceById(ERRAND_ID);
		verify(errandRepositoryMock).save(entity);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(revisionServiceMock).getErrandRevisionByVersion(ERRAND_ID, currentVersion - 1);
		verify(eventLogClientMock).createEvent(eq(ERRAND_ID), eventCaptor.capture());

		final var event = eventCaptor.getValue();
		assertThat(event.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(event.getExpires()).isNull();
		assertThat(event.getHistoryReference()).isEqualTo(currentRevision);
		assertThat(event.getMessage()).isEqualTo(UPDATE_MESSAGE);
		assertThat(event.getMetadata()).isNotNull()
			.extracting(
				Metadata::getKey,
				Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple("CurrentVersion", String.valueOf(currentVersion)),
				tuple("CurrentRevision", currentRevision),
				tuple("PreviousVersion", String.valueOf(currentVersion - 1)),
				tuple("PreviousRevision", REVISION_ID));
		assertThat(event.getOwner()).isEqualTo(OWNER);
		assertThat(event.getSourceType()).isEqualTo(SOURCE_TYPE);
		assertThat(event.getType()).isEqualTo(UPDATE);
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

		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void deleteExistingErrand() {
		// Mock
		when(errandRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(revisionServiceMock.getLatestErrandRevision(ERRAND_ID)).thenReturn(currentRevisionMock);
		when(currentRevisionMock.getId()).thenReturn(REVISION_ID);

		// Call
		service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assertions and verifications
		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandRepositoryMock).deleteById(ERRAND_ID);
		verify(eventLogClientMock).createEvent(eq(ERRAND_ID), eventCaptor.capture());

		final var event = eventCaptor.getValue();
		assertThat(event.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(event.getExpires()).isNull();
		assertThat(event.getHistoryReference()).isEqualTo(REVISION_ID);
		assertThat(event.getMessage()).isEqualTo(DELETE_MESSAGE);
		assertThat(event.getMetadata()).isNullOrEmpty();
		assertThat(event.getOwner()).isEqualTo(OWNER);
		assertThat(event.getSourceType()).isEqualTo(SOURCE_TYPE);
		assertThat(event.getType()).isEqualTo(DELETE);
	}

	@Test
	void deleteNonExistingErrand() {
		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandRepositoryMock, revisionServiceMock, eventLogClientMock);
	}
}
