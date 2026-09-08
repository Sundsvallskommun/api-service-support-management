package se.sundsvall.supportmanagement.service;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ProcessError;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.AccessControlService.ErrandKeyAccess;
import se.sundsvall.supportmanagement.service.model.RevisionResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;

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
	private ErrandLabelService errandLabelServiceMock;

	@Mock
	private ErrandActionService errandActionServiceMock;

	@Mock
	private ErrandPhaseService errandPhaseServiceMock;

	@Mock
	private ErrandProcessService errandProcessServiceMock;

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

		verify(errandPhaseServiceMock).applyPhaseChange(any(ErrandEntity.class), any(), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandLabelServiceMock).validateVersions(any());
		verify(errandLabelServiceMock).settleAccessLabels(any());
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

		verify(errandPhaseServiceMock).applyPhaseChange(any(ErrandEntity.class), any(), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandLabelServiceMock).validateVersions(any());
		verify(errandLabelServiceMock).settleAccessLabels(any());
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
		verify(errandPhaseServiceMock).applyPhaseChange(any(ErrandEntity.class), any(), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandLabelServiceMock).validateVersions(any());
		verify(errandLabelServiceMock).settleAccessLabels(any());
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
		verify(errandPhaseServiceMock).applyPhaseChange(any(ErrandEntity.class), any(), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandLabelServiceMock).validateVersions(any());
		verify(errandLabelServiceMock).settleAccessLabels(any());
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

	/**
	 * The process shown on an errand is the latest one rather than a live one, so an errand whose start failed shows the
	 * failure instead of looking like an errand that never had a process at all.
	 */
	@Test
	void readErrandShowsTheProcessOfTheErrand() {
		final var entity = buildErrandEntity();
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user"));

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(accessControlServiceMock.roleBasedFieldResolver(any(), any(), any())).thenReturn(_ -> null);
		when(errandProcessServiceMock.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, List.of(ERRAND_ID))).thenReturn(Map.of(ERRAND_ID, ErrandProcess.create()
			.withProcessKey("alkt-ansokan")
			.withProcessStatus(FAILED)
			.withError(ProcessError.create().withCode("START_FAILED").withMessage("boom"))));

		final var response = service.readErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		assertThat(response.getProcess().getProcessStatus()).isEqualTo(FAILED.name());
		assertThat(response.getProcess().getError().getMessage()).isEqualTo("boom");
	}

	/**
	 * The list view asks for the processes of the whole page in one go. A lookup per errand would be invisible in a test
	 * asserting only the payload, so what is asserted here is the shape of the call rather than what it returned.
	 */
	@Test
	void findErrandsReadsTheProcessesOfThePageInOneCall() {
		final var first = buildErrandEntity().withId("errand-1");
		final var second = buildErrandEntity().withId("errand-2");
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user"));

		when(accessControlServiceMock.withAccessControl(any(), any(), any(), any(), any())).thenReturn((_, _, criteriaBuilder) -> criteriaBuilder.conjunction());
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any(), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(first, second)));
		when(accessControlServiceMock.roleBasedFieldResolver(any(), any(), any())).thenReturn(_ -> null);
		when(errandProcessServiceMock.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, List.of("errand-1", "errand-2")))
			.thenReturn(Map.of("errand-2", ErrandProcess.create().withProcessKey("alkt-ansokan").withProcessStatus(RUNNING)));

		final var matches = service.findErrands(NAMESPACE, MUNICIPALITY_ID, null, PageRequest.of(0, 20));

		assertThat(matches.getContent()).extracting(Errand::getProcess).containsExactly(null, ErrandProcess.create().withProcessKey("alkt-ansokan").withProcessStatus(RUNNING));
		verify(errandProcessServiceMock, times(1)).findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, List.of("errand-1", "errand-2"));
	}

	@Test
	void updateExistingErrand() {
		final var entity = buildErrandEntity();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(accessControlServiceMock.verifyKeyAccess(any(), any(), any(), any())).thenReturn(new ErrandKeyAccess(_ -> _ -> true, _ -> null));
		when(errandRepositoryMock.saveAndFlush(entity)).thenReturn(entity);
		when(revisionServiceMock.createErrandRevision(any())).thenReturn(new RevisionResult(previousRevisionMock, currentRevisionMock));
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, buildErrand());

		assertThat(response.getId()).isEqualTo(ERRAND_ID);
		assertThat(response.getSuspension()).extracting("suspendedFrom", "suspendedTo").containsExactlyInAnyOrder(entity.getSuspendedFrom(), entity.getSuspendedTo());

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ERRAND, RW);
		verify(errandPhaseServiceMock).applyPhaseChange(eq(entity), any(), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandLabelServiceMock).validateVersions(any());
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
		when(accessControlServiceMock.verifyKeyAccess(any(), any(), any(), any())).thenReturn(new ErrandKeyAccess(_ -> _ -> true, _ -> null));
		when(errandRepositoryMock.saveAndFlush(entity)).thenReturn(entity);
		when(contactReasonRepositoryMock.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason", NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.ofNullable(ContactReasonEntity.create().withReason("reason")));

		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, buildErrand());

		assertThat(response.getId()).isEqualTo(ERRAND_ID);

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ERRAND, RW);
		verify(errandPhaseServiceMock).applyPhaseChange(eq(entity), any(), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandLabelServiceMock).validateVersions(any());
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
		doThrow(Problem.valueOf(UNAUTHORIZED)).when(accessControlServiceMock).verifyKeyAccess(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(entity), any());

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
		when(accessControlServiceMock.verifyKeyAccess(any(), any(), any(), any())).thenReturn(new ErrandKeyAccess(_ -> _ -> true, _ -> Map.of(ErrandField.ID, Set.<String>of())));
		when(errandRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		final var response = service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, Errand.create().withTitle("new title"));

		assertThat(response.getId()).isEqualTo(ERRAND_ID);
		assertThat(response).hasAllNullFieldsOrPropertiesExcept("id");

		verify(errandRepositoryMock).saveAndFlush(entity);
		verify(errandActionServiceMock).processErrandActions(entity, OperationType.UPDATE);
		verify(revisionServiceMock).createErrandRevision(entity);
		verify(errandPhaseServiceMock).applyPhaseChange(eq(entity), any(), any(), eq(NAMESPACE), eq(MUNICIPALITY_ID));
		verify(errandLabelServiceMock).validateVersions(any());
	}

	@Test
	void createErrandWithInvalidMeasureType() {
		final var errand = buildErrand().withMeasures(List.of(Measure.create().withType("INVALID_TYPE")));

		when(stringGeneratorServiceMock.generateErrandNumber(any(String.class), any(String.class))).thenReturn("KC-23090001");
		doThrow(Problem.valueOf(BAD_REQUEST, "'INVALID_TYPE' is not a valid measure type for namespace 'namespace' and municipality with id 'municipalityId'"))
			.when(measureValidatorMock).validate(errand.getMeasures(), NAMESPACE, MUNICIPALITY_ID);

		assertThatThrownBy(() -> service.createErrand(NAMESPACE, MUNICIPALITY_ID, errand, null))
			.hasMessage("Bad Request: 'INVALID_TYPE' is not a valid measure type for namespace 'namespace' and municipality with id 'municipalityId'");

		// The request is held to the measure types before an errand is built from it, so nothing else runs.
		verify(measureValidatorMock).validate(errand.getMeasures(), NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(contactReasonRepositoryMock, errandPhaseServiceMock, errandLabelServiceMock);
	}

	@Test
	void updateErrandWithInvalidMeasureType() {
		final var entity = buildErrandEntity();
		final var errand = buildErrand().withMeasures(List.of(Measure.create().withType("INVALID_TYPE")));
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(accessControlServiceMock.verifyKeyAccess(any(), any(), any(), any())).thenReturn(new ErrandKeyAccess(_ -> _ -> true, _ -> null));
		doThrow(Problem.valueOf(BAD_REQUEST, "'INVALID_TYPE' is not a valid measure type for namespace 'namespace' and municipality with id 'municipalityId'"))
			.when(measureValidatorMock).validate(errand.getMeasures(), NAMESPACE, MUNICIPALITY_ID);

		assertThatThrownBy(() -> service.updateErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, errand))
			.hasMessage("Bad Request: 'INVALID_TYPE' is not a valid measure type for namespace 'namespace' and municipality with id 'municipalityId'");

		// The request is held to the measure types before the errand is touched, so nothing downstream of that runs.
		verify(measureValidatorMock).validate(errand.getMeasures(), NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(contactReasonRepositoryMock, errandPhaseServiceMock, errandLabelServiceMock, errandActionServiceMock);
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
	@DisplayName("Verification that the errand still carries its external tags into the delete event, since the removal empties the persistence context and a detached errand can no longer load them")
	void deleteErrandKeepsExternalTagsForTheEvent() {
		final var tag = DbExternalTag.create().withKey("caseId").withValue("case-4711");
		final var entity = buildErrandEntity().withExternalTags(new ArrayList<>(List.of(tag)));
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user"));

		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);
		when(revisionServiceMock.getLatestErrandRevision(any())).thenReturn(currentRevisionMock);
		when(errandAttachmentServiceMock.readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(emptyList());

		// Stands in for the removal emptying the persistence context: what a caller is left holding is an errand whose
		// collections are no longer there to be read.
		doAnswer(invocation -> {
			invocation.<ErrandEntity>getArgument(0).setExternalTags(null);
			return null;
		}).when(errandDataDeleterMock).deleteRelatedData(any(), any());

		service.deleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null);

		verify(revisionServiceMock).getLatestErrandRevision(same(entity));
		verify(revisionServiceMock).deleteErrandRevisions(entity.getNamespace(), entity.getMunicipalityId(), entity.getId());
		verify(errandRepositoryMock).deleteById(ERRAND_ID);
		verify(eventServiceMock).createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, currentRevisionMock, null, false, ERRAND);
		assertThat(entity.getExternalTags()).containsExactly(tag);
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
		verifyNoMoreInteractions(errandRepositoryMock, revisionServiceMock, eventServiceMock, errandLabelServiceMock, errandPhaseServiceMock);
	}
}
