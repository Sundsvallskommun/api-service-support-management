package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Investigation;
import se.sundsvall.supportmanagement.api.model.errand.InvestigationSection;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.SectionAssessment;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigation;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationParameter;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationParameters;
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
		assertThat(result.getRecommendation()).isEqualTo("APPROVAL");
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
		assertThat(result.getRecommendation()).isEqualTo("REJECTION");
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
			.withRecommendation("PARTIAL_APPROVAL")
			.withVersion(2L)
			.withSections(List.of(InvestigationSectionEntity.create().withId("sectionId").withSectionKey("financial").withAssessment(SectionAssessment.APPROVED)))
			.withAttachments(List.of(AttachmentEntity.create().withId("attachmentId")));

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
			.satisfies(attachment -> assertThat(attachment.getId()).isEqualTo("attachmentId"));
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
	void testToInvestigationEntityWithParameters() {

		// Arrange
		final var investigation = Investigation.create().withParameters(List.of(
			Parameter.create().withKey("riskLevel").withDisplayName("Risknivå").withGroup("assessment").withValues(List.of("low")),
			Parameter.create().withKey("Checked").withValues(List.of("yes")),
			Parameter.create().withKey("riskLevel").withDisplayName("ignored").withValues(List.of("medium")),
			Parameter.create().withKey("area")));

		// Act
		final var result = toInvestigationEntity(investigation, ErrandEntity.create(), NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getParameters())
			.extracting(InvestigationParameterEntity::getKey, InvestigationParameterEntity::getDisplayName, InvestigationParameterEntity::getParameterGroup,
				InvestigationParameterEntity::getValues)
			.containsExactly(
				tuple("area", null, null, List.of()),
				tuple("Checked", null, null, List.of("yes")),
				tuple("riskLevel", "Risknivå", "assessment", List.of("low", "medium")));
		assertThat(result.getParameters()).allSatisfy(parameter -> assertThat(parameter.getInvestigationEntity()).isSameAs(result));
	}

	@Test
	void testToInvestigationEntityTrimsKeysBeforeMerging() {

		// Arrange
		final var investigation = Investigation.create().withParameters(List.of(
			Parameter.create().withKey(" riskLevel ").withDisplayName("Risknivå").withValues(List.of("low")),
			Parameter.create().withKey("riskLevel").withGroup("ignored").withValues(List.of("medium"))));

		// Act
		final var result = toInvestigationEntity(investigation, ErrandEntity.create(), NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getParameters())
			.extracting(InvestigationParameterEntity::getKey, InvestigationParameterEntity::getDisplayName, InvestigationParameterEntity::getParameterGroup,
				InvestigationParameterEntity::getValues)
			.containsExactly(tuple("riskLevel", "Risknivå", null, List.of("low", "medium")));
		assertThat(investigation.getParameters().getFirst().getKey()).as("the request is left as it was").isEqualTo(" riskLevel ");
	}

	@Test
	void testToInvestigationEntityWithoutParameters() {
		assertThat(toInvestigationEntity(Investigation.create(), ErrandEntity.create(), NAMESPACE, MUNICIPALITY_ID).getParameters()).isEmpty();
	}

	@Test
	void testToInvestigationParametersOrdersKeysWhateverOrderTheyAreReadIn() {

		// Arrange
		final var entities = List.of(
			InvestigationParameterEntity.create().withKey("ärende"),
			InvestigationParameterEntity.create().withKey("zon"),
			InvestigationParameterEntity.create().withKey("Area"),
			InvestigationParameterEntity.create().withKey("Zon"));

		// Act
		final var result = toInvestigationParameters(entities);

		// Assert
		assertThat(result).extracting(Parameter::getKey).containsExactly("Area", "Zon", "zon", "ärende");
	}

	@Test
	void testUpdateInvestigationEntityReplacesParameters() {

		// Arrange
		final var entity = InvestigationEntity.create();
		final var stored = new ArrayList<>(List.of(
			InvestigationParameterEntity.create().withId("old").withInvestigationEntity(entity).withKey("riskLevel").withValues(List.of("low"))));
		entity.setParameters(stored);

		// Act
		updateInvestigationEntity(entity, Investigation.create().withParameters(List.of(
			Parameter.create().withKey("riskLevel").withValues(List.of("high")),
			Parameter.create().withKey("area").withGroup("place").withValues(List.of("north")))));

		// Assert
		assertThat(entity.getParameters()).isSameAs(stored);
		assertThat(entity.getModified()).as("the investigation is marked modified").isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getParameters())
			.extracting(InvestigationParameterEntity::getId, InvestigationParameterEntity::getKey, InvestigationParameterEntity::getParameterGroup, InvestigationParameterEntity::getValues)
			.containsExactly(
				tuple(null, "area", "place", List.of("north")),
				tuple(null, "riskLevel", null, List.of("high")));
		assertThat(entity.getParameters()).allSatisfy(parameter -> assertThat(parameter.getInvestigationEntity()).isSameAs(entity));
	}

	@Test
	void testUpdateInvestigationEntityLeavesUnchangedParametersAlone() {

		// Arrange
		final var entity = InvestigationEntity.create();
		final var parameter = InvestigationParameterEntity.create().withId("id").withInvestigationEntity(entity).withKey("riskLevel").withDisplayName("Risknivå")
			.withValues(List.of("low", "medium"));
		entity.setParameters(new ArrayList<>(List.of(parameter)));

		// Act
		updateInvestigationEntity(entity, Investigation.create().withParameters(List.of(
			Parameter.create().withKey("riskLevel").withDisplayName("Risknivå").withValues(List.of("low")),
			Parameter.create().withKey("riskLevel").withValues(List.of("medium")))));

		// Assert
		assertThat(entity.getParameters()).singleElement().isSameAs(parameter);
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void testUpdateInvestigationEntityLeavesParametersReadInAnotherOrderAlone() {

		// Arrange
		final var entity = InvestigationEntity.create();
		final var stored = List.of(
			InvestigationParameterEntity.create().withId("1").withInvestigationEntity(entity).withKey("zon").withValues(List.of("a")),
			InvestigationParameterEntity.create().withId("2").withInvestigationEntity(entity).withKey("ärende").withValues(List.of("b")),
			InvestigationParameterEntity.create().withId("3").withInvestigationEntity(entity).withKey("Zon").withValues(List.of("c")));
		entity.setParameters(new ArrayList<>(stored));

		// Act
		updateInvestigationEntity(entity, Investigation.create().withParameters(List.of(
			Parameter.create().withKey("ärende").withValues(List.of("b")),
			Parameter.create().withKey("Zon").withValues(List.of("c")),
			Parameter.create().withKey("zon").withValues(List.of("a")))));

		// Assert
		assertThat(entity.getParameters()).containsExactlyElementsOf(stored);
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void testUpdateInvestigationEntityWithEmptyParametersRemovesThemAll() {

		// Arrange
		final var entity = InvestigationEntity.create();
		entity.setParameters(new ArrayList<>(List.of(InvestigationParameterEntity.create().withKey("riskLevel").withValues(List.of("low")))));

		// Act
		updateInvestigationEntity(entity, Investigation.create().withParameters(List.of()));

		// Assert
		assertThat(entity.getParameters()).isEmpty();
		assertThat(entity.getModified()).as("the investigation is marked modified").isCloseTo(now(), within(1, SECONDS));
	}

	@Test
	void testUpdateInvestigationEntityWithParametersOnInvestigationWithoutAny() {

		// Arrange
		final var entity = InvestigationEntity.create();

		// Act
		updateInvestigationEntity(entity, Investigation.create().withParameters(List.of(Parameter.create().withKey("riskLevel").withValues(List.of("low")))));

		// Assert
		assertThat(entity.getParameters()).singleElement().satisfies(parameter -> {
			assertThat(parameter.getKey()).isEqualTo("riskLevel");
			assertThat(parameter.getValues()).containsExactly("low");
			assertThat(parameter.getInvestigationEntity()).isSameAs(entity);
		});
	}

	@Test
	void testUpdateInvestigationEntityWithEmptyParametersOnInvestigationWithoutAny() {

		// Arrange
		final var entity = InvestigationEntity.create();

		// Act
		updateInvestigationEntity(entity, Investigation.create().withParameters(List.of()));

		// Assert
		assertThat(entity.getParameters()).isNull();
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void testUpdateInvestigationEntityWithoutParametersLeavesThemAlone() {

		// Arrange
		final var parameter = InvestigationParameterEntity.create().withKey("riskLevel").withValues(List.of("low"));
		final var entity = InvestigationEntity.create().withParameters(new ArrayList<>(List.of(parameter)));

		// Act
		updateInvestigationEntity(entity, Investigation.create().withRecommendation("APPROVAL"));

		// Assert
		assertThat(entity.getParameters()).singleElement().isSameAs(parameter);
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void testToInvestigationWithParameters() {

		// Arrange
		final var entity = InvestigationEntity.create().withParameters(List.of(
			InvestigationParameterEntity.create().withId("id").withKey("riskLevel").withDisplayName("Risknivå").withParameterGroup("assessment").withValues(List.of("low", "medium"))));

		// Act
		final var result = toInvestigation(entity);

		// Assert
		assertThat(result.getParameters()).containsExactly(
			Parameter.create().withKey("riskLevel").withDisplayName("Risknivå").withGroup("assessment").withValues(List.of("low", "medium")));
	}

	@Test
	void testToInvestigationWithoutParameters() {
		assertThat(toInvestigation(InvestigationEntity.create()).getParameters()).isEmpty();
	}

	@Test
	void testToInvestigationParameterWithNull() {
		assertThat(toInvestigationParameter(null)).isNull();
	}

	@Test
	void testToInvestigationParametersWithNull() {
		assertThat(toInvestigationParameters(null)).isEmpty();
	}

	@Test
	void testToInvestigationSectionsWithNull() {
		assertThat(toInvestigationSections(null)).isEmpty();
	}
}
