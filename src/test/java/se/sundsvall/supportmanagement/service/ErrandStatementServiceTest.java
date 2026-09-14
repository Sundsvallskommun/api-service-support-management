package se.sundsvall.supportmanagement.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Statement;
import se.sundsvall.supportmanagement.integration.db.StatementRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;

@ExtendWith(MockitoExtension.class)
class ErrandStatementServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String STATEMENT_ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String ATTACHMENT_ID = "8f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String KEY = "statementData";
	private static final String IF_MATCH = "\"3\"";
	private static final String USER = "jo12doe";
	private static final String COUNTERPARTY_NAME = "Miljokontoret";

	@Mock
	private StatementRepository statementRepositoryMock;

	@Mock
	private StatementValidator statementValidatorMock;

	@Mock
	private ArtefactAttachmentService artefactAttachmentServiceMock;

	@Mock
	private ArtefactJsonParameterService artefactJsonParameterServiceMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Captor
	private ArgumentCaptor<StatementEntity> statementEntityCaptor;

	@Captor
	private ArgumentCaptor<Supplier<StatementJsonParameterEntity>> jsonParameterFactoryCaptor;

	@InjectMocks
	private ErrandStatementService service;

	/**
	 * The identifier is bound to the thread, which the test classes run before this one share.
	 */
	@BeforeEach
	@AfterEach
	void clearIdentifier() {
		Identifier.remove();
	}

	private StatementEntity mockStatement() {
		final var entity = StatementEntity.create().withId(STATEMENT_ID);
		when(statementRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID))
			.thenReturn(Optional.of(entity));
		return entity;
	}

	private ErrandEntity mockErrand() {
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);
		when(accessControlServiceMock.getErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyBoolean(), any(), any())).thenReturn(errandEntity);
		return errandEntity;
	}

	@Test
	void createErrandStatement() {

		// Arrange
		Identifier.set(Identifier.create().withType(AD_ACCOUNT).withValue(USER));
		final var errandEntity = mockErrand();
		final var statement = Statement.create().withStatus("DRAFT").withCounterpartyName(COUNTERPARTY_NAME);
		when(statementRepositoryMock.save(any())).thenAnswer(invocation -> invocation.<StatementEntity>getArgument(0).withId(STATEMENT_ID));

		// Act
		final var result = service.createErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, statement);

		// Verify
		assertThat(result).isEqualTo(STATEMENT_ID);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.STATEMENT, RW);

		final var inOrder = inOrder(statementValidatorMock, statementRepositoryMock);
		inOrder.verify(statementValidatorMock).validateOutcome(NAMESPACE, MUNICIPALITY_ID, null);
		inOrder.verify(statementValidatorMock).validate(statementEntityCaptor.capture(), eq(true));
		inOrder.verify(statementRepositoryMock).save(same(statementEntityCaptor.getValue()));
		assertThat(statementEntityCaptor.getValue()).satisfies(saved -> {
			assertThat(saved.getErrandEntity()).isSameAs(errandEntity);
			assertThat(saved.getNamespace()).isEqualTo(NAMESPACE);
			assertThat(saved.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
			assertThat(saved.getCreatedBy()).isEqualTo(USER);
			assertThat(saved.getStatus()).isEqualTo(ItemStatus.DRAFT);
			assertThat(saved.getCounterpartyName()).isEqualTo(COUNTERPARTY_NAME);
		});
	}

	/**
	 * What the validator accepts is StatementValidatorTest's business. What matters here is that it is consulted, and that
	 * its rejection stops the statement before it is saved.
	 */
	@Test
	void createErrandStatementRejectedByValidator() {

		// Arrange
		doThrow(Problem.valueOf(BAD_REQUEST, "A statement cannot be ACTIVE without sentAt being set")).when(statementValidatorMock).validate(any(), anyBoolean());

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.createErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, Statement.create().withStatus("ACTIVE").withCounterpartyName(COUNTERPARTY_NAME)));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		verifyNoInteractions(statementRepositoryMock);
	}

	/**
	 * An outcome the namespace has not registered is refused before the statement is built, let alone saved.
	 */
	@Test
	void createErrandStatementWithAnOutcomeTheNamespaceHasNotRegistered() {

		// Arrange
		doThrow(Problem.valueOf(BAD_REQUEST, "not a valid statement outcome")).when(statementValidatorMock).validateOutcome(NAMESPACE, MUNICIPALITY_ID, "UNKNOWN");
		final var statement = Statement.create().withStatus("COMPLETED").withOutcome("UNKNOWN").withCounterpartyName(COUNTERPARTY_NAME);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.createErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, statement));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		verify(statementValidatorMock, never()).validate(any(), anyBoolean());
		verifyNoInteractions(statementRepositoryMock);
	}

	/**
	 * A read asks whether the errand may be read and nothing more. Locking it is for the writes, and a read taking the lock
	 * would queue behind every one of them.
	 */
	@Test
	void readErrandStatement() {

		// Arrange
		mockStatement().withVersion(3L).withCounterpartyName(COUNTERPARTY_NAME);

		// Act
		final var result = service.readErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID);

		// Verify
		assertThat(result.getId()).isEqualTo(STATEMENT_ID);
		assertThat(result.getCounterpartyName()).isEqualTo(COUNTERPARTY_NAME);
		assertThat(result.getVersion()).isEqualTo(3L);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.STATEMENT, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void aStatementOfAnotherErrandIsNotFound() {

		// Arrange
		when(statementRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID)).thenReturn(Optional.empty());

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.readErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(STATEMENT_ID, ERRAND_ID);
	}

	@Test
	void findErrandStatements() {

		// Arrange
		when(statementRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(List.of(StatementEntity.create().withId("statement-1"), StatementEntity.create().withId("statement-2")));

		// Act
		final var result = service.findErrandStatements(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Verify
		assertThat(result).extracting(Statement::getId).containsExactly("statement-1", "statement-2");
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.STATEMENT, LR);
	}

	/**
	 * The validator is to judge the statement as it would be stored, so it is asked once the patch has been applied - a
	 * status patched on its own has to be weighed against the sentAt already stored.
	 */
	@Test
	void updateErrandStatement() {

		// Arrange
		Identifier.set(Identifier.create().withType(AD_ACCOUNT).withValue(USER));
		final var sentAt = OffsetDateTime.now();
		final var entity = mockStatement().withVersion(3L).withStatus(ItemStatus.DRAFT).withSentAt(sentAt).withCounterpartyName(COUNTERPARTY_NAME);
		final var statusWhenValidated = new AtomicReference<ItemStatus>();
		doAnswer(invocation -> {
			statusWhenValidated.set(invocation.<StatementEntity>getArgument(0).getStatus());
			return null;
		}).when(statementValidatorMock).validate(entity, true);
		when(statementRepositoryMock.saveAndFlush(entity)).thenAnswer(invocation -> invocation.<StatementEntity>getArgument(0).withVersion(4L));

		// Act
		final var result = service.updateErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, IF_MATCH, Statement.create().withStatus("ACTIVE"));

		// Verify
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.STATEMENT, RW);
		assertThat(statusWhenValidated).hasValue(ItemStatus.ACTIVE);
		assertThat(entity.getSentAt()).isEqualTo(sentAt);
		assertThat(entity.getCounterpartyName()).isEqualTo(COUNTERPARTY_NAME);
		assertThat(entity.getModifiedBy()).isEqualTo(USER);

		final var inOrder = inOrder(statementValidatorMock, statementRepositoryMock);
		inOrder.verify(statementValidatorMock).validate(entity, true);
		inOrder.verify(statementRepositoryMock).saveAndFlush(entity);
		assertThat(result.getStatus()).isEqualTo("ACTIVE");
		assertThat(result.getVersion()).as("the version the flush wrote, which the ETag of the response carries").isEqualTo(4L);
	}

	/**
	 * If-Match is opt-in. A request without one is let through rather than turned away.
	 */
	@Test
	void updateErrandStatementWithoutIfMatch() {

		// Arrange
		final var entity = mockStatement().withVersion(3L);
		when(statementRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		// Act
		final var result = service.updateErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, null, Statement.create().withTitle("title"));

		// Verify
		assertThat(result.getTitle()).isEqualTo("title");
		verify(statementValidatorMock).validate(entity, false);
	}

	/**
	 * An ETag that has moved on says so rather than overwriting what somebody else just wrote.
	 */
	@Test
	void updateErrandStatementWithStaleIfMatch() {

		// Arrange
		final var entity = mockStatement().withVersion(4L).withTitle("title");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, IF_MATCH, Statement.create().withTitle("new title")));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED);
		assertThat(entity.getTitle()).as("the patch is not applied").isEqualTo("title");
		verifyNoInteractions(statementValidatorMock);
		verify(statementRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updateErrandStatementRejectedByValidator() {

		// Arrange
		final var entity = mockStatement();
		doThrow(Problem.valueOf(BAD_REQUEST, "A statement cannot be ACTIVE without sentAt being set")).when(statementValidatorMock).validate(entity, true);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, null, Statement.create().withStatus("ACTIVE")));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		verify(statementRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updateErrandStatementWithAnOutcomeTheNamespaceHasNotRegistered() {

		// Arrange
		final var entity = mockStatement().withStatus(ItemStatus.COMPLETED).withOutcome("SUPPORTS");
		doThrow(Problem.valueOf(BAD_REQUEST, "not a valid statement outcome")).when(statementValidatorMock).validateOutcome(NAMESPACE, MUNICIPALITY_ID, "UNKNOWN");
		final var statement = Statement.create().withOutcome("UNKNOWN");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, null, statement));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(entity.getOutcome()).as("the patch is not applied").isEqualTo("SUPPORTS");
		verify(statementValidatorMock, never()).validate(any(), anyBoolean());
		verify(statementRepositoryMock, never()).saveAndFlush(any());
	}

	/**
	 * The JSON parameters of the statement are its own and go with it, so removing the statement is all there is to it.
	 */
	@Test
	void deleteErrandStatement() {

		// Arrange
		final var entity = mockStatement().withVersion(3L)
			.withJsonParameters(new ArrayList<>(List.of(StatementJsonParameterEntity.create().withKey(KEY))));

		// Act
		service.deleteErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, IF_MATCH);

		// Verify
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.STATEMENT, RW);
		verify(statementRepositoryMock).delete(entity);
		verifyNoInteractions(artefactJsonParameterServiceMock);
	}

	@Test
	void deleteErrandStatementWithoutIfMatch() {

		// Arrange
		final var entity = mockStatement().withVersion(3L);

		// Act
		service.deleteErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, null);

		// Verify
		verify(statementRepositoryMock).delete(entity);
	}

	@Test
	void deleteErrandStatementWithStaleIfMatch() {

		// Arrange
		mockStatement().withVersion(4L);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.deleteErrandStatement(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, IF_MATCH));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED);
		verify(statementRepositoryMock, never()).delete(any());
	}

	/**
	 * The statement is flushed once the attachment is in its collection, so that the join table is written within the call
	 * rather than at a commit the caller never sees fail.
	 */
	@Test
	void createStatementAttachment() {

		// Arrange
		final var entity = mockStatement();
		final var file = new MockMultipartFile("attachment", "remissvar.pdf", "application/pdf", "content".getBytes());
		when(artefactAttachmentServiceMock.uploadAndLink(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(file), any())).thenReturn(ATTACHMENT_ID);

		// Act
		final var result = service.createStatementAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, file);

		// Verify - the collection is created on the way, so the attachment has somewhere to go
		assertThat(result).isEqualTo(ATTACHMENT_ID);
		assertThat(entity.getAttachments()).isNotNull();
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.STATEMENT, RW);

		final var inOrder = inOrder(artefactAttachmentServiceMock, statementRepositoryMock);
		inOrder.verify(artefactAttachmentServiceMock).uploadAndLink(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(file), same(entity.getAttachments()));
		inOrder.verify(statementRepositoryMock).saveAndFlush(entity);
	}

	/**
	 * A collection the statement already has is handed on as it is. Hibernate tracks that one, and a statement whose
	 * collection has been swapped for another has its join table written again from scratch.
	 */
	@Test
	void linkStatementAttachment() {

		// Arrange
		final var attachments = new ArrayList<AttachmentEntity>();
		final var entity = mockStatement().withAttachments(attachments);
		when(artefactAttachmentServiceMock.link(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(ATTACHMENT_ID), same(attachments)))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID));

		// Act
		final var result = service.linkStatementAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, ATTACHMENT_ID);

		// Verify
		assertThat(result.getAttachmentId()).isEqualTo(ATTACHMENT_ID);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.STATEMENT, RW);

		final var inOrder = inOrder(artefactAttachmentServiceMock, statementRepositoryMock);
		inOrder.verify(artefactAttachmentServiceMock).link(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(ATTACHMENT_ID), same(attachments));
		inOrder.verify(statementRepositoryMock).saveAndFlush(entity);
	}

	@Test
	void unlinkStatementAttachment() {

		// Arrange
		final var entity = mockStatement();

		// Act
		service.unlinkStatementAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, ATTACHMENT_ID);

		// Verify
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.STATEMENT, RW);
		verify(artefactAttachmentServiceMock).unlink(eq(ATTACHMENT_ID), same(entity.getAttachments()));
		verify(statementRepositoryMock).saveAndFlush(entity);
	}

	/**
	 * The parameters are the statement's, so reading them asks for the statement grant and nothing of the errand.
	 */
	@Test
	void readStatementJsonParameters() {

		// Arrange
		final var parameters = List.of(StatementJsonParameterEntity.create().withKey(KEY));
		mockStatement().withJsonParameters(parameters);
		when(artefactJsonParameterServiceMock.readAll(same(parameters))).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act
		final var result = service.readStatementJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID);

		// Verify
		assertThat(result).extracting(JsonParameter::getKey).containsExactly(KEY);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.STATEMENT, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void readStatementJsonParameter() {

		// Arrange
		final var parameters = List.of(StatementJsonParameterEntity.create().withKey(KEY));
		mockStatement().withJsonParameters(parameters);
		when(artefactJsonParameterServiceMock.read(same(parameters), eq(KEY))).thenReturn(JsonParameter.create().withKey(KEY));

		// Act
		final var result = service.readStatementJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, KEY);

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.STATEMENT, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void updateStatementJsonParameter() {

		// Arrange
		final var entity = mockStatement();
		final var body = JsonParameter.create().withKey(KEY);
		when(artefactJsonParameterServiceMock.<StatementJsonParameterEntity>upsert(any(), any(), eq(IF_MATCH), same(body))).thenReturn(new UpsertResult(body, true));

		// Act
		final var result = service.updateStatementJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, IF_MATCH, body);

		// Verify - the collection is created on the way, so a new parameter has somewhere to go
		assertThat(result.created()).isTrue();
		assertThat(entity.getJsonParameters()).isNotNull();
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.STATEMENT, RW);
		verify(artefactJsonParameterServiceMock).upsert(same(entity.getJsonParameters()), jsonParameterFactoryCaptor.capture(), eq(IF_MATCH), same(body));
		assertThat(jsonParameterFactoryCaptor.getValue().get().getStatementEntity()).as("a new parameter points at the statement").isSameAs(entity);
	}

	@Test
	void deleteStatementJsonParameter() {

		// Arrange
		final var parameters = new ArrayList<StatementJsonParameterEntity>();
		mockStatement().withJsonParameters(parameters);

		// Act
		service.deleteStatementJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, STATEMENT_ID, KEY, IF_MATCH);

		// Verify
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.STATEMENT, RW);
		verify(artefactJsonParameterServiceMock).delete(same(parameters), eq(KEY), eq(IF_MATCH));
	}
}
