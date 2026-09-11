package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
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
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MeasureAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.MeasureJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.StatementRepository;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
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
 * The attachment and JSON parameter side of the measure service - the part that hands the work to the two services
 * written once for all the handling artefacts.
 */
@ExtendWith(MockitoExtension.class)
class ErrandMeasureServiceArtefactTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String MEASURE_ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String ATTACHMENT_ID = "8f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String KEY = "measureData";
	private static final String IF_MATCH = "\"3\"";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private MeasureValidator measureValidatorMock;

	@Mock
	private MeasureAttachmentRepository measureAttachmentRepositoryMock;

	@Mock
	private MeasureJsonParameterRepository measureJsonParameterRepositoryMock;

	@Mock
	private DecisionRepository decisionRepositoryMock;

	@Mock
	private StatementRepository statementRepositoryMock;

	@Mock
	private ArtefactAttachmentService artefactAttachmentServiceMock;

	@Mock
	private ArtefactJsonParameterService artefactJsonParameterServiceMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private jakarta.persistence.EntityManager entityManagerMock;

	@InjectMocks
	private ErrandMeasureService service;

	private ErrandEntity errandWithMeasure(final MeasureEntity measureEntity) {
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMeasures(new ArrayList<>(List.of(measureEntity)));
		when(accessControlServiceMock.getErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyBoolean(), any(), any())).thenReturn(errandEntity);
		return errandEntity;
	}

	@Test
	void createMeasureAttachment() {

		// Arrange
		final var measureEntity = MeasureEntity.create().withId(MEASURE_ID);
		errandWithMeasure(measureEntity);
		final var file = new MockMultipartFile("attachment", "protokoll.pdf", "application/pdf", "content".getBytes());
		when(artefactAttachmentServiceMock.uploadAndLink(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(file), eq(1), any(), any(), any()))
			.thenReturn(ATTACHMENT_ID);

		// Act
		final var result = service.createMeasureAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, file, 1);

		// Verify - the collection is created on the way, so the link has somewhere to go
		assertThat(result).isEqualTo(ATTACHMENT_ID);
		assertThat(measureEntity.getAttachments()).isNotNull();
	}

	@Test
	void linkMeasureAttachment() {

		// Arrange
		final var measureEntity = MeasureEntity.create().withId(MEASURE_ID);
		errandWithMeasure(measureEntity);
		when(artefactAttachmentServiceMock.link(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(ATTACHMENT_ID), eq(2), any(), any(), any()))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID));

		// Act
		final var result = service.linkMeasureAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, ATTACHMENT_ID,
			ArtefactAttachmentLink.create().withSortOrder(2));

		// Verify
		assertThat(result.getAttachmentId()).isEqualTo(ATTACHMENT_ID);
	}

	@Test
	void updateMeasureAttachment() {

		// Arrange
		final var measureEntity = MeasureEntity.create().withId(MEASURE_ID);
		errandWithMeasure(measureEntity);
		final var body = ArtefactAttachmentLink.create().withSortOrder(4);
		when(artefactAttachmentServiceMock.update(eq(ATTACHMENT_ID), eq(body), any())).thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID).withSortOrder(4));

		// Act
		final var result = service.updateMeasureAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, ATTACHMENT_ID, body);

		// Verify
		assertThat(result.getSortOrder()).isEqualTo(4);
	}

	@Test
	void unlinkMeasureAttachment() {

		// Arrange
		final var measureEntity = MeasureEntity.create().withId(MEASURE_ID);
		final var errandEntity = errandWithMeasure(measureEntity);

		// Act
		service.unlinkMeasureAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, ATTACHMENT_ID);

		// Verify
		verify(artefactAttachmentServiceMock).unlink(eq(ATTACHMENT_ID), any());
		verify(errandsRepositoryMock).saveAndFlush(errandEntity);
	}

	@Test
	void readMeasureJsonParameters() {

		// Arrange
		errandWithMeasure(MeasureEntity.create().withId(MEASURE_ID));
		when(artefactJsonParameterServiceMock.readAll(any())).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act
		final var result = service.readMeasureJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID);

		// Verify
		assertThat(result).hasSize(1);
	}

	@Test
	void readMeasureJsonParameter() {

		// Arrange
		errandWithMeasure(MeasureEntity.create().withId(MEASURE_ID));
		when(artefactJsonParameterServiceMock.read(any(), eq(KEY))).thenReturn(JsonParameter.create().withKey(KEY));

		// Act
		final var result = service.readMeasureJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, KEY);

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
	}

	@Test
	void updateMeasureJsonParameter() {

		// Arrange
		final var measureEntity = MeasureEntity.create().withId(MEASURE_ID);
		final var errandEntity = errandWithMeasure(measureEntity);
		final var body = JsonParameter.create().withKey(KEY);
		when(artefactJsonParameterServiceMock.upsert(eq(errandEntity), eq(KEY), isNull(), eq(body), any(), any(), any()))
			.thenReturn(new UpsertResult(body, true));

		// Act
		final var result = service.updateMeasureJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, KEY, null, body);

		// Verify
		assertThat(result.created()).isTrue();
		assertThat(measureEntity.getJsonParameterLinks()).isNotNull();
	}

	@Test
	void deleteMeasureJsonParameter() {

		// Arrange
		final var errandEntity = errandWithMeasure(MeasureEntity.create().withId(MEASURE_ID));

		// Act
		service.deleteMeasureJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, KEY, IF_MATCH);

		// Verify
		verify(artefactJsonParameterServiceMock).delete(eq(errandEntity), any(), eq(KEY), eq(IF_MATCH));
	}

	@Test
	void aMeasureOfAnotherErrandIsNotFound() {

		// Arrange
		when(accessControlServiceMock.getErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyBoolean(), any(), any()))
			.thenReturn(ErrandEntity.create().withId(ERRAND_ID));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.readMeasureJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, MEASURE_ID, KEY));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(MEASURE_ID, ERRAND_ID);
	}

	/**
	 * Where the measure comes from is resolved through the errand rather than taken as an id and trusted, so a decision
	 * belonging to another errand finds nothing.
	 */
	@Test
	void aDecisionOfAnotherErrandIsNotFound() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);
		when(accessControlServiceMock.getErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyBoolean(), any(), any())).thenReturn(errandEntity);
		when(decisionRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "other-decision")).thenReturn(Optional.empty());

		final var measure = Measure.create().withType("INTERVENTION").withAddedByUser("joe01doe").withAddedByRole("MANAGER").withDecisionId("other-decision");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.createErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, measure));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains("other-decision");
	}

	@Test
	void aStatementOfAnotherErrandIsNotFound() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);
		when(accessControlServiceMock.getErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyBoolean(), any(), any())).thenReturn(errandEntity);
		when(statementRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "other-statement")).thenReturn(Optional.empty());

		final var measure = Measure.create().withType("INTERVENTION").withAddedByUser("joe01doe").withAddedByRole("MANAGER").withStatementId("other-statement");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.createErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, measure));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains("other-statement");
	}

	/**
	 * A measure that names both is hung on both, and each is resolved through the errand.
	 */
	@Test
	void provenanceIsResolvedThroughTheErrand() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);
		when(accessControlServiceMock.getErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyBoolean(), any(), any())).thenReturn(errandEntity);
		final var decisionEntity = DecisionEntity.create().withId("decision-1");
		final var statementEntity = StatementEntity.create().withId("statement-1");
		when(decisionRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "decision-1")).thenReturn(Optional.of(decisionEntity));
		when(statementRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "statement-1")).thenReturn(Optional.of(statementEntity));

		final var measure = Measure.create().withType("INTERVENTION").withAddedByUser("joe01doe").withAddedByRole("MANAGER")
			.withDecisionId("decision-1").withStatementId("statement-1");

		// Act
		service.createErrandMeasure(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, measure);

		// Verify
		assertThat(errandEntity.getMeasures()).singleElement().satisfies(entity -> {
			assertThat(entity.getDecisionEntity()).isSameAs(decisionEntity);
			assertThat(entity.getStatementEntity()).isSameAs(statementEntity);
		});
	}
}
