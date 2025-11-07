package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import static org.zalando.problem.Status.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrand;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.ERRAND;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ErrandLabel;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;

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

	@Mock
	private CommunicationService communicationServiceMock;

	@Mock
	private ErrandAttachmentService errandAttachmentServiceMock;

	@Mock
	private NotesClient notesClientMock;

	@Mock
	private ConversationService conversationServiceMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private AccessControlService accessControlServiceMock;

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
		verify(eventServiceMock).createErrandEvent(eq(CREATE), eq(EVENT_LOG_CREATE_ERRAND), any(ErrandEntity.class), eq(currentRevisionMock), eq(null), eq(false), eq(ERRAND));
	}

	@Test
	void findErrandWithMatches() {
		// Setup
		final Specification<ErrandEntity> filter = filterSpecificationConverterSpy.convert("id: 'uuid'");
		final var sort = Sort.by(DESC, "attribute.1", "attribute.2");
		final Pageable pageable = PageRequest.of(1, 2, sort);
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(buildErrandEntity(), buildErrandEntity()), pageable, 2L));
		when(accessControlServiceMock.withAccessControl(any(), any(), any())).thenReturn(specification);

		// Call
		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, filter, pageable);

		// Assertions and verifications
		assertThat(matches.getContent()).isNotEmpty().hasSize(2);
		assertThat(matches.getNumberOfElements()).isEqualTo(2);
		assertThat(matches.getTotalElements()).isEqualTo(4);
		assertThat(matches.getTotalPages()).isEqualTo(2);
		assertThat(matches.getPageable()).usingRecursiveComparison().isEqualTo(pageable);
		assertThat(matches.getSort()).usingRecursiveComparison().isEqualTo(sort);

		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user);
		verify(errandRepositoryMock).findAll(specificationCaptor.capture(), eq(pageable));
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(specification).and(filter));

		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(specification).and(filter));
	}

	@Test
	void findErrandWithoutMatches() {
		// Setup
		final Specification<ErrandEntity> filter = filterSpecificationConverterSpy.convert("id: 'uuid'");
		final var sort = Sort.by(DESC, "attribute.1", "attribute.2");
		final Pageable pageable = PageRequest.of(3, 7, sort);
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(emptyList()));
		when(accessControlServiceMock.withAccessControl(any(), any(), any())).thenReturn(specification);

		// Call
		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, filter, pageable);

		// Assertions and verifications
		assertThat(matches.getContent()).isEmpty();
		assertThat(matches.getNumberOfElements()).isZero();
		assertThat(matches.getTotalElements()).isZero();
		assertThat(matches.getTotalPages()).isZero();
		assertThat(matches.getPageable()).usingRecursiveComparison().isEqualTo(pageable);
		assertThat(matches.getSort()).usingRecursiveComparison().isEqualTo(sort);

		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user);
		verify(errandRepositoryMock).findAll(specificationCaptor.capture(), eq(pageable));
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(specification).and(filter));

		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(specification).and(filter));
	}

	@Test
	void readExistingErrand() {
		// Setup
		final var entity = buildErrandEntity();
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(errandRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.of(entity));
		when(accessControlServiceMock.withAccessControl(any(), any(), any())).thenReturn(specification);

		// Call
		final var response = service.readErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ERRAND_ID);
		assertThat(response.getActiveNotifications()).hasSize(1);

		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user);
		verify(errandRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(specification));
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
	void readUnauthorized() {
		// Setup
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(errandRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(errandRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.empty());
		when(accessControlServiceMock.withAccessControl(any(), any(), any())).thenReturn(specification);

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(exception.getTitle()).isEqualTo(UNAUTHORIZED.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Unauthorized: Errand not accessible by user 'user'");
		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user);
		verify(errandRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(specification));
	}

	@Test
	void updateExistingErrand() {
		// Setup
		final var entity = buildErrandEntity();
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any())).thenReturn(specification);
		when(errandRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.of(entity));
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
		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, Access.AccessLevelEnum.RW);
		verify(errandRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(specification));
		verify(errandRepositoryMock).save(entity);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, currentRevisionMock, previousRevisionMock, ERRAND);
	}

	@Test
	void updateExistingErrandWithNonExistingMetadataLabelIds() {

		// Arrange
		final var entity = buildErrandEntity();
		final var missingMetadataLabelId = UUID.randomUUID().toString();
		final var patchErrand = buildErrand().withLabels(List.of(ErrandLabel.create().withId(missingMetadataLabelId)));
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any())).thenReturn(specification);
		when(errandRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.of(entity));
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		// Act
		assertThatThrownBy(() -> service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, patchErrand))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: The provided label-ID:s '%s' could not be found in namespace '%s' for municipality with id '%s'".formatted(missingMetadataLabelId, entity.getNamespace(), entity.getMunicipalityId()));

		// Assert
		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, Access.AccessLevelEnum.RW);
		verify(errandRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(specification));
		verify(errandRepositoryMock, never()).save(any());
		verify(revisionServiceMock, never()).createErrandRevision(any());
		verify(eventServiceMock, never()).createErrandEvent(any(), any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("Verification that an update with no change to the errand (hence no creation of a new revision) doesn't create a log event")
	void updateExistingErrandWhenCreateRevisionReturnsNull() {
		// Setup
		final var entity = buildErrandEntity();
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any())).thenReturn(specification);
		when(errandRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.of(entity));
		when(errandRepositoryMock.save(entity)).thenReturn(entity);
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		// Call
		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, buildErrand());

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ERRAND_ID);

		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, Access.AccessLevelEnum.RW);
		verify(errandRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(specification));
		verify(errandRepositoryMock).save(entity);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(revisionServiceMock, never()).getErrandRevisionByVersion(any(), any(), any(), anyInt());
		verify(eventServiceMock, never()).createErrandEvent(any(), any(), any(), any(), any(), any());
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
	void updateExistingErrandUnauthorized() {
		// Setup
		final var errand = Errand.create();
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any())).thenReturn(specification);
		when(errandRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.empty());

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, errand));

		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(exception.getTitle()).isEqualTo(UNAUTHORIZED.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Unauthorized: Errand not accessible by user 'user'");
		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, Access.AccessLevelEnum.RW);
		verify(errandRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(specification));
	}

	@Test
	void deleteExistingErrand() {
		// Setup
		final var entity = buildErrandEntity();
		final var errandAttachment = ErrandAttachment.create().withId("id");
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any())).thenReturn(specification);
		when(errandRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.of(entity));
		when(revisionServiceMock.getLatestErrandRevision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(currentRevisionMock);
		when(errandAttachmentServiceMock.readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(List.of(errandAttachment));
		when(notesClientMock.findNotes(MUNICIPALITY_ID, null, null, ERRAND_ID, null, null, 1, 1000))
			.thenReturn(new FindNotesResponse().notes(List.of(new Note().id("id"))));

		// Call
		service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assertions and verifications
		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, Access.AccessLevelEnum.RW);
		verify(errandRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(specification));
		verify(conversationServiceMock).deleteByErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(notesClientMock).findNotes(MUNICIPALITY_ID, null, null, ERRAND_ID, null, null, 1, 1000);
		verify(notesClientMock).deleteNoteById(MUNICIPALITY_ID, "id");
		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandRepositoryMock).deleteById(ERRAND_ID);
		verify(communicationServiceMock).deleteAllCommunicationsByErrandNumber(entity.getErrandNumber());
		verify(errandAttachmentServiceMock).readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		verify(attachmentRepositoryMock).deleteById(errandAttachment.getId());
		verify(eventServiceMock).createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, currentRevisionMock, null, false, ERRAND);
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

	@Test
	void deleteExistingErrandUnauthorized() {
		// Setup
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any())).thenReturn(specification);
		when(errandRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.empty());

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(exception.getTitle()).isEqualTo(UNAUTHORIZED.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Unauthorized: Errand not accessible by user 'user'");
		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, Access.AccessLevelEnum.RW);
		verify(errandRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(specification));

	}

	@Test
	void countErrands() {
		// Setup
		final Specification<ErrandEntity> filter = filterSpecificationConverterSpy.convert("id: 'uuid'");

		// Mock
		when(errandRepositoryMock.count(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(42L);

		// Call
		final var count = service.countErrands(NAMESPACE, MUNICIPALITY_ID, filter);

		// Assertions and verifications
		assertThat(count).isEqualTo(42L);

		verify(errandRepositoryMock).count(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(filter));
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandRepositoryMock, revisionServiceMock, eventServiceMock);
	}
}
