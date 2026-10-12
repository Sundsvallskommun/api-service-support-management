package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
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
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * The attachment and JSON parameter side of the decision service - the part that hands the work to the two services
 * shared by all the handling artefacts.
 */
@ExtendWith(MockitoExtension.class)
class ErrandDecisionServiceArtefactTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String DECISION_ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String ATTACHMENT_ID = "8f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String KEY = "decisionData";
	private static final String IF_MATCH = "\"3\"";

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

	@Captor
	private ArgumentCaptor<Supplier<DecisionJsonParameterEntity>> jsonParameterFactoryCaptor;

	@InjectMocks
	private ErrandDecisionService service;

	private DecisionEntity mockDecision() {
		final var entity = DecisionEntity.create().withId(DECISION_ID);
		when(decisionRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID))
			.thenReturn(Optional.of(entity));
		return entity;
	}

	private ErrandEntity mockErrand() {
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);
		when(accessControlServiceMock.getErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyBoolean(), any(), any())).thenReturn(errandEntity);
		return errandEntity;
	}

	@Test
	void createDecisionAttachment() {

		// Arrange
		mockErrand();
		final var entity = mockDecision();
		final var file = new MockMultipartFile("attachment", "beslut.pdf", "application/pdf", "content".getBytes());
		when(artefactAttachmentServiceMock.uploadAndLink(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(file), any())).thenReturn(ATTACHMENT_ID);

		// Act
		final var result = service.createDecisionAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, file);

		// Verify - the collection is created on the way, so the attachment has somewhere to go
		assertThat(result).isEqualTo(ATTACHMENT_ID);
		assertThat(entity.getAttachments()).isNotNull();
		verify(artefactAttachmentServiceMock).uploadAndLink(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(file), same(entity.getAttachments()));
		verify(decisionRepositoryMock).saveAndFlush(entity);
		verify(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		verifyNoInteractions(eventServiceMock);
	}

	/**
	 * The attachments are part of the decision, and locked with it - neither added, linked nor unlinked.
	 */
	@Test
	void noAttachmentOfADecisionThatCanNoLongerBeChangedIsTouched() {

		// Arrange
		mockErrand();
		final var entity = mockDecision();
		doThrow(Problem.valueOf(CONFLICT, "decision completed")).when(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		final var file = new MockMultipartFile("attachment", "beslut.pdf", "application/pdf", "content".getBytes());

		// Act
		final var uploaded = catchThrowableOfType(ThrowableProblem.class, () -> service.createDecisionAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, file));
		final var linked = catchThrowableOfType(ThrowableProblem.class, () -> service.linkDecisionAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, ATTACHMENT_ID));
		final var unlinked = catchThrowableOfType(ThrowableProblem.class, () -> service.unlinkDecisionAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, ATTACHMENT_ID));

		// Verify
		assertThat(List.of(uploaded, linked, unlinked)).extracting(ThrowableProblem::getStatus).containsOnly(CONFLICT);
		verifyNoInteractions(artefactAttachmentServiceMock, eventServiceMock);
		verify(decisionRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void linkDecisionAttachment() {

		// Arrange
		mockErrand();
		final var entity = mockDecision();
		when(artefactAttachmentServiceMock.link(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(ATTACHMENT_ID), any()))
			.thenReturn(ErrandAttachment.create().withId(ATTACHMENT_ID));

		// Act
		final var result = service.linkDecisionAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, ATTACHMENT_ID);

		// Verify
		assertThat(result.getId()).isEqualTo(ATTACHMENT_ID);
		verify(artefactAttachmentServiceMock).link(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(ATTACHMENT_ID), same(entity.getAttachments()));
		verify(decisionRepositoryMock).saveAndFlush(entity);
		verify(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		verifyNoInteractions(eventServiceMock);
	}

	@Test
	void unlinkDecisionAttachment() {

		// Arrange
		mockErrand();
		final var entity = mockDecision();

		// Act
		service.unlinkDecisionAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, ATTACHMENT_ID);

		// Verify
		verify(artefactAttachmentServiceMock).unlink(eq(ATTACHMENT_ID), any());
		verify(decisionRepositoryMock).saveAndFlush(entity);
		verify(decisionValidatorMock).validateChangeable(ERRAND_ID, entity);
		verifyNoInteractions(eventServiceMock);
	}

	/**
	 * The parameters are the decision's, so reading them asks for the decision grant and nothing of the errand.
	 */
	@Test
	void readDecisionJsonParameters() {

		// Arrange
		final var parameters = List.of(DecisionJsonParameterEntity.create().withKey(KEY));
		mockDecision().withJsonParameters(parameters);
		when(artefactJsonParameterServiceMock.readAll(same(parameters))).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act
		final var result = service.readDecisionJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID);

		// Verify
		assertThat(result).extracting(JsonParameter::getKey).containsExactly(KEY);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.DECISION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void readDecisionJsonParameter() {

		// Arrange
		final var parameters = List.of(DecisionJsonParameterEntity.create().withKey(KEY));
		mockDecision().withJsonParameters(parameters);
		when(artefactJsonParameterServiceMock.read(same(parameters), eq(KEY))).thenReturn(JsonParameter.create().withKey(KEY));

		// Act
		final var result = service.readDecisionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, KEY);

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.DECISION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void updateDecisionJsonParameter() {

		// Arrange
		final var entity = mockDecision();
		final var body = JsonParameter.create().withKey(KEY);
		when(artefactJsonParameterServiceMock.<DecisionJsonParameterEntity>upsert(any(), any(), isNull(), same(body))).thenReturn(new UpsertResult(body, true));

		// Act
		final var result = service.updateDecisionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, null, body);

		// Verify - the collection is created on the way, so a new parameter has somewhere to go
		assertThat(result.created()).isTrue();
		assertThat(entity.getJsonParameters()).isNotNull();
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.DECISION, RW);
		verify(artefactJsonParameterServiceMock).upsert(same(entity.getJsonParameters()), jsonParameterFactoryCaptor.capture(), isNull(), same(body));
		assertThat(jsonParameterFactoryCaptor.getValue().get().getDecisionEntity()).as("a new parameter points at the decision").isSameAs(entity);
		verifyNoInteractions(decisionValidatorMock, eventServiceMock);
	}

	@Test
	void deleteDecisionJsonParameter() {

		// Arrange
		final var parameters = new ArrayList<DecisionJsonParameterEntity>();
		mockDecision().withJsonParameters(parameters);

		// Act
		service.deleteDecisionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, KEY, IF_MATCH);

		// Verify
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.DECISION, RW);
		verify(artefactJsonParameterServiceMock).delete(same(parameters), eq(KEY), eq(IF_MATCH));
		verifyNoInteractions(decisionValidatorMock, eventServiceMock);
	}

	@Test
	void aDecisionOfAnotherErrandIsNotFound() {

		// Arrange
		when(decisionRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID)).thenReturn(Optional.empty());

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.readDecisionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, KEY));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(DECISION_ID, ERRAND_ID);
	}
}
