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
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionJsonParameterEntity;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
	private InvestigationRepository investigationRepositoryMock;

	@Mock
	private ArtefactAttachmentService artefactAttachmentServiceMock;

	@Mock
	private ArtefactJsonParameterService artefactJsonParameterServiceMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Captor
	private ArgumentCaptor<Supplier<InvestigationJsonParameterEntity>> investigationParameterFactoryCaptor;

	@Captor
	private ArgumentCaptor<Supplier<InvestigationSectionJsonParameterEntity>> sectionParameterFactoryCaptor;

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

	private InvestigationSectionEntity mockSection() {
		final var section = InvestigationSectionEntity.create().withId(SECTION_ID).withSectionKey("financial");
		mockInvestigation().setSections(new ArrayList<>(List.of(section)));
		return section;
	}

	@Test
	void createInvestigationAttachment() {

		// Arrange
		mockErrand();
		final var entity = mockInvestigation();
		final var file = new MockMultipartFile("attachment", "utredning.pdf", "application/pdf", "content".getBytes());
		when(artefactAttachmentServiceMock.uploadAndLink(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(file), any())).thenReturn(ATTACHMENT_ID);

		// Act
		final var result = service.createInvestigationAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, file);

		// Verify
		assertThat(result).isEqualTo(ATTACHMENT_ID);
		assertThat(entity.getAttachments()).isNotNull();
		verify(artefactAttachmentServiceMock).uploadAndLink(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(file), same(entity.getAttachments()));
		verify(investigationRepositoryMock).saveAndFlush(entity);
	}

	@Test
	void linkInvestigationAttachment() {

		// Arrange
		mockErrand();
		final var entity = mockInvestigation();
		when(artefactAttachmentServiceMock.link(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(ATTACHMENT_ID), any()))
			.thenReturn(ArtefactAttachment.create().withAttachmentId(ATTACHMENT_ID));

		// Act
		final var result = service.linkInvestigationAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, ATTACHMENT_ID);

		// Verify
		assertThat(result.getAttachmentId()).isEqualTo(ATTACHMENT_ID);
		verify(artefactAttachmentServiceMock).link(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(ATTACHMENT_ID), same(entity.getAttachments()));
		verify(investigationRepositoryMock).saveAndFlush(entity);
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

	/**
	 * The parameters are the investigation's, so reading them asks for the investigation grant and nothing of the errand.
	 */
	@Test
	void readInvestigationJsonParameters() {

		// Arrange
		final var parameters = List.of(InvestigationJsonParameterEntity.create().withKey(KEY));
		mockInvestigation().withJsonParameters(parameters);
		when(artefactJsonParameterServiceMock.readAll(same(parameters))).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act
		final var result = service.readInvestigationJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID);

		// Verify
		assertThat(result).extracting(JsonParameter::getKey).containsExactly(KEY);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.INVESTIGATION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void readInvestigationJsonParameter() {

		// Arrange
		final var parameters = List.of(InvestigationJsonParameterEntity.create().withKey(KEY));
		mockInvestigation().withJsonParameters(parameters);
		when(artefactJsonParameterServiceMock.read(same(parameters), eq(KEY))).thenReturn(JsonParameter.create().withKey(KEY));

		// Act
		final var result = service.readInvestigationJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, KEY);

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.INVESTIGATION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void updateInvestigationJsonParameter() {

		// Arrange
		final var entity = mockInvestigation();
		final var body = JsonParameter.create().withKey(KEY);
		when(artefactJsonParameterServiceMock.<InvestigationJsonParameterEntity>upsert(any(), any(), isNull(), same(body))).thenReturn(new UpsertResult(body, true));

		// Act
		final var result = service.updateInvestigationJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, null, body);

		// Verify - the collection is created on the way, so a new parameter has somewhere to go
		assertThat(result.created()).isTrue();
		assertThat(entity.getJsonParameters()).isNotNull();
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.INVESTIGATION, RW);
		verify(artefactJsonParameterServiceMock).upsert(same(entity.getJsonParameters()), investigationParameterFactoryCaptor.capture(), isNull(), same(body));
		assertThat(investigationParameterFactoryCaptor.getValue().get().getInvestigationEntity()).as("a new parameter points at the investigation").isSameAs(entity);
	}

	@Test
	void deleteInvestigationJsonParameter() {

		// Arrange
		final var parameters = new ArrayList<InvestigationJsonParameterEntity>();
		mockInvestigation().withJsonParameters(parameters);

		// Act
		service.deleteInvestigationJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, KEY, IF_MATCH);

		// Verify
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.INVESTIGATION, RW);
		verify(artefactJsonParameterServiceMock).delete(same(parameters), eq(KEY), eq(IF_MATCH));
	}

	@Test
	void readSectionJsonParameters() {

		// Arrange
		final var parameters = List.of(InvestigationSectionJsonParameterEntity.create().withKey(KEY));
		mockSection().withJsonParameters(parameters);
		when(artefactJsonParameterServiceMock.readAll(same(parameters))).thenReturn(List.of(JsonParameter.create().withKey(KEY)));

		// Act
		final var result = service.readSectionJsonParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID);

		// Verify
		assertThat(result).extracting(JsonParameter::getKey).containsExactly(KEY);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.INVESTIGATION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void readSectionJsonParameter() {

		// Arrange
		final var parameters = List.of(InvestigationSectionJsonParameterEntity.create().withKey(KEY));
		mockSection().withJsonParameters(parameters);
		when(artefactJsonParameterServiceMock.read(same(parameters), eq(KEY))).thenReturn(JsonParameter.create().withKey(KEY));

		// Act
		final var result = service.readSectionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, KEY);

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.INVESTIGATION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void updateSectionJsonParameter() {

		// Arrange
		final var section = mockSection();
		final var body = JsonParameter.create().withKey(KEY);
		when(artefactJsonParameterServiceMock.<InvestigationSectionJsonParameterEntity>upsert(any(), any(), isNull(), same(body))).thenReturn(new UpsertResult(body, false));

		// Act
		final var result = service.updateSectionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, null, body);

		// Verify - the collection is created on the way, so a new parameter has somewhere to go
		assertThat(result.created()).isFalse();
		assertThat(section.getJsonParameters()).isNotNull();
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.INVESTIGATION, RW);
		verify(artefactJsonParameterServiceMock).upsert(same(section.getJsonParameters()), sectionParameterFactoryCaptor.capture(), isNull(), same(body));
		assertThat(sectionParameterFactoryCaptor.getValue().get().getInvestigationSectionEntity()).as("a new parameter points at the section").isSameAs(section);
	}

	@Test
	void deleteSectionJsonParameter() {

		// Arrange
		final var parameters = new ArrayList<InvestigationSectionJsonParameterEntity>();
		mockSection().withJsonParameters(parameters);

		// Act
		service.deleteSectionJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, KEY, IF_MATCH);

		// Verify
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.INVESTIGATION, RW);
		verify(artefactJsonParameterServiceMock).delete(same(parameters), eq(KEY), eq(IF_MATCH));
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
