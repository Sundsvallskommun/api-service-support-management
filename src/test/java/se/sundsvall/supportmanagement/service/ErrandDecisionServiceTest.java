package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.Decision;
import se.sundsvall.supportmanagement.api.model.errand.DecisionTerm;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionTermEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod.MANUAL;

/**
 * The decision itself and its terms - the attachment and JSON parameter side is ErrandDecisionServiceArtefactTest's.
 */
@ExtendWith(MockitoExtension.class)
class ErrandDecisionServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String DECISION_ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String OTHER_DECISION_ID = "6f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String INVESTIGATION_ID = "7f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String OTHER_INVESTIGATION_ID = "8f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String TERM_ID = "9f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String OTHER_TERM_ID = "af79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String USER = "joe01doe";
	private static final long VERSION = 3L;
	private static final String IF_MATCH = "\"3\"";
	private static final String STALE_IF_MATCH = "\"2\"";
	private static final String PROCESS_ROW_ID = "bf79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String OTHER_PROCESS_ROW_ID = "cf79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String EVENT_LOG_CREATE = "Ett beslut har lagts till i ärendet.";
	private static final String EVENT_LOG_UPDATE = "Ett beslut i ärendet har uppdaterats.";
	private static final String EVENT_LOG_CONCLUDE = "Ett beslut i ärendet har fattats.";
	private static final String EVENT_LOG_DELETE = "Ett beslut har tagits bort från ärendet.";

	@Mock
	private DecisionRepository decisionRepositoryMock;

	@Mock
	private ArtefactAttachmentService artefactAttachmentServiceMock;

	@Mock
	private ArtefactJsonParameterService artefactJsonParameterServiceMock;

	@Mock
	private InvestigationRepository investigationRepositoryMock;

	@Mock
	private ErrandProcessRepository processRepositoryMock;

	@Mock
	private DecisionValidator decisionValidatorMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private EventService eventServiceMock;

	@Mock
	private EntityManager entityManagerMock;

	@Captor
	private ArgumentCaptor<DecisionEntity> decisionEntityCaptor;

	@InjectMocks
	private ErrandDecisionService service;

	/**
	 * The identifier is bound to the thread, which the test classes run before this one share.
	 */
	@BeforeEach
	@AfterEach
	void clearIdentifier() {
		Identifier.remove();
	}

	private ErrandEntity mockErrand() {
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.DECISION, RW)).thenReturn(errandEntity);
		return errandEntity;
	}

	private DecisionEntity mockDecision() {
		final var entity = DecisionEntity.create().withId(DECISION_ID).withVersion(VERSION);
		when(decisionRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID))
			.thenReturn(Optional.of(entity));
		return entity;
	}

	/**
	 * Persisting gives the instance its id, and the service reads the id off it.
	 */
	private void mockSaveAssigningId() {
		when(decisionRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.<DecisionEntity>getArgument(0).withId(DECISION_ID));
	}

	private void mockMissingDecision() {
		when(decisionRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID))
			.thenReturn(Optional.empty());
	}

	private InvestigationEntity mockInvestigation(final String investigationId) {
		final var investigationEntity = InvestigationEntity.create().withId(investigationId);
		when(investigationRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, investigationId))
			.thenReturn(Optional.of(investigationEntity));
		return investigationEntity;
	}

	private void mockMissingInvestigation(final String investigationId) {
		when(investigationRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, investigationId))
			.thenReturn(Optional.empty());
	}

	private static Decision decision() {
		return Decision.create()
			.withType("PERMIT")
			.withStatus("DRAFT")
			.withOutcome("APPROVAL")
			.withMethod("MANUAL")
			.withDecidedBy(USER);
	}

	private static DecisionTermEntity term(final String id, final String text) {
		return DecisionTermEntity.create().withId(id).withText(text);
	}

	/**
	 * A manual decision is made by no process, so it names no process row and the process rows are not even read for it.
	 * The request is refused as a whole before the version of the errand is moved, and the event is written last, once the
	 * decision is in the database.
	 */
	@Test
	void createErrandDecision() {

		// Arrange
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(USER));
		final var errandEntity = mockErrand();
		final var investigationEntity = mockInvestigation(INVESTIGATION_ID);
		mockSaveAssigningId();

		// Act
		final var result = service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision().withInvestigationId(INVESTIGATION_ID).withErrandProcessId(PROCESS_ROW_ID));

		// Verify
		assertThat(result).isEqualTo(DECISION_ID);
		final var inOrder = inOrder(accessControlServiceMock, decisionValidatorMock, entityManagerMock, decisionRepositoryMock, eventServiceMock);
		inOrder.verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.DECISION, RW);
		inOrder.verify(decisionValidatorMock).validateChangeable(ERRAND_ID, null);
		inOrder.verify(decisionValidatorMock).validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		inOrder.verify(decisionValidatorMock).validateMethod(NAMESPACE, MUNICIPALITY_ID, MANUAL);
		inOrder.verify(decisionValidatorMock).validateOutcome(NAMESPACE, MUNICIPALITY_ID, "APPROVAL");
		inOrder.verify(entityManagerMock).lock(errandEntity, OPTIMISTIC_FORCE_INCREMENT);
		inOrder.verify(decisionRepositoryMock).saveAndFlush(decisionEntityCaptor.capture());
		inOrder.verify(eventServiceMock).createDecisionEvent(EVENT_LOG_CREATE, errandEntity, false);
		verifyNoMoreInteractions(accessControlServiceMock, decisionValidatorMock, eventServiceMock);
		verifyNoInteractions(processRepositoryMock);

		final var saved = decisionEntityCaptor.getValue();
		assertThat(saved.getErrandEntity()).isSameAs(errandEntity);
		assertThat(saved.getInvestigationEntity()).isSameAs(investigationEntity);
		assertThat(saved.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(saved.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(saved.getMethod()).isEqualTo(MANUAL);
		assertThat(saved.getOutcome()).isEqualTo("APPROVAL");
		assertThat(saved.getCreatedBy()).isEqualTo(USER);
		assertThat(saved.getErrandProcessId()).as("the process row in the body is ignored").isNull();
	}

	@Test
	void createErrandDecisionWithoutInvestigation() {

		// Arrange
		mockErrand();
		mockSaveAssigningId();

		// Act
		service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision());

		// Verify
		verify(decisionRepositoryMock).saveAndFlush(decisionEntityCaptor.capture());
		assertThat(decisionEntityCaptor.getValue().getInvestigationEntity()).isNull();
		verifyNoInteractions(investigationRepositoryMock);
	}

	@Test
	void createErrandDecisionWithoutMethod() {

		// Arrange
		mockErrand();
		mockSaveAssigningId();

		// Act
		service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision().withMethod(null));

		// Verify
		verify(decisionValidatorMock).validateMethod(NAMESPACE, MUNICIPALITY_ID, null);
		verify(decisionRepositoryMock).saveAndFlush(decisionEntityCaptor.capture());
		assertThat(decisionEntityCaptor.getValue().getMethod()).isNull();
		assertThat(decisionEntityCaptor.getValue().getErrandProcessId()).isNull();
	}

	/**
	 * A decision created already concluded is the one event the waiting process needs, and says so.
	 */
	@Test
	void createErrandDecisionAlreadyConcluded() {

		// Arrange
		final var errandEntity = mockErrand();
		mockSaveAssigningId();

		// Act
		service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision().withStatus("COMPLETED"));

		// Verify
		verify(eventServiceMock).createDecisionEvent(EVENT_LOG_CONCLUDE, errandEntity, true);
	}

	/**
	 * The service says which process made an automatic decision, and the body has no say in it.
	 */
	@Test
	void createAutomaticErrandDecisionNamesTheLiveProcess() {

		// Arrange
		mockErrand();
		mockSaveAssigningId();
		when(processRepositoryMock.findByErrandIdAndActiveMarkerIsNotNull(ERRAND_ID)).thenReturn(Optional.of(ErrandProcessEntity.create().withId(PROCESS_ROW_ID)));

		// Act
		service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision().withMethod("AUTOMATIC").withErrandProcessId(OTHER_PROCESS_ROW_ID));

		// Verify
		verify(decisionValidatorMock).validateMethod(NAMESPACE, MUNICIPALITY_ID, AUTOMATIC);
		verify(decisionRepositoryMock).saveAndFlush(decisionEntityCaptor.capture());
		assertThat(decisionEntityCaptor.getValue().getErrandProcessId()).isEqualTo(PROCESS_ROW_ID);
	}

	@Test
	void createAutomaticErrandDecisionOnAnErrandWithoutALiveProcessNamesNone() {

		// Arrange
		mockErrand();
		mockSaveAssigningId();
		when(processRepositoryMock.findByErrandIdAndActiveMarkerIsNotNull(ERRAND_ID)).thenReturn(Optional.empty());

		// Act
		service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision().withMethod("AUTOMATIC").withErrandProcessId(OTHER_PROCESS_ROW_ID));

		// Verify
		verify(decisionRepositoryMock).saveAndFlush(decisionEntityCaptor.capture());
		assertThat(decisionEntityCaptor.getValue().getErrandProcessId()).isNull();
	}

	@Test
	void createErrandDecisionOnAnErrandWhoseProcessHasRunToItsEnd() {

		// Arrange
		mockErrand();
		doThrow(Problem.valueOf(CONFLICT, "process life over")).when(decisionValidatorMock).validateChangeable(ERRAND_ID, null);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision()));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		verify(decisionValidatorMock).validateChangeable(ERRAND_ID, null);
		verifyNoMoreInteractions(decisionValidatorMock);
		verifyNoInteractions(decisionRepositoryMock, entityManagerMock, eventServiceMock);
	}

	/**
	 * The investigation is looked up through the errand, so an id belonging to another errand finds nothing and is
	 * answered as a 404 rather than written as a reference across errands.
	 */
	@Test
	void createErrandDecisionWithInvestigationOfAnotherErrand() {

		// Arrange
		mockErrand();
		mockMissingInvestigation(INVESTIGATION_ID);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision().withInvestigationId(INVESTIGATION_ID)));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(INVESTIGATION_ID, ERRAND_ID);
		verifyNoInteractions(decisionRepositoryMock);
	}

	/**
	 * What the validator accepts is DecisionValidatorTest's business. What matters here is that its rejection stops the
	 * request before anything is looked up or written.
	 */
	@Test
	void createErrandDecisionRejectedByValidator() {

		// Arrange
		mockErrand();
		doThrow(Problem.valueOf(CONFLICT, "only one decision allowed")).when(decisionValidatorMock).validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision().withInvestigationId(INVESTIGATION_ID)));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		verifyNoInteractions(investigationRepositoryMock, decisionRepositoryMock);
	}

	/**
	 * An outcome the namespace has not registered is refused before anything is looked up or written.
	 */
	@Test
	void createErrandDecisionWithAnOutcomeTheNamespaceHasNotRegistered() {

		// Arrange
		mockErrand();
		doThrow(Problem.valueOf(BAD_REQUEST, "not a valid decision outcome")).when(decisionValidatorMock).validateOutcome(NAMESPACE, MUNICIPALITY_ID, "APPROVAL");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision().withInvestigationId(INVESTIGATION_ID)));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		verifyNoInteractions(investigationRepositoryMock, decisionRepositoryMock);
	}

	/**
	 * Access is settled before the cardinality rule of the namespace is consulted, so a caller without access to the errand
	 * cannot learn from a 409 whether it already holds a decision.
	 */
	@Test
	void createErrandDecisionWithoutAccess() {

		// Arrange
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.DECISION, RW)).thenThrow(Problem.valueOf(UNAUTHORIZED, "not accessible"));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.createErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, decision()));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(UNAUTHORIZED);
		verifyNoInteractions(decisionValidatorMock, investigationRepositoryMock, decisionRepositoryMock);
	}

	@Test
	void readErrandDecision() {

		// Arrange
		mockDecision()
			.withMethod(MANUAL)
			.withOutcome("APPROVAL")
			.withInvestigationEntity(InvestigationEntity.create().withId(INVESTIGATION_ID))
			.withTerms(List.of(term(TERM_ID, "text")));

		// Act
		final var result = service.readErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID);

		// Verify
		assertThat(result.getId()).isEqualTo(DECISION_ID);
		assertThat(result.getMethod()).isEqualTo("MANUAL");
		assertThat(result.getOutcome()).isEqualTo("APPROVAL");
		assertThat(result.getInvestigationId()).isEqualTo(INVESTIGATION_ID);
		assertThat(result.getTerms()).extracting(DecisionTerm::getId).containsExactly(TERM_ID);
		assertThat(result.getVersion()).isEqualTo(VERSION);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.DECISION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void readErrandDecisionNotFound() {

		// Arrange
		mockMissingDecision();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.readErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(DECISION_ID, ERRAND_ID);
	}

	@Test
	void findErrandDecisions() {

		// Arrange
		when(decisionRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(List.of(DecisionEntity.create().withId(DECISION_ID), DecisionEntity.create().withId(OTHER_DECISION_ID)));

		// Act
		final var result = service.findErrandDecisions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Verify
		assertThat(result).extracting(Decision::getId).containsExactly(DECISION_ID, OTHER_DECISION_ID);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.DECISION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void findErrandDecisionsEmpty() {

		// Arrange
		when(decisionRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(List.of());

		// Act
		final var result = service.findErrandDecisions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Verify
		assertThat(result).isEmpty();
	}

	@Test
	void updateErrandDecision() {

		// Arrange
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(USER));
		final var errandEntity = mockErrand();
		final var investigationEntity = InvestigationEntity.create().withId(INVESTIGATION_ID);
		final var entity = mockDecision()
			.withTitle("old title")
			.withStatus(ItemStatus.DRAFT)
			.withOutcome("REJECTION")
			.withMethod(MANUAL)
			.withInvestigationEntity(investigationEntity);
		when(decisionRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		// Act
		final var result = service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH,
			Decision.create().withTitle("new title").withMethod("MANUAL").withErrandProcessId(PROCESS_ROW_ID));

		// Verify
		assertThat(result.getTitle()).isEqualTo("new title");
		assertThat(result.getOutcome()).isEqualTo("REJECTION");
		assertThat(result.getInvestigationId()).isEqualTo(INVESTIGATION_ID);
		assertThat(result.getErrandProcessId()).as("the process row in the body is ignored").isNull();
		assertThat(entity.getModifiedBy()).isEqualTo(USER);
		assertThat(entity.getInvestigationEntity()).isSameAs(investigationEntity);
		final var inOrder = inOrder(decisionValidatorMock, entityManagerMock, decisionRepositoryMock, eventServiceMock);
		inOrder.verify(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		inOrder.verify(decisionValidatorMock).validateMethod(NAMESPACE, MUNICIPALITY_ID, MANUAL);
		inOrder.verify(decisionValidatorMock).validateOutcome(NAMESPACE, MUNICIPALITY_ID, null);
		inOrder.verify(entityManagerMock).lock(errandEntity, OPTIMISTIC_FORCE_INCREMENT);
		inOrder.verify(decisionRepositoryMock).saveAndFlush(entity);
		inOrder.verify(eventServiceMock).createDecisionEvent(EVENT_LOG_UPDATE, errandEntity, false);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.DECISION, RW);
		verifyNoMoreInteractions(accessControlServiceMock, decisionValidatorMock, eventServiceMock);
		verifyNoInteractions(investigationRepositoryMock, processRepositoryMock);
	}

	/**
	 * A patch that leaves the method out is checked against the method the decision already has, so an automatic decision
	 * cannot be changed by a caseworker simply by not naming the method.
	 */
	@Test
	void updateErrandDecisionWithoutMethodIsCheckedAgainstTheStoredOne() {

		// Arrange
		final var entity = mockDecision().withMethod(AUTOMATIC).withErrandProcessId(PROCESS_ROW_ID);
		when(decisionRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		// Act
		service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, null, Decision.create().withJustification("new justification"));

		// Verify
		verify(decisionValidatorMock).validateMethod(NAMESPACE, MUNICIPALITY_ID, AUTOMATIC);
		assertThat(entity.getErrandProcessId()).as("with no live process the decision keeps the row that made it").isEqualTo(PROCESS_ROW_ID);
	}

	/**
	 * The process that writes an automatic decision now is the one that made it.
	 */
	@Test
	void updateAutomaticErrandDecisionNamesTheLiveProcess() {

		// Arrange
		final var entity = mockDecision().withMethod(AUTOMATIC).withErrandProcessId(OTHER_PROCESS_ROW_ID);
		when(decisionRepositoryMock.saveAndFlush(entity)).thenReturn(entity);
		when(processRepositoryMock.findByErrandIdAndActiveMarkerIsNotNull(ERRAND_ID)).thenReturn(Optional.of(ErrandProcessEntity.create().withId(PROCESS_ROW_ID)));

		// Act
		final var result = service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH, Decision.create().withLegalBasis("8 kap. 12 § alkohollagen"));

		// Verify
		assertThat(result.getErrandProcessId()).isEqualTo(PROCESS_ROW_ID);
	}

	/**
	 * A caseworker taking an automatic decision over makes it a manual one, which no process made.
	 */
	@Test
	void updateErrandDecisionToManualNamesNoProcess() {

		// Arrange
		final var entity = mockDecision().withMethod(AUTOMATIC).withErrandProcessId(PROCESS_ROW_ID);
		when(decisionRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		// Act
		service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH, Decision.create().withMethod("MANUAL"));

		// Verify
		assertThat(entity.getMethod()).isEqualTo(MANUAL);
		assertThat(entity.getErrandProcessId()).isNull();
		verifyNoInteractions(processRepositoryMock);
	}

	/**
	 * The patch that completes the decision is the event the waiting process needs, and says so.
	 */
	@ParameterizedTest
	@EnumSource(value = ItemStatus.class, names = "COMPLETED", mode = EXCLUDE)
	void updateErrandDecisionConcludingIt(final ItemStatus from) {

		// Arrange
		final var errandEntity = mockErrand();
		final var entity = mockDecision().withStatus(from);
		when(decisionRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		// Act
		service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH, Decision.create().withStatus("COMPLETED"));

		// Verify
		verify(eventServiceMock).createDecisionEvent(EVENT_LOG_CONCLUDE, errandEntity, true);
	}

	/**
	 * A decision completed already is concluded only once. Changing it again, which only an errand without a process
	 * allows, is an ordinary change.
	 */
	@Test
	void updateErrandDecisionAlreadyConcluded() {

		// Arrange
		final var errandEntity = mockErrand();
		final var entity = mockDecision().withStatus(ItemStatus.COMPLETED);
		when(decisionRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		// Act
		service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH, Decision.create().withStatus("COMPLETED").withTitle("new title"));

		// Verify
		verify(eventServiceMock).createDecisionEvent(EVENT_LOG_UPDATE, errandEntity, false);
	}

	/**
	 * A decision that can no longer be changed is answered with the conflict before its version is even looked at - reading
	 * it again would not make it changeable.
	 */
	@Test
	void updateErrandDecisionThatCanNoLongerBeChanged() {

		// Arrange
		final var entity = mockDecision().withTitle("old title").withStatus(ItemStatus.COMPLETED);
		doThrow(Problem.valueOf(CONFLICT, "decision completed")).when(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, STALE_IF_MATCH, Decision.create().withTitle("new title").withStatus("DRAFT")));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(entity.getTitle()).isEqualTo("old title");
		assertThat(entity.getStatus()).isEqualTo(ItemStatus.COMPLETED);
		verify(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		verifyNoMoreInteractions(decisionValidatorMock);
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
		verifyNoInteractions(entityManagerMock, eventServiceMock);
	}

	/**
	 * If-Match is opt-in: a request without it is let through rather than answered with 412.
	 */
	@Test
	void updateErrandDecisionWithoutIfMatch() {

		// Arrange
		final var entity = mockDecision();
		when(decisionRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		// Act
		final var result = service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, null, Decision.create().withTitle("new title"));

		// Verify
		assertThat(result.getTitle()).isEqualTo("new title");
		verify(decisionValidatorMock).validateMethod(NAMESPACE, MUNICIPALITY_ID, null);
	}

	@Test
	void updateErrandDecisionWithStaleIfMatch() {

		// Arrange
		final var entity = mockDecision().withTitle("old title");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, STALE_IF_MATCH, Decision.create().withTitle("new title")));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED);
		assertThat(entity.getTitle()).isEqualTo("old title");
		verify(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		verifyNoMoreInteractions(decisionValidatorMock);
		verifyNoInteractions(investigationRepositoryMock, entityManagerMock, eventServiceMock);
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updateErrandDecisionWithAnotherInvestigation() {

		// Arrange
		final var entity = mockDecision().withInvestigationEntity(InvestigationEntity.create().withId(INVESTIGATION_ID));
		final var otherInvestigationEntity = mockInvestigation(OTHER_INVESTIGATION_ID);
		when(decisionRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		// Act
		final var result = service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH, Decision.create().withInvestigationId(OTHER_INVESTIGATION_ID));

		// Verify
		assertThat(entity.getInvestigationEntity()).isSameAs(otherInvestigationEntity);
		assertThat(result.getInvestigationId()).isEqualTo(OTHER_INVESTIGATION_ID);
	}

	@Test
	void updateErrandDecisionWithInvestigationOfAnotherErrand() {

		// Arrange
		final var investigationEntity = InvestigationEntity.create().withId(INVESTIGATION_ID);
		final var entity = mockDecision().withInvestigationEntity(investigationEntity);
		mockMissingInvestigation(OTHER_INVESTIGATION_ID);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH, Decision.create().withInvestigationId(OTHER_INVESTIGATION_ID)));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(OTHER_INVESTIGATION_ID, ERRAND_ID);
		assertThat(entity.getInvestigationEntity()).isSameAs(investigationEntity);
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updateErrandDecisionRejectedByValidator() {

		// Arrange
		final var entity = mockDecision().withMethod(MANUAL);
		doThrow(Problem.valueOf(FORBIDDEN, "method not allowed for caller")).when(decisionValidatorMock).validateMethod(NAMESPACE, MUNICIPALITY_ID, AUTOMATIC);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH, Decision.create().withMethod("AUTOMATIC")));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(entity.getMethod()).isEqualTo(MANUAL);
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updateErrandDecisionWithAnOutcomeTheNamespaceHasNotRegistered() {

		// Arrange
		final var entity = mockDecision().withOutcome("APPROVAL");
		doThrow(Problem.valueOf(BAD_REQUEST, "not a valid decision outcome")).when(decisionValidatorMock).validateOutcome(NAMESPACE, MUNICIPALITY_ID, "UNKNOWN");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH, Decision.create().withOutcome("UNKNOWN")));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(entity.getOutcome()).as("the patch is not applied").isEqualTo("APPROVAL");
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updateErrandDecisionNotFound() {

		// Arrange
		mockMissingDecision();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH, Decision.create().withTitle("new title")));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(DECISION_ID, ERRAND_ID);
		verifyNoInteractions(decisionValidatorMock);
	}

	/**
	 * The JSON parameters of the decision are its own and go with it, so removing the decision is all there is to it.
	 */
	@Test
	void deleteErrandDecision() {

		// Arrange
		final var errandEntity = mockErrand();
		final var entity = mockDecision().withJsonParameters(new ArrayList<>(List.of(DecisionJsonParameterEntity.create().withKey("decisionData"))));

		// Act
		service.deleteErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH);

		// Verify
		final var inOrder = inOrder(decisionValidatorMock, entityManagerMock, decisionRepositoryMock, eventServiceMock);
		inOrder.verify(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		inOrder.verify(entityManagerMock).lock(errandEntity, OPTIMISTIC_FORCE_INCREMENT);
		inOrder.verify(decisionRepositoryMock).delete(entity);
		inOrder.verify(decisionRepositoryMock).flush();
		inOrder.verify(eventServiceMock).createDecisionEvent(EVENT_LOG_DELETE, errandEntity, false);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.DECISION, RW);
		verifyNoMoreInteractions(accessControlServiceMock, decisionValidatorMock, eventServiceMock);
		verifyNoInteractions(artefactJsonParameterServiceMock);
	}

	/**
	 * Deleting a decision that can no longer be changed would be the one change the lock is there to stop.
	 */
	@Test
	void deleteErrandDecisionThatCanNoLongerBeChanged() {

		// Arrange
		final var entity = mockDecision();
		doThrow(Problem.valueOf(CONFLICT, "process life over")).when(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.deleteErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		verify(decisionRepositoryMock, never()).delete(any());
		verifyNoInteractions(entityManagerMock, eventServiceMock);
	}

	/**
	 * The event is written whatever becomes of it: a failed event log is logged rather than failing the deletion, and a
	 * failed publication has already marked the transaction for rollback on its own.
	 */
	@Test
	void deleteErrandDecisionWhoseEventFails() {

		// Arrange
		final var errandEntity = mockErrand();
		final var entity = mockDecision();
		doThrow(new IllegalStateException("event failed")).when(eventServiceMock).createDecisionEvent(EVENT_LOG_DELETE, errandEntity, false);

		// Act
		service.deleteErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH);

		// Verify
		verify(decisionRepositoryMock).delete(entity);
	}

	@Test
	void deleteErrandDecisionWithoutIfMatch() {

		// Arrange
		final var entity = mockDecision();

		// Act
		service.deleteErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, null);

		// Verify
		verify(decisionRepositoryMock).delete(entity);
	}

	@Test
	void deleteErrandDecisionWithStaleIfMatch() {

		// Arrange
		mockDecision();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.deleteErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, STALE_IF_MATCH));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED);
		verify(decisionRepositoryMock, never()).delete(any());
		verifyNoInteractions(entityManagerMock, eventServiceMock);
	}

	@Test
	void deleteErrandDecisionNotFound() {

		// Arrange
		mockMissingDecision();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.deleteErrandDecision(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, IF_MATCH));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(DECISION_ID, ERRAND_ID);
		verify(decisionRepositoryMock, never()).delete(any());
	}

	/**
	 * The id is read off the very instance added to the decision, once the flush has persisted it. saveAndFlush would
	 * merge the already managed decision, and the merge persists a copy of the new term - the instance added here would
	 * never get its id. The stubbed flush assigns the id the way the persist does, to that instance alone.
	 */
	@Test
	void createDecisionTerm() {

		// Arrange
		final var entity = mockDecision();
		doAnswer(_ -> {
			entity.getTerms().getFirst().setId(TERM_ID);
			return null;
		}).when(decisionRepositoryMock).flush();

		// Act
		final var result = service.createDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID,
			DecisionTerm.create().withSortOrder(1).withCategory("serveringstid").withText("text"));

		// Verify
		assertThat(result).isEqualTo(TERM_ID);
		assertThat(entity.getTerms()).hasSize(1);
		final var term = entity.getTerms().getFirst();
		assertThat(term.getDecisionEntity()).isSameAs(entity);
		assertThat(term.getSortOrder()).isEqualTo(1);
		assertThat(term.getCategory()).isEqualTo("serveringstid");
		assertThat(term.getText()).isEqualTo("text");
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.DECISION, RW);
		verifyNoMoreInteractions(accessControlServiceMock);
		verify(decisionRepositoryMock).flush();
		verify(entityManagerMock).lock(entity, OPTIMISTIC_FORCE_INCREMENT);
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
		verify(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		verifyNoInteractions(eventServiceMock);
	}

	/**
	 * The terms are part of the decision, and locked with it.
	 */
	@Test
	void noTermOfADecisionThatCanNoLongerBeChangedIsWritten() {

		// Arrange
		final var term = term(TERM_ID, "text");
		final var entity = mockDecision().withTerms(new ArrayList<>(List.of(term)));
		doThrow(Problem.valueOf(CONFLICT, "decision completed")).when(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);

		// Act
		final var created = catchThrowableOfType(ThrowableProblem.class,
			() -> service.createDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, DecisionTerm.create().withText("new")));
		final var updated = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID, DecisionTerm.create().withText("changed")));
		final var deleted = catchThrowableOfType(ThrowableProblem.class,
			() -> service.deleteDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID));

		// Verify
		assertThat(List.of(created, updated, deleted)).extracting(ThrowableProblem::getStatus).containsOnly(CONFLICT);
		assertThat(entity.getTerms()).containsExactly(term);
		assertThat(term.getText()).isEqualTo("text");
		verify(decisionRepositoryMock, never()).flush();
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
		verifyNoInteractions(entityManagerMock, eventServiceMock);
	}

	@Test
	void createDecisionTermOnDecisionWithTerms() {

		// Arrange
		final var entity = mockDecision().withTerms(new ArrayList<>(List.of(term(OTHER_TERM_ID, "existing"))));
		doAnswer(_ -> {
			entity.getTerms().getLast().setId(TERM_ID);
			return null;
		}).when(decisionRepositoryMock).flush();

		// Act
		final var result = service.createDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, DecisionTerm.create().withText("new"));

		// Verify
		assertThat(result).isEqualTo(TERM_ID);
		assertThat(entity.getTerms()).extracting(DecisionTermEntity::getId, DecisionTermEntity::getText)
			.containsExactly(tuple(OTHER_TERM_ID, "existing"), tuple(TERM_ID, "new"));
		verify(entityManagerMock).lock(entity, OPTIMISTIC_FORCE_INCREMENT);
	}

	@Test
	void createDecisionTermOnMissingDecision() {

		// Arrange
		mockMissingDecision();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.createDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, DecisionTerm.create().withText("text")));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(DECISION_ID, ERRAND_ID);
		verify(decisionRepositoryMock, never()).flush();
	}

	@Test
	void findDecisionTerms() {

		// Arrange
		mockDecision().withTerms(List.of(term(TERM_ID, "first"), term(OTHER_TERM_ID, "second")));

		// Act
		final var result = service.findDecisionTerms(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID);

		// Verify
		assertThat(result).extracting(DecisionTerm::getId, DecisionTerm::getText).containsExactly(tuple(TERM_ID, "first"), tuple(OTHER_TERM_ID, "second"));
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.DECISION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void findDecisionTermsWithoutTerms() {

		// Arrange
		mockDecision();

		// Act
		final var result = service.findDecisionTerms(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID);

		// Verify
		assertThat(result).isEmpty();
	}

	@Test
	void readDecisionTerm() {

		// Arrange
		mockDecision().withTerms(List.of(term(OTHER_TERM_ID, "first"), term(TERM_ID, "second")));

		// Act
		final var result = service.readDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID);

		// Verify
		assertThat(result.getId()).isEqualTo(TERM_ID);
		assertThat(result.getText()).isEqualTo("second");
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.DECISION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void readDecisionTermNotFound() {

		// Arrange
		mockDecision().withTerms(List.of(term(OTHER_TERM_ID, "first")));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.readDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(TERM_ID, DECISION_ID);
	}

	@Test
	void readDecisionTermOfDecisionWithoutTerms() {

		// Arrange
		mockDecision();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.readDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(TERM_ID, DECISION_ID);
	}

	@Test
	void updateDecisionTerm() {

		// Arrange
		final var term = term(TERM_ID, "old text").withSortOrder(1).withCategory("serveringstid");
		final var entity = mockDecision().withTerms(new ArrayList<>(List.of(term)));

		// Act
		final var result = service.updateDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID, DecisionTerm.create().withText("new text"));

		// Verify
		assertThat(result.getId()).isEqualTo(TERM_ID);
		assertThat(result.getText()).isEqualTo("new text");
		assertThat(result.getSortOrder()).isEqualTo(1);
		assertThat(result.getCategory()).isEqualTo("serveringstid");
		assertThat(term.getText()).isEqualTo("new text");
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.DECISION, RW);
		verifyNoMoreInteractions(accessControlServiceMock);
		verify(decisionRepositoryMock).saveAndFlush(entity);
		verify(entityManagerMock).lock(entity, OPTIMISTIC_FORCE_INCREMENT);
		verify(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		verifyNoInteractions(eventServiceMock);
	}

	@Test
	void updateDecisionTermNotFound() {

		// Arrange
		mockDecision().withTerms(new ArrayList<>(List.of(term(OTHER_TERM_ID, "text"))));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID, DecisionTerm.create().withText("new text")));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(TERM_ID, DECISION_ID);
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void deleteDecisionTerm() {

		// Arrange
		final var remainingTerm = term(OTHER_TERM_ID, "remaining");
		final var entity = mockDecision().withTerms(new ArrayList<>(List.of(term(TERM_ID, "removed"), remainingTerm)));

		// Act
		service.deleteDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID);

		// Verify
		assertThat(entity.getTerms()).containsExactly(remainingTerm);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.DECISION, RW);
		verifyNoMoreInteractions(accessControlServiceMock);
		verify(decisionRepositoryMock).saveAndFlush(entity);
		verify(entityManagerMock).lock(entity, OPTIMISTIC_FORCE_INCREMENT);
		verify(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		verifyNoInteractions(eventServiceMock);
	}

	@Test
	void deleteDecisionTermNotFound() {

		// Arrange
		final var term = term(OTHER_TERM_ID, "text");
		final var entity = mockDecision().withTerms(new ArrayList<>(List.of(term)));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.deleteDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(TERM_ID, DECISION_ID);
		assertThat(entity.getTerms()).containsExactly(term);
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
	}

	/**
	 * A decision without terms answers 404 for the term rather than failing on the missing list.
	 */
	@Test
	void deleteDecisionTermOfDecisionWithoutTerms() {

		// Arrange
		mockDecision();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.deleteDecisionTerm(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, TERM_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(TERM_ID, DECISION_ID);
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
	}
}
