package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachmentLink;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.DecisionAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.DecisionJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * The attachment and JSON parameter side of the decision service - the part that hands the work to the two services
 * written once for all the handling artefacts.
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
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private DecisionRepository decisionRepositoryMock;

	@Mock
	private DecisionAttachmentRepository decisionAttachmentRepositoryMock;

	@Mock
	private DecisionJsonParameterRepository decisionJsonParameterRepositoryMock;

	@Mock
	private ArtefactAttachmentService artefactAttachmentServiceMock;

	@Mock
	private ArtefactJsonParameterService artefactJsonParameterServiceMock;

	@Mock
	private InvestigationRepository investigationRepositoryMock;

	@Mock
	private DecisionValidator decisionValidatorMock;

	@Mock
	private AccessControlService accessControlServiceMock;

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
		when(artefactAttachmentServiceMock.uploadAndLink(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(file), eq(0), any()))
			.thenReturn(ATTACHMENT_ID);

		// Act
		final var result = service.createDecisionAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, file, 0);

		// Verify - the collection is created on the way, so the link has somewhere to go
		assertThat(result).isEqualTo(ATTACHMENT_ID);
		assertThat(entity.getAttachments()).isNotNull();
	}

	@Test
	void linkDecisionAttachment() {

		// Arrange
		mockErrand();
		mockDecision();
		when(artefactAttachmentServiceMock.link(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(ATTACHMENT_ID), eq(2), any()))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID));

		// Act
		final var result = service.linkDecisionAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, ATTACHMENT_ID,
			ArtefactAttachmentLink.create().withSortOrder(2));

		// Verify
		assertThat(result.getAttachmentId()).isEqualTo(ATTACHMENT_ID);
	}

	@Test
	void updateDecisionAttachment() {

		// Arrange
		mockErrand();
		mockDecision();
		final var body = ArtefactAttachmentLink.create().withSortOrder(4);
		when(artefactAttachmentServiceMock.update(eq(ATTACHMENT_ID), eq(body), any())).thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID).withSortOrder(4));

		// Act
		final var result = service.updateDecisionAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, ATTACHMENT_ID, body);

		// Verify
		assertThat(result.getSortOrder()).isEqualTo(4);
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
	}

	@Test
	void readDecisionJsonParameters() {

		// Arrange
		mockDecision();
		when(artefactJsonParameterServiceMock.readAll(any())).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act
		final var result = service.readDecisionJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID);

		// Verify
		assertThat(result).hasSize(1);
	}

	@Test
	void readDecisionJsonParameter() {

		// Arrange
		mockDecision();
		when(artefactJsonParameterServiceMock.read(any(), eq(KEY))).thenReturn(JsonParameter.create().withKey(KEY));

		// Act
		final var result = service.readDecisionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, KEY);

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
	}

	@Test
	void updateDecisionJsonParameter() {

		// Arrange
		final var errandEntity = mockErrand();
		final var entity = mockDecision();
		final var body = JsonParameter.create().withKey(KEY);
		when(artefactJsonParameterServiceMock.upsert(eq(errandEntity), eq(KEY), isNull(), eq(body), any(), any(), any())).thenReturn(new UpsertResult(body, true));

		// Act
		final var result = service.updateDecisionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, KEY, null, body);

		// Verify
		assertThat(result.created()).isTrue();
		assertThat(entity.getJsonParameterLinks()).isNotNull();
	}

	@Test
	void deleteDecisionJsonParameter() {

		// Arrange
		final var errandEntity = mockErrand();
		mockDecision();

		// Act
		service.deleteDecisionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, DECISION_ID, KEY, IF_MATCH);

		// Verify
		verify(artefactJsonParameterServiceMock).delete(eq(errandEntity), any(), eq(KEY), eq(IF_MATCH));
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
