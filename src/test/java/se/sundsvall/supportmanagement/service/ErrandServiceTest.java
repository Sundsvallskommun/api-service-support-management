package se.sundsvall.supportmanagement.service;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.model.RevisionResult;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrand;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.ERRAND;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

@ExtendWith(MockitoExtension.class)
class ErrandServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String EVENT_LOG_CREATE_ERRAND = "Ärendet har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND = "Ärendet har uppdaterats.";
	private static final String EVENT_LOG_DELETE_ERRAND = "Ärendet har raderats.";
	private static final String REFERRED_FROM_RELATION_TYPE = "REFERRED_FROM";
	private static final String REFERRED_FROM_RESOURCE_IDENTIFIER_TYPE = "case";
	private static final String REFERRED_FROM_RESOURCE_IDENTIFIER_SERVICE = "support-management";

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
	private AccessControlService accessControlServiceMock;

	@Mock
	private RelationClient relationClientMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

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
		verifyNoInteractions(relationClientMock);
	}

	@Test
	void createErrandWithReferredFrom() {
		// Setup
		final var errand = buildErrand();
		final var referredFrom = "originalErrandId";
		final var relation = new Relation()
			.type(REFERRED_FROM_RELATION_TYPE)
			.source(new ResourceIdentifier()
				.resourceId(referredFrom)
				.type(REFERRED_FROM_RESOURCE_IDENTIFIER_TYPE)
				.service(REFERRED_FROM_RESOURCE_IDENTIFIER_SERVICE)
				.namespace(NAMESPACE))
			.target(new ResourceIdentifier()
				.resourceId(ERRAND_ID)
				.type(REFERRED_FROM_RESOURCE_IDENTIFIER_TYPE)
				.service(REFERRED_FROM_RESOURCE_IDENTIFIER_SERVICE)
				.namespace(NAMESPACE));

		// Mock
		when(errandRepositoryMock.save(any(ErrandEntity.class))).thenReturn(ErrandEntity.create().withId(ERRAND_ID));
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(new RevisionResult(null, currentRevisionMock));
		when(stringGeneratorServiceMock.generateErrandNumber(any(String.class), any(String.class))).thenReturn("KC-23090001");
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(any(), any(), any()))
			.thenReturn(Optional.of(ContactReasonEntity.create().withReason("reason")));

		final var result = service.createErrand(NAMESPACE, MUNICIPALITY_ID, errand, referredFrom);

		// Assertions and verifications
		assertThat(result).isEqualTo(ERRAND_ID);

		verify(errandRepositoryMock).save(any(ErrandEntity.class));
		verify(revisionServiceMock).createErrandRevision(any(ErrandEntity.class));
		verify(eventServiceMock).createErrandEvent(eq(CREATE), eq(EVENT_LOG_CREATE_ERRAND), any(ErrandEntity.class),
			eq(currentRevisionMock), eq(null), eq(false), eq(ERRAND));
		verify(relationClientMock).createRelation(MUNICIPALITY_ID, relation);
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void findErrandWithMatches(boolean limited) {
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
		when(accessControlServiceMock.limitedMappingPredicateByLabel(any(), any(), any())).thenReturn(_ -> limited);

		// Call
		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, filter, pageable);

		// Assertions and verifications
		assertThat(matches.getContent()).isNotEmpty().hasSize(2).extracting("priority").containsOnly(limited ? null : Priority.HIGH);
		assertThat(matches.getNumberOfElements()).isEqualTo(2);
		assertThat(matches.getTotalElements()).isEqualTo(4);
		assertThat(matches.getTotalPages()).isEqualTo(2);
		assertThat(matches.getPageable()).usingRecursiveComparison().isEqualTo(pageable);
		assertThat(matches.getSort()).usingRecursiveComparison().isEqualTo(sort);

		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user);
		verify(accessControlServiceMock).limitedMappingPredicateByLabel(NAMESPACE, MUNICIPALITY_ID, user);
		verify(errandRepositoryMock).findAll(specificationCaptor.capture(), eq(pageable));
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

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void readExistingErrand(boolean limited) {
		// Setup
		final var entity = buildErrandEntity();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean())).thenReturn(entity);
		when(accessControlServiceMock.limitedMappingPredicateByLabel(any(), any(), any())).thenReturn(_ -> limited);

		// Call
		final var response = service.readErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ERRAND_ID);
		assertThat(response.getPriority()).isEqualTo(limited ? null : Priority.HIGH);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false);
		verify(accessControlServiceMock).limitedMappingPredicateByLabel(NAMESPACE, MUNICIPALITY_ID, user);
		verifyNoInteractions(errandRepositoryMock);
	}

	@Test
	void updateExistingErrand() {
		// Setup
		final var entity = buildErrandEntity();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(entity);
		when(errandRepositoryMock.save(entity)).thenReturn(entity);
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		// Call
		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, buildErrand());

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ERRAND_ID);
		assertThat(response.getSuspension()).extracting("suspendedFrom", "suspendedTo").containsExactlyInAnyOrder(entity.getSuspendedFrom(), entity.getSuspendedTo());

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, Access.AccessLevelEnum.RW);
		verify(errandRepositoryMock).save(entity);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, currentRevisionMock, previousRevisionMock, ERRAND);
	}

	@Test
	@DisplayName("Verification that an update with no change to the errand (hence no creation of a new revision) doesn't create a log event")
	void updateExistingErrandWhenCreateRevisionReturnsNull() {
		// Setup
		final var entity = buildErrandEntity();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(entity);
		when(errandRepositoryMock.save(entity)).thenReturn(entity);
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		// Call
		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, buildErrand());

		// Assertions and verifications
		assertThat(response.getId()).isEqualTo(ERRAND_ID);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, Access.AccessLevelEnum.RW);
		verify(errandRepositoryMock).save(entity);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(revisionServiceMock, never()).getErrandRevisionByVersion(any(), any(), any(), anyInt());
		verify(eventServiceMock, never()).createErrandEvent(any(), any(), any(), any(), any(), any());
	}

	@Test
	void deleteExistingErrand() {
		// Setup
		final var entity = buildErrandEntity();
		final var errandAttachment = ErrandAttachment.create().withId("id");
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		// Mock
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(entity);
		when(revisionServiceMock.getLatestErrandRevision(any())).thenReturn(currentRevisionMock);
		when(errandAttachmentServiceMock.readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(List.of(errandAttachment));
		when(notesClientMock.findNotes(MUNICIPALITY_ID, null, null, ERRAND_ID, null, null, 1, 1000))
			.thenReturn(new FindNotesResponse().notes(List.of(new Note().id("id"))));

		// Call
		service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Assertions and verifications
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, Access.AccessLevelEnum.RW);
		verify(conversationServiceMock).deleteByErrandId(same(entity));
		verify(notesClientMock).findNotes(MUNICIPALITY_ID, null, null, ERRAND_ID, null, null, 1, 1000);
		verify(notesClientMock).deleteNoteById(MUNICIPALITY_ID, "id");
		verify(errandRepositoryMock).deleteById(ERRAND_ID);
		verify(communicationServiceMock).deleteAllCommunicationsByErrandNumber(entity.getErrandNumber());
		verify(errandAttachmentServiceMock).readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		verify(attachmentRepositoryMock).deleteById(errandAttachment.getId());
		verify(revisionServiceMock).getLatestErrandRevision(same(entity));
		verify(eventServiceMock).createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, currentRevisionMock, null, false, ERRAND);
	}

	@Test
	void countErrands() {
		// Setup
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);
		final Specification<ErrandEntity> filter = filterSpecificationConverterSpy.convert("id: 'uuid'");
		final Specification<ErrandEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

		// Mock
		when(accessControlServiceMock.withAccessControl(any(), any(), any())).thenReturn(specification);
		when(errandRepositoryMock.count(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(42L);

		// Call
		final var count = service.countErrands(NAMESPACE, MUNICIPALITY_ID, filter);

		// Assertions and verifications
		assertThat(count).isEqualTo(42L);

		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user);
		verify(errandRepositoryMock).count(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withNamespace(NAMESPACE).and(withMunicipalityId(MUNICIPALITY_ID)).and(specification).and(filter));
	}

	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandRepositoryMock, revisionServiceMock, eventServiceMock, metadataLabelRepositoryMock);
	}
}
