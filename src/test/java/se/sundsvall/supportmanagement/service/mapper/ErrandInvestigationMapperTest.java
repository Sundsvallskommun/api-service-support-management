package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Investigation;
import se.sundsvall.supportmanagement.api.model.errand.InvestigationSection;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionOutcome;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.SectionAssessment;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigation;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationSection;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationSectionEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationSections;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigations;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.updateInvestigationEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.updateInvestigationSectionEntity;

class ErrandInvestigationMapperTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Test
	void testToInvestigationEntity() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId("errandId");
		final var startedAt = now();
		final var investigation = Investigation.create()
			.withType("SUITABILITY")
			.withStatus("ACTIVE")
			.withTitle("title")
			.withInvestigatorUserId("jo12doe")
			.withStartedAt(startedAt)
			.withSummary("summary")
			.withConclusion("conclusion")
			.withRecommendation("APPROVAL")
			.withRecommendationMotivation("motivation");

		// Act
		final var result = toInvestigationEntity(investigation, errandEntity, NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getErrandEntity()).isSameAs(errandEntity);
		assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getStatus()).isEqualTo(ItemStatus.ACTIVE);
		assertThat(result.getRecommendation()).isEqualTo(DecisionOutcome.APPROVAL);
		assertThat(result.getStartedAt()).isEqualTo(startedAt);
	}

	@Test
	void testToInvestigationEntityWithoutEnums() {

		// Act
		final var result = toInvestigationEntity(Investigation.create(), ErrandEntity.create(), NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getStatus()).isNull();
		assertThat(result.getRecommendation()).isNull();
	}

	@Test
	void testUpdateInvestigationEntity() {

		// Arrange
		final var entity = InvestigationEntity.create().withStatus(ItemStatus.DRAFT).withSummary("old");

		// Act
		final var result = updateInvestigationEntity(entity, Investigation.create()
			.withStatus("COMPLETED")
			.withRecommendation("REJECTION")
			.withSummary("new"));

		// Assert
		assertThat(result).isSameAs(entity);
		assertThat(result.getStatus()).isEqualTo(ItemStatus.COMPLETED);
		assertThat(result.getRecommendation()).isEqualTo(DecisionOutcome.REJECTION);
		assertThat(result.getSummary()).isEqualTo("new");
	}

	@Test
	void testUpdateInvestigationEntityLeavesOmittedFieldsAlone() {

		// Arrange
		final var entity = InvestigationEntity.create().withStatus(ItemStatus.ACTIVE).withSummary("summary");

		// Act
		final var result = updateInvestigationEntity(entity, Investigation.create());

		// Assert
		assertThat(result.getStatus()).isEqualTo(ItemStatus.ACTIVE);
		assertThat(result.getSummary()).isEqualTo("summary");
	}

	@Test
	void testToInvestigation() {

		// Arrange
		final var entity = InvestigationEntity.create()
			.withId("id")
			.withStatus(ItemStatus.ACTIVE)
			.withRecommendation(DecisionOutcome.PARTIAL_APPROVAL)
			.withVersion(2L)
			.withSections(List.of(InvestigationSectionEntity.create().withId("sectionId").withSectionKey("financial").withAssessment(SectionAssessment.APPROVED)))
			.withAttachments(List.of(InvestigationAttachmentEntity.create()
				.withAttachmentEntity(AttachmentEntity.create().withId("attachmentId"))));

		// Act
		final var result = toInvestigation(entity);

		// Assert
		assertThat(result.getId()).isEqualTo("id");
		assertThat(result.getStatus()).isEqualTo("ACTIVE");
		assertThat(result.getRecommendation()).isEqualTo("PARTIAL_APPROVAL");
		assertThat(result.getVersion()).isEqualTo(2L);
		assertThat(result.getSections()).singleElement().satisfies(section -> {
			assertThat(section.getSectionKey()).isEqualTo("financial");
			assertThat(section.getAssessment()).isEqualTo("APPROVED");
		});
		assertThat(result.getAttachments()).singleElement()
			.satisfies(attachment -> assertThat(attachment.getAttachmentId()).isEqualTo("attachmentId"));
	}

	@Test
	void testToInvestigationWithNull() {
		assertThat(toInvestigation(null)).isNull();
	}

	@Test
	void testToInvestigations() {
		assertThat(toInvestigations(List.of(InvestigationEntity.create().withId("id")))).hasSize(1);
	}

	@Test
	void testToInvestigationsWithNull() {
		assertThat(toInvestigations(null)).isEmpty();
	}

	@Test
	void testToInvestigationSectionEntity() {

		// Arrange
		final var investigationEntity = InvestigationEntity.create().withId("investigationId");
		final var completedAt = now();
		final var section = InvestigationSection.create()
			.withSectionKey("premises")
			.withHeading("heading")
			.withSortOrder(2)
			.withAssessment("DEFICIENCY")
			.withText("text")
			.withCompletedBy("jo12doe")
			.withCompletedAt(completedAt);

		// Act
		final var result = toInvestigationSectionEntity(section, investigationEntity);

		// Assert
		assertThat(result.getInvestigationEntity()).isSameAs(investigationEntity);
		assertThat(result.getSectionKey()).isEqualTo("premises");
		assertThat(result.getAssessment()).isEqualTo(SectionAssessment.DEFICIENCY);
		assertThat(result.getCompletedAt()).isEqualTo(completedAt);
	}

	@Test
	void testToInvestigationSectionEntityWithoutAssessment() {
		assertThat(toInvestigationSectionEntity(InvestigationSection.create(), InvestigationEntity.create()).getAssessment()).isNull();
	}

	@Test
	void testUpdateInvestigationSectionEntity() {

		// Arrange
		final var entity = InvestigationSectionEntity.create().withAssessment(SectionAssessment.PENDING).withText("old");

		// Act
		final var result = updateInvestigationSectionEntity(entity, InvestigationSection.create().withAssessment("APPROVED").withText("new"));

		// Assert
		assertThat(result).isSameAs(entity);
		assertThat(result.getAssessment()).isEqualTo(SectionAssessment.APPROVED);
		assertThat(result.getText()).isEqualTo("new");
	}

	@Test
	void testUpdateInvestigationSectionEntityLeavesOmittedFieldsAlone() {

		// Arrange
		final var entity = InvestigationSectionEntity.create().withAssessment(SectionAssessment.APPROVED).withText("text");

		// Act
		final var result = updateInvestigationSectionEntity(entity, InvestigationSection.create());

		// Assert
		assertThat(result.getAssessment()).isEqualTo(SectionAssessment.APPROVED);
		assertThat(result.getText()).isEqualTo("text");
	}

	@Test
	void testToInvestigationSectionWithNull() {
		assertThat(toInvestigationSection(null)).isNull();
	}

	@Test
	void testToInvestigationSectionsWithNull() {
		assertThat(toInvestigationSections(null)).isEmpty();
	}
}
