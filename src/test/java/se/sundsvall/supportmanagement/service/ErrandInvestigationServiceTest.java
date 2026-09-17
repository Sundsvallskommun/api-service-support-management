package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.Investigation;
import se.sundsvall.supportmanagement.api.model.errand.InvestigationSection;
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.model.enums.SectionAssessment;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

/**
 * The investigation and its sections. What they carry - attachments and JSON parameters - is tested in
 * {@link ErrandInvestigationServiceArtefactTest}.
 */
@ExtendWith(MockitoExtension.class)
class ErrandInvestigationServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String INVESTIGATION_ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String OTHER_INVESTIGATION_ID = "4f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String SECTION_ID = "7f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String OTHER_SECTION_ID = "6f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String SECTION_KEY = "financial";
	private static final String OTHER_SECTION_KEY = "personal";
	private static final String IF_MATCH = "\"3\"";
	private static final String CALLER = "jo12doe";

	@Mock
	private InvestigationRepository investigationRepositoryMock;

	@Mock
	private ArtefactAttachmentService artefactAttachmentServiceMock;

	@Mock
	private ArtefactJsonParameterService artefactJsonParameterServiceMock;

	@Mock
	private DecisionValidator decisionValidatorMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private EntityManager entityManagerMock;

	@Captor
	private ArgumentCaptor<InvestigationEntity> investigationCaptor;

	@InjectMocks
	private ErrandInvestigationService service;

	/**
	 * The identifier is bound to the thread, which the test classes run before this one share.
	 */
	@BeforeEach
	@AfterEach
	void clearIdentifier() {
		Identifier.remove();
	}

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

	private static InvestigationSectionEntity sectionEntity(final String id, final String sectionKey) {
		return InvestigationSectionEntity.create().withId(id).withSectionKey(sectionKey).withAssessment(SectionAssessment.PENDING);
	}

	@Test
	void createErrandInvestigation() {

		// Arrange
		final var errandEntity = mockErrand();
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(CALLER));
		final var investigation = Investigation.create()
			.withType("SUITABILITY")
			.withStatus("ACTIVE")
			.withTitle("Investigation of suitability")
			.withRecommendation("APPROVAL");
		when(investigationRepositoryMock.save(any(InvestigationEntity.class))).thenAnswer(invocation -> invocation.<InvestigationEntity>getArgument(0).withId(INVESTIGATION_ID));

		// Act
		final var result = service.createErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, investigation);

		// Verify
		assertThat(result).isEqualTo(INVESTIGATION_ID);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.INVESTIGATION, RW);
		verifyNoMoreInteractions(accessControlServiceMock);
		verify(decisionValidatorMock).validateOutcome(NAMESPACE, MUNICIPALITY_ID, "APPROVAL");
		verify(investigationRepositoryMock).save(investigationCaptor.capture());
		final var saved = investigationCaptor.getValue();
		assertThat(saved.getErrandEntity()).isSameAs(errandEntity);
		assertThat(saved.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(saved.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(saved.getType()).isEqualTo("SUITABILITY");
		assertThat(saved.getStatus()).isEqualTo(ItemStatus.ACTIVE);
		assertThat(saved.getTitle()).isEqualTo("Investigation of suitability");
		assertThat(saved.getRecommendation()).isEqualTo("APPROVAL");
		assertThat(saved.getCreatedBy()).isEqualTo(CALLER);
	}

	/**
	 * The recommendation proposes a decision, so one the namespace has not registered as a decision outcome is refused
	 * before anything is written.
	 */
	@Test
	void createErrandInvestigationWithARecommendationTheNamespaceHasNotRegistered() {

		// Arrange
		mockErrand();
		doThrow(Problem.valueOf(BAD_REQUEST, "not a valid decision outcome")).when(decisionValidatorMock).validateOutcome(NAMESPACE, MUNICIPALITY_ID, "UNKNOWN");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.createErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, Investigation.create().withStatus("DRAFT").withRecommendation("UNKNOWN")));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		verifyNoInteractions(investigationRepositoryMock);
	}

	/**
	 * A process writes investigations as well as a caseworker does, and is recorded as the one who did.
	 * Taking only ad accounts would leave its writes unattributed.
	 */
	@Test
	void createErrandInvestigationWrittenByAProcess() {

		// Arrange
		mockErrand();
		Identifier.set(Identifier.create().withType(Identifier.Type.CUSTOM).withValue("pw-alkt"));
		when(investigationRepositoryMock.save(any(InvestigationEntity.class))).thenAnswer(invocation -> invocation.<InvestigationEntity>getArgument(0).withId(INVESTIGATION_ID));

		// Act
		service.createErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, Investigation.create().withStatus("DRAFT"));

		// Verify
		verify(investigationRepositoryMock).save(investigationCaptor.capture());
		assertThat(investigationCaptor.getValue().getCreatedBy()).isEqualTo("pw-alkt");
	}

	@Test
	void readErrandInvestigation() {

		// Arrange
		mockInvestigation()
			.withStatus(ItemStatus.ACTIVE)
			.withTitle("Investigation of suitability")
			.withVersion(3L)
			.withSections(List.of(sectionEntity(SECTION_ID, SECTION_KEY)));

		// Act
		final var result = service.readErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID);

		// Verify
		assertThat(result.getId()).isEqualTo(INVESTIGATION_ID);
		assertThat(result.getStatus()).isEqualTo("ACTIVE");
		assertThat(result.getTitle()).isEqualTo("Investigation of suitability");
		assertThat(result.getVersion()).isEqualTo(3L);
		assertThat(result.getSections()).extracting(InvestigationSection::getId).containsExactly(SECTION_ID);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.INVESTIGATION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void readErrandInvestigationNotFound() {

		// Arrange
		when(investigationRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID))
			.thenReturn(Optional.empty());

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.readErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(INVESTIGATION_ID, ERRAND_ID);
	}

	@Test
	void findErrandInvestigations() {

		// Arrange
		when(investigationRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(List.of(InvestigationEntity.create().withId(OTHER_INVESTIGATION_ID), InvestigationEntity.create().withId(INVESTIGATION_ID)));

		// Act
		final var result = service.findErrandInvestigations(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Verify
		assertThat(result).extracting(Investigation::getId).containsExactly(OTHER_INVESTIGATION_ID, INVESTIGATION_ID);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.INVESTIGATION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	/**
	 * The answer is built from what the flush hands back, since that is where the new version - and with it the
	 * ETag of the response - comes from.
	 */
	@Test
	void updateErrandInvestigation() {

		// Arrange
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(CALLER));
		final var entity = mockInvestigation().withStatus(ItemStatus.ACTIVE).withTitle("Investigation of suitability").withVersion(3L);
		when(investigationRepositoryMock.saveAndFlush(entity)).thenAnswer(_ -> entity.withVersion(4L));
		final var investigation = Investigation.create().withStatus("COMPLETED").withConclusion("The applicant meets the requirements.");

		// Act
		final var result = service.updateErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, IF_MATCH, investigation);

		// Verify
		assertThat(result.getStatus()).isEqualTo("COMPLETED");
		assertThat(result.getConclusion()).isEqualTo("The applicant meets the requirements.");
		assertThat(result.getTitle()).isEqualTo("Investigation of suitability");
		assertThat(result.getModifiedBy()).isEqualTo(CALLER);
		assertThat(result.getVersion()).isEqualTo(4L);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.INVESTIGATION, RW);
		verifyNoMoreInteractions(accessControlServiceMock);
		verify(investigationRepositoryMock).saveAndFlush(entity);
	}

	/**
	 * If-Match is opt-in. A client that sends none has its change applied rather than refused.
	 */
	@Test
	void updateErrandInvestigationWithoutIfMatch() {

		// Arrange
		final var entity = mockInvestigation().withTitle("Investigation of suitability").withVersion(3L);
		when(investigationRepositoryMock.saveAndFlush(entity)).thenReturn(entity);

		// Act
		final var result = service.updateErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, null, Investigation.create().withTitle("New title"));

		// Verify
		assertThat(result.getTitle()).isEqualTo("New title");
		verify(investigationRepositoryMock).saveAndFlush(entity);
	}

	@Test
	void updateErrandInvestigationWithAStaleIfMatch() {

		// Arrange
		final var entity = mockInvestigation().withTitle("Investigation of suitability").withVersion(4L);
		final var investigation = Investigation.create().withTitle("New title");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, IF_MATCH, investigation));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED);
		assertThat(entity.getTitle()).isEqualTo("Investigation of suitability");
		assertThat(entity.getModifiedBy()).isNull();
		verifyNoInteractions(decisionValidatorMock);
		verify(investigationRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updateErrandInvestigationWithARecommendationTheNamespaceHasNotRegistered() {

		// Arrange
		final var entity = mockInvestigation().withRecommendation("APPROVAL");
		doThrow(Problem.valueOf(BAD_REQUEST, "not a valid decision outcome")).when(decisionValidatorMock).validateOutcome(NAMESPACE, MUNICIPALITY_ID, "UNKNOWN");
		final var investigation = Investigation.create().withRecommendation("UNKNOWN");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, null, investigation));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(entity.getRecommendation()).as("the patch is not applied").isEqualTo("APPROVAL");
		verify(investigationRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updateErrandInvestigationNotFound() {

		// Arrange
		when(investigationRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID))
			.thenReturn(Optional.empty());
		final var investigation = Investigation.create().withTitle("New title");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, IF_MATCH, investigation));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(INVESTIGATION_ID, ERRAND_ID);
		verify(investigationRepositoryMock, never()).saveAndFlush(any());
	}

	/**
	 * The JSON parameters of the investigation and of its sections are theirs and go with it, so removing the
	 * investigation is all there is to it.
	 */
	@Test
	void deleteErrandInvestigation() {

		// Arrange
		final var section = sectionEntity(SECTION_ID, SECTION_KEY)
			.withJsonParameters(new ArrayList<>(List.of(InvestigationSectionJsonParameterEntity.create().withKey("sectionData"))));
		final var entity = mockInvestigation()
			.withVersion(3L)
			.withJsonParameters(new ArrayList<>(List.of(InvestigationJsonParameterEntity.create().withKey("investigationData"))))
			.withSections(new ArrayList<>(List.of(section, sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY))));

		// Act
		service.deleteErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, IF_MATCH);

		// Verify
		verify(investigationRepositoryMock).delete(entity);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.INVESTIGATION, RW);
		verify(decisionValidatorMock).validateInvestigationRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID);
		verifyNoMoreInteractions(accessControlServiceMock, decisionValidatorMock);
		verifyNoInteractions(artefactJsonParameterServiceMock);
	}

	/**
	 * The database would take the reference away from a decision that can no longer be changed, so the investigation
	 * stays - and the lock is answered before a stale version, as on the decision itself.
	 */
	@Test
	void deleteErrandInvestigationADecisionThatCanNoLongerBeChangedRestsOn() {

		// Arrange
		mockInvestigation().withVersion(4L);
		doThrow(Problem.valueOf(CONFLICT, "decision locked")).when(decisionValidatorMock).validateInvestigationRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.deleteErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, IF_MATCH));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		verify(investigationRepositoryMock, never()).delete(any());
	}

	/**
	 * If-Match is opt-in.
	 */
	@Test
	void deleteErrandInvestigationWithoutIfMatch() {

		// Arrange
		final var entity = mockInvestigation();

		// Act
		service.deleteErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, null);

		// Verify
		verify(investigationRepositoryMock).delete(entity);
	}

	@Test
	void deleteErrandInvestigationWithAStaleIfMatch() {

		// Arrange
		mockInvestigation().withVersion(4L);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.deleteErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, IF_MATCH));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED);
		verify(investigationRepositoryMock, never()).delete(any());
	}

	@Test
	void deleteErrandInvestigationNotFound() {

		// Arrange
		when(investigationRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID))
			.thenReturn(Optional.empty());

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.deleteErrandInvestigation(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, IF_MATCH));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(INVESTIGATION_ID, ERRAND_ID);
		verify(investigationRepositoryMock, never()).delete(any());
	}

	/**
	 * The id handed back is that of the very section added to the investigation, which gets it when the flush
	 * persists it - the stubbed flush stands in for that. Saving the investigation instead would merge an entity
	 * already managed, and the merge persists a copy of the new section: the copy gets the id, and the section
	 * the id is read from never does.
	 */
	@Test
	void createInvestigationSection() {

		// Arrange
		final var entity = mockInvestigation().withSections(new ArrayList<>(List.of(sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY))));
		doAnswer(_ -> {
			entity.getSections().getLast().setId(SECTION_ID);
			return null;
		}).when(investigationRepositoryMock).flush();
		final var section = InvestigationSection.create()
			.withSectionKey(SECTION_KEY)
			.withHeading("Financial conduct")
			.withSortOrder(2)
			.withAssessment("APPROVED");

		// Act
		final var result = service.createInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, section);

		// Verify
		assertThat(result).isEqualTo(SECTION_ID);
		assertThat(entity.getSections()).hasSize(2);
		final var created = entity.getSections().getLast();
		assertThat(created.getInvestigationEntity()).isSameAs(entity);
		assertThat(created.getSectionKey()).isEqualTo(SECTION_KEY);
		assertThat(created.getHeading()).isEqualTo("Financial conduct");
		assertThat(created.getAssessment()).isEqualTo(SectionAssessment.APPROVED);
		verify(investigationRepositoryMock).flush();
		verify(entityManagerMock).lock(entity, OPTIMISTIC_FORCE_INCREMENT);
		verify(investigationRepositoryMock, never()).saveAndFlush(any());
		verify(investigationRepositoryMock, never()).save(any());
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.INVESTIGATION, RW);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	/**
	 * An investigation that has never had a section has no collection to add to yet.
	 */
	@Test
	void createInvestigationSectionInAnInvestigationWithoutSections() {

		// Arrange
		final var entity = mockInvestigation();
		final var section = InvestigationSection.create().withSectionKey(SECTION_KEY).withAssessment("PENDING");

		// Act
		service.createInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, section);

		// Verify
		assertThat(entity.getSections()).extracting(InvestigationSectionEntity::getSectionKey).containsExactly(SECTION_KEY);
		verify(investigationRepositoryMock).flush();
		verify(entityManagerMock).lock(entity, OPTIMISTIC_FORCE_INCREMENT);
	}

	/**
	 * The database holds the key unique per investigation too, but there it would surface as a failed flush
	 * rather than as the conflict it is.
	 */
	@Test
	void createInvestigationSectionWithATakenKey() {

		// Arrange
		final var entity = mockInvestigation().withSections(new ArrayList<>(List.of(sectionEntity(OTHER_SECTION_ID, SECTION_KEY))));
		final var section = InvestigationSection.create().withSectionKey(SECTION_KEY).withAssessment("PENDING");

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.createInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, section));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(problem.getMessage()).contains(SECTION_KEY, INVESTIGATION_ID);
		assertThat(entity.getSections()).extracting(InvestigationSectionEntity::getId).containsExactly(OTHER_SECTION_ID);
		verify(investigationRepositoryMock, never()).flush();
	}

	@Test
	void readInvestigationSection() {

		// Arrange
		final var existingSection = sectionEntity(SECTION_ID, SECTION_KEY).withText("No payment remarks are registered.");
		mockInvestigation().withSections(List.of(sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY), existingSection));

		// Act
		final var result = service.readInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID);

		// Verify
		assertThat(result.getId()).isEqualTo(SECTION_ID);
		assertThat(result.getSectionKey()).isEqualTo(SECTION_KEY);
		assertThat(result.getText()).isEqualTo("No payment remarks are registered.");
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.INVESTIGATION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void readInvestigationSectionNotFound() {

		// Arrange
		mockInvestigation().withSections(List.of(sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY)));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.readInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(SECTION_ID, INVESTIGATION_ID);
	}

	@Test
	void findInvestigationSections() {

		// Arrange
		mockInvestigation().withSections(List.of(sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY), sectionEntity(SECTION_ID, SECTION_KEY)));

		// Act
		final var result = service.findInvestigationSections(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID);

		// Verify
		assertThat(result).extracting(InvestigationSection::getId).containsExactly(OTHER_SECTION_ID, SECTION_ID);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.INVESTIGATION, LR);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	@Test
	void updateInvestigationSection() {

		// Arrange
		final var existingSection = sectionEntity(SECTION_ID, SECTION_KEY).withHeading("Financial conduct");
		final var entity = mockInvestigation().withSections(new ArrayList<>(List.of(existingSection, sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY))));
		final var section = InvestigationSection.create().withSectionKey("premises").withAssessment("DEFICIENCY");

		// Act
		final var result = service.updateInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, section);

		// Verify
		assertThat(result.getId()).isEqualTo(SECTION_ID);
		assertThat(result.getSectionKey()).isEqualTo("premises");
		assertThat(result.getAssessment()).isEqualTo("DEFICIENCY");
		assertThat(result.getHeading()).isEqualTo("Financial conduct");
		assertThat(existingSection.getSectionKey()).isEqualTo("premises");
		verify(investigationRepositoryMock).saveAndFlush(entity);
		verify(entityManagerMock).lock(entity, OPTIMISTIC_FORCE_INCREMENT);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.INVESTIGATION, RW);
		verifyNoMoreInteractions(accessControlServiceMock);
	}

	/**
	 * A section is not in conflict with itself. Sending its own key back along with the rest of it is no key
	 * change.
	 */
	@Test
	void updateInvestigationSectionKeepingItsKey() {

		// Arrange
		final var entity = mockInvestigation().withSections(new ArrayList<>(List.of(sectionEntity(SECTION_ID, SECTION_KEY), sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY))));
		final var section = InvestigationSection.create().withSectionKey(SECTION_KEY).withText("No remarks.");

		// Act
		final var result = service.updateInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, section);

		// Verify
		assertThat(result.getSectionKey()).isEqualTo(SECTION_KEY);
		assertThat(result.getText()).isEqualTo("No remarks.");
		verify(investigationRepositoryMock).saveAndFlush(entity);
		verify(entityManagerMock).lock(entity, OPTIMISTIC_FORCE_INCREMENT);
	}

	@Test
	void updateInvestigationSectionWithoutAKey() {

		// Arrange
		final var entity = mockInvestigation().withSections(new ArrayList<>(List.of(sectionEntity(SECTION_ID, SECTION_KEY), sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY))));
		final var section = InvestigationSection.create().withAssessment("APPROVED");

		// Act
		final var result = service.updateInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, section);

		// Verify
		assertThat(result.getSectionKey()).isEqualTo(SECTION_KEY);
		assertThat(result.getAssessment()).isEqualTo("APPROVED");
		verify(investigationRepositoryMock).saveAndFlush(entity);
		verify(entityManagerMock).lock(entity, OPTIMISTIC_FORCE_INCREMENT);
	}

	@Test
	void updateInvestigationSectionToATakenKey() {

		// Arrange
		final var existingSection = sectionEntity(SECTION_ID, SECTION_KEY);
		mockInvestigation().withSections(new ArrayList<>(List.of(existingSection, sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY))));
		final var section = InvestigationSection.create().withSectionKey(OTHER_SECTION_KEY);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, section));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(problem.getMessage()).contains(OTHER_SECTION_KEY, INVESTIGATION_ID);
		assertThat(existingSection.getSectionKey()).isEqualTo(SECTION_KEY);
		verify(investigationRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void updateInvestigationSectionNotFound() {

		// Arrange
		mockInvestigation().withSections(new ArrayList<>(List.of(sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY))));
		final var section = InvestigationSection.create().withSectionKey(SECTION_KEY);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.updateInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID, section));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(SECTION_ID, INVESTIGATION_ID);
		verify(investigationRepositoryMock, never()).saveAndFlush(any());
	}

	/**
	 * The section leaves the collection of the investigation, which takes its JSON parameters with it, and the
	 * investigation moves its version before it is flushed.
	 */
	@Test
	void deleteInvestigationSection() {

		// Arrange
		final var removedSection = sectionEntity(SECTION_ID, SECTION_KEY)
			.withJsonParameters(new ArrayList<>(List.of(InvestigationSectionJsonParameterEntity.create().withKey("sectionData"))));
		final var remainingSection = sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY);
		final var entity = mockInvestigation().withSections(new ArrayList<>(List.of(removedSection, remainingSection)));

		// Act
		service.deleteInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID);

		// Verify
		final var inOrder = inOrder(entityManagerMock, investigationRepositoryMock);
		inOrder.verify(entityManagerMock).lock(entity, OPTIMISTIC_FORCE_INCREMENT);
		inOrder.verify(investigationRepositoryMock).saveAndFlush(entity);
		assertThat(entity.getSections()).containsExactly(remainingSection);
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.INVESTIGATION, RW);
		verifyNoMoreInteractions(accessControlServiceMock);
		verifyNoInteractions(artefactJsonParameterServiceMock);
	}

	@Test
	void deleteInvestigationSectionNotFound() {

		// Arrange
		mockInvestigation().withSections(new ArrayList<>(List.of(sectionEntity(OTHER_SECTION_ID, OTHER_SECTION_KEY))));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.deleteInvestigationSection(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID, SECTION_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(SECTION_ID, INVESTIGATION_ID);
		verify(investigationRepositoryMock, never()).saveAndFlush(any());
	}
}
