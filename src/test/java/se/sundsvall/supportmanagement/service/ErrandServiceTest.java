package se.sundsvall.supportmanagement.service;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.config.action.enums.OperationType;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ErrandLabel;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.model.RevisionResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrand;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;

@ExtendWith(MockitoExtension.class)
class ErrandServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String EVENT_LOG_CREATE_ERRAND = "Ärendet har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND = "Ärendet har uppdaterats.";
	private static final String EVENT_LOG_DELETE_ERRAND = "Ärendet har raderats.";
	private static final String REFERRED_FROM_RESOURCE_IDENTIFIER_TYPE = "case";
	private static final String REFERRED_FROM_RESOURCE_IDENTIFIER_SERVICE = "support-management";

	@Mock
	private ErrandNumberGeneratorService stringGeneratorServiceMock;

	@Mock
	private ErrandsRepository errandRepositoryMock;

	@Mock
	private ContactReasonRepository contactReasonRepositoryMock;

	@Mock
	private MeasureValidator measureValidatorMock;

	@Mock
	private RevisionService revisionServiceMock;

	@Mock
	private Revision currentRevisionMock;

	@Mock
	private Revision previousRevisionMock;

	@Mock
	private EventService eventServiceMock;

	@Mock
	private ErrandAttachmentService errandAttachmentServiceMock;

	@Mock
	private ErrandDataDeleter errandDataDeleterMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private RelationClient relationClientMock;

	@Mock
	private MetadataLabelRepository metadataLabelRepositoryMock;

	@Mock
	private ErrandActionService errandActionServiceMock;

	@Mock
	private ErrandPhaseService errandPhaseServiceMock;

	@Mock
	private jakarta.persistence.EntityManager entityManagerMock;

	@Spy
	private FilterSpecificationConverter filterSpecificationConverterSpy;

	@InjectMocks
	private ErrandService service;

	@Captor
	private ArgumentCaptor<Specification<ErrandEntity>> specificationCaptor;

	@Test
	void createErrand() {
		final var errand = buildErrand();

		when(errandRepositoryMock.save(any(ErrandEntity.class))).thenReturn(ErrandEntity.create().withId(ERRAND_ID));
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(new RevisionResult(null, currentRevisionMock));
		when(stringGeneratorServiceMock.generateErrandNumber(any(String.class), any(String.class))).thenReturn("KC-23090001");
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		final var result = service.createErrand(NAMESPACE, MUNICIPALITY_ID, errand, null);

		assertThat(result).isEqualTo(ERRAND_ID);

		verify(errandPhaseServiceMock).processPhaseChange(any(ErrandEntity.class), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandPhaseServiceMock).validateStatusAgainstActivePhase(any(ErrandEntity.class), any());
		verify(errandRepositoryMock).save(any(ErrandEntity.class));
		verify(errandActionServiceMock).processErrandActions(any(ErrandEntity.class), eq(OperationType.CREATE));
		verify(revisionServiceMock).createErrandRevision(any(ErrandEntity.class));
		verify(eventServiceMock).createErrandEvent(eq(CREATE), eq(EVENT_LOG_CREATE_ERRAND), any(ErrandEntity.class), eq(currentRevisionMock), eq(null), eq(false), eq(ERRAND));
		verifyNoInteractions(relationClientMock);
	}

	@Test
	void createErrandWithReferredFrom() {
		final var errand = buildErrand();
		final var relationType = "some_relation_type";
		final var referredFromType = "referredFromType";
		final var referredFromService = "referredFromService";
		final var referredFromNamespace = "referredFromNamespace";
		final var referredFromIdentifier = "referredFromIdentifier";
		final var referredFrom = relationType + "|" + referredFromIdentifier + ";" + referredFromType + ";" + referredFromService + ";" + referredFromNamespace + "|";
		final var relation = new Relation()
			.type(relationType.toUpperCase())
			.source(new ResourceIdentifier()
				.resourceId(referredFromIdentifier)
				.type(REFERRED_FROM_RESOURCE_IDENTIFIER_TYPE)
				.service(referredFromService.toLowerCase())
				.namespace(referredFromNamespace))
			.target(new ResourceIdentifier()
				.resourceId(ERRAND_ID)
				.type(REFERRED_FROM_RESOURCE_IDENTIFIER_TYPE)
				.service(REFERRED_FROM_RESOURCE_IDENTIFIER_SERVICE)
				.namespace(NAMESPACE));

		when(errandRepositoryMock.save(any(ErrandEntity.class))).thenReturn(ErrandEntity.create().withId(ERRAND_ID));
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(new RevisionResult(null, currentRevisionMock));
		when(stringGeneratorServiceMock.generateErrandNumber(any(String.class), any(String.class))).thenReturn("KC-23090001");
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(any(), any(), any()))
			.thenReturn(Optional.of(ContactReasonEntity.create().withReason("reason")));

		final var result = service.createErrand(NAMESPACE, MUNICIPALITY_ID, errand, referredFrom);

		assertThat(result).isEqualTo(ERRAND_ID);

		verify(errandPhaseServiceMock).processPhaseChange(any(ErrandEntity.class), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandPhaseServiceMock).validateStatusAgainstActivePhase(any(ErrandEntity.class), any());
		verify(errandRepositoryMock).save(any(ErrandEntity.class));
		verify(errandActionServiceMock).processErrandActions(any(ErrandEntity.class), eq(OperationType.CREATE));
		verify(revisionServiceMock).createErrandRevision(any(ErrandEntity.class));
		verify(eventServiceMock).createErrandEvent(eq(CREATE), eq(EVENT_LOG_CREATE_ERRAND), any(ErrandEntity.class),
			eq(currentRevisionMock), eq(null), eq(false), eq(ERRAND));
		verify(relationClientMock).createRelation(MUNICIPALITY_ID, relation);
	}

	@Test
	@DisplayName("Verification that errand is still persisted when eventService throws during create")
	void createErrand_eventServiceFails_errandStillCreated() {
		final var errand = buildErrand();
		final var persistedEntity = ErrandEntity.create().withId(ERRAND_ID);

		when(errandRepositoryMock.save(any(ErrandEntity.class))).thenReturn(persistedEntity);
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(new RevisionResult(null, currentRevisionMock));
		when(stringGeneratorServiceMock.generateErrandNumber(any(String.class), any(String.class))).thenReturn("KC-23090001");
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));
		doThrow(new RuntimeException("EventLog down")).when(eventServiceMock).createErrandEvent(any(), any(), any(), any(), any(), anyBoolean(), any());

		final var result = service.createErrand(NAMESPACE, MUNICIPALITY_ID, errand, null);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(errandPhaseServiceMock).processPhaseChange(any(ErrandEntity.class), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandPhaseServiceMock).validateStatusAgainstActivePhase(any(ErrandEntity.class), any());
		verify(errandRepositoryMock).save(any(ErrandEntity.class));
		verify(revisionServiceMock).createErrandRevision(any(ErrandEntity.class));
		verify(eventServiceMock).createErrandEvent(eq(CREATE), eq(EVENT_LOG_CREATE_ERRAND), any(ErrandEntity.class), eq(currentRevisionMock), eq(null), eq(false), eq(ERRAND));
	}

	@Test
	@DisplayName("Verification that errand is still persisted when relationClient throws during create with referredFrom")
	void createErrand_relationClientFails_errandStillCreated() {
		final var errand = buildErrand();
		final var referredFrom = "REFERRED_FROM|src;case;service;ns|";

		when(errandRepositoryMock.save(any(ErrandEntity.class))).thenReturn(ErrandEntity.create().withId(ERRAND_ID));
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(new RevisionResult(null, currentRevisionMock));
		when(stringGeneratorServiceMock.generateErrandNumber(any(String.class), any(String.class))).thenReturn("KC-23090001");
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));
		doThrow(new RuntimeException("Relation service down")).when(relationClientMock).createRelation(any(), any());

		final var result = service.createErrand(NAMESPACE, MUNICIPALITY_ID, errand, referredFrom);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(errandPhaseServiceMock).processPhaseChange(any(ErrandEntity.class), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandPhaseServiceMock).validateStatusAgainstActivePhase(any(ErrandEntity.class), any());
		verify(errandRepositoryMock).save(any(ErrandEntity.class));
		verify(revisionServiceMock).createErrandRevision(any(ErrandEntity.class));
		verify(eventServiceMock).createErrandEvent(eq(CREATE), eq(EVENT_LOG_CREATE_ERRAND), any(ErrandEntity.class), eq(currentRevisionMock), eq(null), eq(false), eq(ERRAND));
		verify(relationClientMock).createRelation(any(), any());
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void findErrandWithMatches(boolean limited) {
		final Specification<ErrandEntity> filter = filterSpecificationConverterSpy.convert("id: 'uuid'");
		final var sort = Sort.by(DESC, "attribute.1", "attribute.2");
		final Pageable pageable = PageRequest.of(1, 2, sort);
		final Specification<ErrandEntity> specification = (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(buildErrandEntity(), buildErrandEntity()), pageable, 2L));
		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any(), any())).thenReturn(specification);
		when(accessControlServiceMock.roleBasedFieldResolver(any(), any(), any())).thenReturn(_ -> limited ? Map.of(ErrandField.ID, Set.<String>of()) : null);

		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, filter, pageable);

		assertThat(matches.getContent()).isNotEmpty().hasSize(2).extracting("priority").containsOnly(limited ? null : Priority.HIGH);
		assertThat(matches.getNumberOfElements()).isEqualTo(2);
		assertThat(matches.getTotalElements()).isEqualTo(4);
		assertThat(matches.getTotalPages()).isEqualTo(2);
		assertThat(matches.getPageable()).usingRecursiveComparison().isEqualTo(pageable);
		assertThat(matches.getSort()).usingRecursiveComparison().isEqualTo(sort);

		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, LR);
		verify(accessControlServiceMock).roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, user);
		verify(errandRepositoryMock).findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable));
	}

	@Test
	void findErrandWithoutMatches() {
		final Specification<ErrandEntity> filter = filterSpecificationConverterSpy.convert("id: 'uuid'");
		final var sort = Sort.by(DESC, "attribute.1", "attribute.2");
		final Pageable pageable = PageRequest.of(3, 7, sort);
		final Specification<ErrandEntity> specification = (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(emptyList()));
		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any(), any())).thenReturn(specification);

		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, filter, pageable);

		assertThat(matches.getContent()).isEmpty();
		assertThat(matches.getNumberOfElements()).isZero();
		assertThat(matches.getTotalElements()).isZero();
		assertThat(matches.getTotalPages()).isZero();
		assertThat(matches.getPageable()).usingRecursiveComparison().isEqualTo(pageable);
		assertThat(matches.getSort()).usingRecursiveComparison().isEqualTo(sort);

		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, LR);
		verify(errandRepositoryMock).findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), eq(pageable));
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void readExistingErrand(boolean limited) {
		final var entity = buildErrandEntity();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(accessControlServiceMock.roleBasedFieldResolver(any(), any(), any())).thenReturn(_ -> limited ? Map.of(ErrandField.ID, Set.<String>of()) : null);

		final var response = service.readErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		assertThat(response.getId()).isEqualTo(ERRAND_ID);
		assertThat(response.getPriority()).isEqualTo(limited ? null : Priority.HIGH);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR);
		verify(accessControlServiceMock).roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, user);
		verifyNoInteractions(errandRepositoryMock);
	}

	@Test
	void updateExistingErrand() {
		final var entity = buildErrandEntity();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(accessControlServiceMock.readableKeyResolver(any(), any(), any(), any())).thenReturn(_ -> _ -> true);
		when(accessControlServiceMock.roleBasedFieldResolver(any(), any(), any())).thenReturn(_ -> null);
		when(errandRepositoryMock.saveAndFlush(entity)).thenReturn(entity);
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, buildErrand());

		assertThat(response.getId()).isEqualTo(ERRAND_ID);
		assertThat(response.getSuspension()).extracting("suspendedFrom", "suspendedTo").containsExactlyInAnyOrder(entity.getSuspendedFrom(), entity.getSuspendedTo());

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ERRAND, RW);
		verify(errandPhaseServiceMock).processPhaseChange(eq(entity), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandPhaseServiceMock).validateStatusAgainstActivePhase(eq(entity), any());
		verify(errandRepositoryMock).saveAndFlush(entity);
		verify(errandActionServiceMock).processErrandActions(entity, OperationType.UPDATE);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(eventServiceMock).createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, currentRevisionMock, previousRevisionMock, ERRAND);
	}

	@Test
	@DisplayName("Verification that an update with no change to the errand (hence no creation of a new revision) doesn't create a log event")
	void updateExistingErrandWhenCreateRevisionReturnsNull() {
		final var entity = buildErrandEntity();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(accessControlServiceMock.readableKeyResolver(any(), any(), any(), any())).thenReturn(_ -> _ -> true);
		when(accessControlServiceMock.roleBasedFieldResolver(any(), any(), any())).thenReturn(_ -> null);
		when(errandRepositoryMock.saveAndFlush(entity)).thenReturn(entity);
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, buildErrand());

		assertThat(response.getId()).isEqualTo(ERRAND_ID);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ERRAND, RW);
		verify(errandPhaseServiceMock).processPhaseChange(eq(entity), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandPhaseServiceMock).validateStatusAgainstActivePhase(eq(entity), any());
		verify(errandRepositoryMock).saveAndFlush(entity);
		verify(errandActionServiceMock).processErrandActions(entity, OperationType.UPDATE);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(revisionServiceMock, never()).getErrandRevisionByVersion(any(), any(), any(), anyInt());
		verify(eventServiceMock, never()).createErrandEvent(any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateErrandRejectsAKeyTheUserMayNotReach() {
		final var entity = buildErrandEntity();
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user"));

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(accessControlServiceMock.readableKeyResolver(any(), any(), any(), any())).thenReturn(_ -> "visible"::equals);
		when(accessControlServiceMock.roleBasedFieldResolver(any(), any(), any())).thenReturn(_ -> null);
		doThrow(Problem.valueOf(UNAUTHORIZED)).when(accessControlServiceMock).verifyAccessibleKeys(ArgumentMatchers.<Predicate<String>>any(), eq(List.of("salary")));

		final var patch = Errand.create().withParameters(List.of(Parameter.create().withKey("salary").withValues(List.of("secret"))));

		// The whole errand patch is bound by the same key grants as the dedicated parameter endpoints.
		assertThatThrownBy(() -> service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, patch))
			.isInstanceOf(ThrowableProblem.class)
			.extracting("status").isEqualTo(UNAUTHORIZED);

		verify(errandRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updateErrandReturnsAPayloadMappedByTheSameGrantsAsARead() {
		final var entity = buildErrandEntity();
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user"));

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(accessControlServiceMock.readableKeyResolver(any(), any(), any(), any())).thenReturn(_ -> _ -> true);
		when(accessControlServiceMock.roleBasedFieldResolver(any(), any(), any())).thenReturn(_ -> Map.of(ErrandField.ID, Set.<String>of()));
		when(errandRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, Errand.create().withTitle("new title"));

		assertThat(response.getId()).isEqualTo(ERRAND_ID);
		assertThat(response).hasAllNullFieldsOrPropertiesExcept("id");

		verify(errandRepositoryMock).saveAndFlush(entity);
		verify(errandActionServiceMock).processErrandActions(entity, OperationType.UPDATE);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(errandPhaseServiceMock).processPhaseChange(eq(entity), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandPhaseServiceMock).validateStatusAgainstActivePhase(eq(entity), any());
	}

	@Test
	void createErrandWithInvalidMeasureType() {
		final var errand = buildErrand().withMeasures(List.of(Measure.create().withType("INVALID_TYPE")));

		when(stringGeneratorServiceMock.generateErrandNumber(any(String.class), any(String.class))).thenReturn("KC-23090001");
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(any(), any(), any()))
			.thenReturn(Optional.of(ContactReasonEntity.create().withReason("reason")));
		doThrow(Problem.valueOf(BAD_REQUEST, "'INVALID_TYPE' is not a valid measure type for namespace 'namespace' and municipality with id 'municipalityId'"))
			.when(measureValidatorMock).validate(errand.getMeasures(), NAMESPACE, MUNICIPALITY_ID);

		assertThatThrownBy(() -> service.createErrand(NAMESPACE, MUNICIPALITY_ID, errand, null))
			.hasMessage("Bad Request: 'INVALID_TYPE' is not a valid measure type for namespace 'namespace' and municipality with id 'municipalityId'");

		verify(measureValidatorMock).validate(errand.getMeasures(), NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void updateErrandWithInvalidMeasureType() {
		final var entity = buildErrandEntity();
		final var errand = buildErrand().withMeasures(List.of(Measure.create().withType("INVALID_TYPE")));
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(accessControlServiceMock.readableKeyResolver(any(), any(), any(), any())).thenReturn(_ -> _ -> true);
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ContactReasonEntity.create().withReason("reason")));
		doThrow(Problem.valueOf(BAD_REQUEST, "'INVALID_TYPE' is not a valid measure type for namespace 'namespace' and municipality with id 'municipalityId'"))
			.when(measureValidatorMock).validate(errand.getMeasures(), NAMESPACE, MUNICIPALITY_ID);

		assertThatThrownBy(() -> service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, errand))
			.hasMessage("Bad Request: 'INVALID_TYPE' is not a valid measure type for namespace 'namespace' and municipality with id 'municipalityId'");

		verify(errandPhaseServiceMock).processPhaseChange(eq(entity), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandPhaseServiceMock).validateStatusAgainstActivePhase(eq(entity), any());
		verify(measureValidatorMock).validate(errand.getMeasures(), NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void deleteExistingErrand() {
		final var entity = buildErrandEntity();
		final var errandAttachment = ErrandAttachment.create().withId("id");
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(revisionServiceMock.getLatestErrandRevision(any())).thenReturn(currentRevisionMock);
		when(errandAttachmentServiceMock.readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(List.of(errandAttachment));

		service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ERRAND, RW);
		verify(errandAttachmentServiceMock).readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		verify(errandDataDeleterMock).deleteRelatedData(same(entity), eq(List.of("id")));
		verify(revisionServiceMock).deleteErrandRevisions(entity.getNamespace(), entity.getMunicipalityId(), entity.getId());
		verify(errandRepositoryMock).deleteById(ERRAND_ID);
		verify(revisionServiceMock).getLatestErrandRevision(same(entity));
		verify(eventServiceMock).createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, currentRevisionMock, null, false, ERRAND);
	}

	@Test
	@DisplayName("Verification that a delete removes the revisions of the errand, and reads the latest one first since the event it writes points at it")
	void deleteErrandRemovesRevisions() {
		final var entity = buildErrandEntity();
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user"));

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(revisionServiceMock.getLatestErrandRevision(any())).thenReturn(currentRevisionMock);
		when(errandAttachmentServiceMock.readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(emptyList());

		service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null);

		final var inOrder = inOrder(revisionServiceMock, errandRepositoryMock);
		inOrder.verify(revisionServiceMock).getLatestErrandRevision(same(entity));
		inOrder.verify(revisionServiceMock).deleteErrandRevisions(entity.getNamespace(), entity.getMunicipalityId(), entity.getId());
		inOrder.verify(errandRepositoryMock).deleteById(ERRAND_ID);
		verify(eventServiceMock).createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, currentRevisionMock, null, false, ERRAND);
	}

	@Test
	@DisplayName("Verification that delete still removes the errand row when the event log is unreachable")
	void deleteErrandWhenEventLogFailsErrandIsStillDeleted() {
		final var entity = buildErrandEntity();
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user"));

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(revisionServiceMock.getLatestErrandRevision(any())).thenReturn(currentRevisionMock);
		when(errandAttachmentServiceMock.readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(emptyList());
		doThrow(new RuntimeException("Event log down")).when(eventServiceMock)
			.createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, currentRevisionMock, null, false, ERRAND);

		service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null);

		verify(errandDataDeleterMock).deleteRelatedData(same(entity), eq(emptyList()));
		verify(revisionServiceMock).deleteErrandRevisions(entity.getNamespace(), entity.getMunicipalityId(), entity.getId());
		verify(errandRepositoryMock).deleteById(ERRAND_ID);
		verify(revisionServiceMock).getLatestErrandRevision(same(entity));
		verify(eventServiceMock).createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, currentRevisionMock, null, false, ERRAND);
	}

	@Test
	@DisplayName("Verification that a purge removes the errand, everything belonging to it and its revisions, without an access check and without an event")
	void purgeErrand() {
		final var entity = buildErrandEntity();

		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));

		final var removed = service.purgeErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		assertThat(removed).isTrue();
		verify(errandRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandDataDeleterMock).deleteRelatedData(same(entity), eq(emptyList()));
		verify(revisionServiceMock).deleteErrandRevisions(entity.getNamespace(), entity.getMunicipalityId(), entity.getId());
		verify(errandRepositoryMock).deleteById(ERRAND_ID);
		verifyNoInteractions(eventServiceMock, accessControlServiceMock, errandAttachmentServiceMock);
	}

	@Test
	@DisplayName("Verification that a purge passes the attachments of the errand on without reading them through the access check")
	void purgeErrandWithAttachments() {
		final var entity = buildErrandEntity()
			.withAttachments(List.of(AttachmentEntity.create().withId("attachmentId")));

		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));

		final var removed = service.purgeErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		assertThat(removed).isTrue();
		verify(errandRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandDataDeleterMock).deleteRelatedData(same(entity), eq(List.of("attachmentId")));
		verify(revisionServiceMock).deleteErrandRevisions(entity.getNamespace(), entity.getMunicipalityId(), entity.getId());
		verify(errandRepositoryMock).deleteById(ERRAND_ID);
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	@DisplayName("Verification that an errand already gone is left alone rather than treated as an error, since that is the outcome the purge wanted")
	void purgeErrandThatIsAlreadyGone() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		final var removed = service.purgeErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		assertThat(removed).isFalse();
		verify(errandRepositoryMock).findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandRepositoryMock, never()).deleteById(any());
		verifyNoInteractions(errandDataDeleterMock, revisionServiceMock, eventServiceMock, accessControlServiceMock);
	}

	@Test
	void countErrands() {
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);
		final Specification<ErrandEntity> filter = filterSpecificationConverterSpy.convert("id: 'uuid'");
		final Specification<ErrandEntity> specification = (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();

		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any(), any())).thenReturn(specification);
		when(errandRepositoryMock.count(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(42L);

		final var count = service.countErrands(NAMESPACE, MUNICIPALITY_ID, filter);

		assertThat(count).isEqualTo(42L);

		verify(accessControlServiceMock).withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, LR);
		verify(errandRepositoryMock).count(ArgumentMatchers.<Specification<ErrandEntity>>any());
	}

	@Test
	void expandLabelsToAncestorChain_leafExpandsToFullChain() {
		final var leafId = "leaf-id";
		final var parentId = "parent-id";
		final var childId = "child-id";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(leafId)));

		when(metadataLabelRepositoryMock.findAllById(Set.of(leafId)))
			.thenReturn(List.of(MetadataLabelEntity.create().withId(leafId).withResourcePath("parent/child/leaf")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child")))
			.thenReturn(List.of(
				MetadataLabelEntity.create().withId(parentId).withResourcePath("parent"),
				MetadataLabelEntity.create().withId(childId).withResourcePath("parent/child")));

		service.expandLabelsToAncestorChain(errandEntity);

		assertThat(errandEntity.getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(leafId, parentId, childId);

		verify(metadataLabelRepositoryMock).findAllById(Set.of(leafId));
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child"));
	}

	@Test
	void expandLabelsToAncestorChain_partialChainExpandsCorrectly() {
		final var leafId = "leaf-id";
		final var parentId = "parent-id";
		final var childId = "child-id";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of(
				ErrandLabelEmbeddable.create().withMetadataLabelId(parentId),
				ErrandLabelEmbeddable.create().withMetadataLabelId(leafId)));

		when(metadataLabelRepositoryMock.findAllById(Set.of(parentId, leafId)))
			.thenReturn(List.of(
				MetadataLabelEntity.create().withId(parentId).withResourcePath("parent"),
				MetadataLabelEntity.create().withId(leafId).withResourcePath("parent/child/leaf")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child")))
			.thenReturn(List.of(
				MetadataLabelEntity.create().withId(parentId).withResourcePath("parent"),
				MetadataLabelEntity.create().withId(childId).withResourcePath("parent/child")));

		service.expandLabelsToAncestorChain(errandEntity);

		assertThat(errandEntity.getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(parentId, leafId, childId);

		verify(metadataLabelRepositoryMock).findAllById(Set.of(parentId, leafId));
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child"));
	}

	@Test
	void expandLabelsToAncestorChain_emptyLabels_noRepoInteraction() {
		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of());

		service.expandLabelsToAncestorChain(errandEntity);

		assertThat(errandEntity.getLabels()).isEmpty();
		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	void expandLabelsToAncestorChain_alreadyFullChain_noLabelsAdded() {
		final var leafId = "leaf-id";
		final var parentId = "parent-id";
		final var childId = "child-id";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withLabels(List.of(
				ErrandLabelEmbeddable.create().withMetadataLabelId(parentId),
				ErrandLabelEmbeddable.create().withMetadataLabelId(childId),
				ErrandLabelEmbeddable.create().withMetadataLabelId(leafId)));

		when(metadataLabelRepositoryMock.findAllById(Set.of(parentId, childId, leafId)))
			.thenReturn(List.of(
				MetadataLabelEntity.create().withId(parentId).withResourcePath("parent"),
				MetadataLabelEntity.create().withId(childId).withResourcePath("parent/child"),
				MetadataLabelEntity.create().withId(leafId).withResourcePath("parent/child/leaf")));
		when(metadataLabelRepositoryMock.findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child")))
			.thenReturn(List.of(
				MetadataLabelEntity.create().withId(parentId).withResourcePath("parent"),
				MetadataLabelEntity.create().withId(childId).withResourcePath("parent/child")));

		service.expandLabelsToAncestorChain(errandEntity);

		assertThat(errandEntity.getLabels())
			.extracting(ErrandLabelEmbeddable::getMetadataLabelId)
			.containsExactlyInAnyOrder(parentId, childId, leafId);

		verify(metadataLabelRepositoryMock).findAllById(Set.of(parentId, childId, leafId));
		verify(metadataLabelRepositoryMock).findByNamespaceAndMunicipalityIdAndResourcePathIn(NAMESPACE, MUNICIPALITY_ID, Set.of("parent", "parent/child"));
	}

	@Test
	void validateLabelVersions_noVersions_noRepoInteraction() {
		var labels = List.of(
			new ErrandLabel().withId("id-1"),
			new ErrandLabel().withId("id-2"));

		service.validateLabelVersions(labels);

		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	void validateLabelVersions_nullLabels_noRepoInteraction() {
		service.validateLabelVersions(null);

		verifyNoInteractions(metadataLabelRepositoryMock);
	}

	@Test
	void validateLabelVersions_versionsMatch_noException() {
		var labelId = "label-id-1";
		when(metadataLabelRepositoryMock.findAllById(List.of(labelId)))
			.thenReturn(List.of(MetadataLabelEntity.create().withId(labelId).withVersion(3L)));

		service.validateLabelVersions(List.of(new ErrandLabel().withId(labelId).withVersion(3L)));

		verify(metadataLabelRepositoryMock).findAllById(List.of(labelId));
	}

	@Test
	void validateLabelVersions_versionMismatch_throws412() {
		var labelId = "label-id-1";
		when(metadataLabelRepositoryMock.findAllById(List.of(labelId)))
			.thenReturn(List.of(MetadataLabelEntity.create().withId(labelId).withVersion(5L)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.validateLabelVersions(List.of(new ErrandLabel().withId(labelId).withVersion(3L))))
			.withMessageContaining(labelId)
			.withMessageContaining("3")
			.withMessageContaining("5");

		verify(metadataLabelRepositoryMock).findAllById(List.of(labelId));
	}

	@Test
	void validateLabelVersions_nullVersionInDb_noException() {
		var labelId = "label-id-1";
		when(metadataLabelRepositoryMock.findAllById(List.of(labelId)))
			.thenReturn(List.of(MetadataLabelEntity.create().withId(labelId)));

		service.validateLabelVersions(List.of(new ErrandLabel().withId(labelId).withVersion(1L)));

		verify(metadataLabelRepositoryMock).findAllById(List.of(labelId));
	}

	@ParameterizedTest
	@MethodSource("argumentsForExpandRelation")
	void expandRelation(final String input, final boolean expectSuccess, final Class<? extends Exception> expectedException) {
		if (expectSuccess) {
			assertThatNoException().isThrownBy(() -> service.expandRelation(input));
		} else {
			assertThatException()
				.isThrownBy(() -> service.expandRelation(input))
				.isInstanceOf(expectedException);
		}
	}

	static Stream<Arguments> argumentsForExpandRelation() {
		return Stream.of(
			argumentSet("null input", null, false, IllegalArgumentException.class),
			argumentSet("blank input", "", false, IllegalArgumentException.class),
			argumentSet("invalid format", "someService,someNamespace", false, IllegalArgumentException.class),
			argumentSet("valid input", "REFERRED_FROM|someIdentifier;case;someService;someNamespace|", true, null));
	}

	// measureValidatorMock is deliberately left out - create and update consult it unconditionally, so every test would
	// have to verify it. That it is consulted on both paths is asserted by createErrandWithInvalidMeasureType and
	// updateErrandWithInvalidMeasureType, and what it accepts is MeasureValidatorTest's business.
	@AfterEach
	void verifyNoMoreInteractionsOnMocks() {
		verifyNoMoreInteractions(errandRepositoryMock, revisionServiceMock, eventServiceMock, metadataLabelRepositoryMock, errandPhaseServiceMock);
	}
}
