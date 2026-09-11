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
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationSectionJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionEntity;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * The attachment and JSON parameter side of the investigation service, including the parameters a single section owns.
 */
@ExtendWith(MockitoExtension.class)
class ErrandInvestigationServiceArtefactTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String INVESTIGATION_ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String SECTION_ID = "7f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String ATTACHMENT_ID = "8f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String KEY = "investigationData";
	private static final String IF_MATCH = "\"3\"";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private InvestigationRepository investigationRepositoryMock;

	@Mock
	private InvestigationAttachmentRepository investigationAttachmentRepositoryMock;

	@Mock
	private InvestigationJsonParameterRepository investigationJsonParameterRepositoryMock;

	@Mock
	private InvestigationSectionJsonParameterRepository investigationSectionJsonParameterRepositoryMock;

	@Mock
	private ArtefactAttachmentService artefactAttachmentServiceMock;

	@Mock
	private ArtefactJsonParameterService artefactJsonParameterServiceMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@InjectMocks
	private ErrandInvestigationService service;

	private InvestigationEntity mockInvestigation() {
		final var entity = InvestigationEntity.create().withId(INVESTIGATION_ID);
		when(investigationRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID))
			.thenReturn(Optional.of(entity));
		return entity;
	}

	private ErrandEntity mockErrand() {
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);
		when(accessControlServiceMock.getErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), anyBoolean(), any(), any())).thenReturn(errandEntity);
		return errandEntity;
	}

	@Test
	void createInvestigationAttachment() {

		// Arrange
		mockErrand();
		final var entity = mockInvestigation();
		final var file = new MockMultipartFile("attachment", "utredning.pdf", "application/pdf", "content".getBytes());
		when(artefactAttachmentServiceMock.uploadAndLink(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(file), eq(1), any()))
			.thenReturn(ATTACHMENT_ID);

		// Act
		final var result = service.createInvestigationAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, file, 1);

		// Verify
		assertThat(result).isEqualTo(ATTACHMENT_ID);
		assertThat(entity.getAttachments()).isNotNull();
	}

	@Test
	void linkInvestigationAttachment() {

		// Arrange
		mockErrand();
		mockInvestigation();
		when(artefactAttachmentServiceMock.link(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(ATTACHMENT_ID), eq(2), any()))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID));

		// Act
		final var result = service.linkInvestigationAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, ATTACHMENT_ID,
			ArtefactAttachmentLink.create().withSortOrder(2));

		// Verify
		assertThat(result.getAttachmentId()).isEqualTo(ATTACHMENT_ID);
	}

	@Test
	void updateInvestigationAttachment() {

		// Arrange
		mockErrand();
		mockInvestigation();
		final var body = ArtefactAttachmentLink.create().withSortOrder(4);
		when(artefactAttachmentServiceMock.update(eq(ATTACHMENT_ID), eq(body), any())).thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID).withSortOrder(4));

		// Act
		final var result = service.updateInvestigationAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, ATTACHMENT_ID, body);

		// Verify
		assertThat(result.getSortOrder()).isEqualTo(4);
	}

	@Test
	void unlinkInvestigationAttachment() {

		// Arrange
		mockErrand();
		final var entity = mockInvestigation();

		// Act
		service.unlinkInvestigationAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, ATTACHMENT_ID);

		// Verify
		verify(artefactAttachmentServiceMock).unlink(eq(ATTACHMENT_ID), any());
		verify(investigationRepositoryMock).saveAndFlush(entity);
	}

	@Test
	void readInvestigationJsonParameters() {

		// Arrange
		final var errandEntity = mockErrand();
		mockInvestigation();
		when(artefactJsonParameterServiceMock.readAll(same(errandEntity), any())).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act
		final var result = service.readInvestigationJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID);

		// Verify
		assertThat(result).hasSize(1);
	}

	@Test
	void readInvestigationJsonParameter() {

		// Arrange
		final var errandEntity = mockErrand();
		mockInvestigation();
		when(artefactJsonParameterServiceMock.read(same(errandEntity), any(), eq(KEY))).thenReturn(JsonParameter.create().withKey(KEY));

		// Act
		final var result = service.readInvestigationJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, KEY);

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
	}

	@Test
	void updateInvestigationJsonParameter() {

		// Arrange
		final var errandEntity = mockErrand();
		final var entity = mockInvestigation();
		final var body = JsonParameter.create().withKey(KEY);
		when(artefactJsonParameterServiceMock.upsert(eq(errandEntity), isNull(), eq(body), any(), any(), any())).thenReturn(new UpsertResult(body, true));

		// Act
		final var result = service.updateInvestigationJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, null, body);

		// Verify
		assertThat(result.created()).isTrue();
		assertThat(entity.getJsonParameterLinks()).isNotNull();
	}

	@Test
	void deleteInvestigationJsonParameter() {

		// Arrange
		final var errandEntity = mockErrand();
		mockInvestigation();

		// Act
		service.deleteInvestigationJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, KEY, IF_MATCH);

		// Verify
		verify(artefactJsonParameterServiceMock).delete(eq(errandEntity), any(), eq(KEY), eq(IF_MATCH));
	}

	private InvestigationEntity mockInvestigationWithSection() {
		final var section = InvestigationSectionEntity.create().withId(SECTION_ID).withSectionKey("financial");
		final var entity = mockInvestigation();
		entity.setSections(new ArrayList<>(List.of(section)));
		return entity;
	}

	@Test
	void readSectionJsonParameters() {

		// Arrange
		final var errandEntity = mockErrand();
		mockInvestigationWithSection();
		when(artefactJsonParameterServiceMock.readAll(same(errandEntity), any())).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act
		final var result = service.readSectionJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID);

		// Verify
		assertThat(result).hasSize(1);
	}

	@Test
	void readSectionJsonParameter() {

		// Arrange
		final var errandEntity = mockErrand();
		mockInvestigationWithSection();
		when(artefactJsonParameterServiceMock.read(same(errandEntity), any(), eq(KEY))).thenReturn(JsonParameter.create().withKey(KEY));

		// Act
		final var result = service.readSectionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, KEY);

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
	}

	@Test
	void updateSectionJsonParameter() {

		// Arrange
		final var errandEntity = mockErrand();
		final var entity = mockInvestigationWithSection();
		final var body = JsonParameter.create().withKey(KEY);
		when(artefactJsonParameterServiceMock.upsert(eq(errandEntity), isNull(), eq(body), any(), any(), any())).thenReturn(new UpsertResult(body, false));

		// Act
		final var result = service.updateSectionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, null, body);

		// Verify
		assertThat(result.created()).isFalse();
		assertThat(entity.getSections().getFirst().getJsonParameterLinks()).isNotNull();
	}

	@Test
	void deleteSectionJsonParameter() {

		// Arrange
		final var errandEntity = mockErrand();
		mockInvestigationWithSection();

		// Act
		service.deleteSectionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, KEY, IF_MATCH);

		// Verify
		verify(artefactJsonParameterServiceMock).delete(eq(errandEntity), any(), eq(KEY), eq(IF_MATCH));
	}

	@Test
	void anInvestigationOfAnotherErrandIsNotFound() {

		// Arrange
		when(investigationRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID))
			.thenReturn(Optional.empty());

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.readInvestigationJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, KEY));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(INVESTIGATION_ID, ERRAND_ID);
	}

	@Test
	void aSectionOfAnotherInvestigationIsNotFound() {

		// Arrange
		mockInvestigation();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.readSectionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, KEY));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(SECTION_ID, INVESTIGATION_ID);
	}
}
