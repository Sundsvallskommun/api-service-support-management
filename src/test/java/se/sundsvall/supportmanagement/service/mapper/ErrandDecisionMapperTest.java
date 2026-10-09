package se.sundsvall.supportmanagement.service.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Decision;
import se.sundsvall.supportmanagement.api.model.errand.DecisionTerm;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionTermEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecision;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTerm;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTermEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTerms;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisions;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.updateDecisionEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.updateDecisionTermEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toArtefactParameter;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toArtefactParameters;

class ErrandDecisionMapperTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Test
	void testToDecisionEntity() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId("errandId");
		final var investigationEntity = InvestigationEntity.create().withId("investigationId");
		final var decidedAt = now();
		final var decision = Decision.create()
			.withType("PERMIT")
			.withStatus("COMPLETED")
			.withOutcome("APPROVAL")
			.withMethod("MANUAL")
			.withDecidedBy("jo12doe")
			.withDecidedByRole("DELEGATE")
			.withDecidedAt(decidedAt)
			.withLegalBasis("legalBasis")
			.withDelegationReference("3.2.1")
			.withJustification("justification")
			.withAppealable(true)
			.withValidFrom(LocalDate.of(2024, 3, 1))
			.withValidTo(LocalDate.of(2025, 2, 28));

		// Act
		final var result = toDecisionEntity(decision, errandEntity, investigationEntity, NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getErrandEntity()).isSameAs(errandEntity);
		assertThat(result.getInvestigationEntity()).isSameAs(investigationEntity);
		assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getStatus()).isEqualTo(ItemStatus.COMPLETED);
		assertThat(result.getOutcome()).isEqualTo("APPROVAL");
		assertThat(result.getMethod()).isEqualTo(DecisionMethod.MANUAL);
		assertThat(result.getDecidedAt()).isEqualTo(decidedAt);
		assertThat(result.getValidFrom()).isEqualTo(LocalDate.of(2024, 3, 1));
	}

	@Test
	void testToDecisionEntityWithoutEnumsOrInvestigation() {

		// Act
		final var result = toDecisionEntity(Decision.create(), ErrandEntity.create(), null, NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getStatus()).isNull();
		assertThat(result.getOutcome()).isNull();
		assertThat(result.getMethod()).isNull();
		assertThat(result.getInvestigationEntity()).isNull();
	}

	@Test
	void testUpdateDecisionEntity() {

		// Arrange
		final var entity = DecisionEntity.create().withOutcome("REJECTION").withJustification("old");

		// Act
		final var result = updateDecisionEntity(entity, Decision.create()
			.withOutcome("APPROVAL")
			.withMethod("AUTOMATIC")
			.withStatus("COMPLETED")
			.withJustification("new"));

		// Assert
		assertThat(result).isSameAs(entity);
		assertThat(result.getOutcome()).isEqualTo("APPROVAL");
		assertThat(result.getMethod()).isEqualTo(DecisionMethod.AUTOMATIC);
		assertThat(result.getStatus()).isEqualTo(ItemStatus.COMPLETED);
		assertThat(result.getJustification()).isEqualTo("new");
	}

	@Test
	void testUpdateDecisionEntityLeavesOmittedFieldsAlone() {

		// Arrange
		final var entity = DecisionEntity.create()
			.withOutcome("APPROVAL")
			.withMethod(DecisionMethod.MANUAL)
			.withJustification("justification");

		// Act
		final var result = updateDecisionEntity(entity, Decision.create());

		// Assert
		assertThat(result.getOutcome()).isEqualTo("APPROVAL");
		assertThat(result.getMethod()).isEqualTo(DecisionMethod.MANUAL);
		assertThat(result.getJustification()).isEqualTo("justification");
	}

	@Test
	void testToDecision() {

		// Arrange
		final var entity = DecisionEntity.create()
			.withId("id")
			.withStatus(ItemStatus.COMPLETED)
			.withOutcome("DISMISSAL")
			.withMethod(DecisionMethod.AUTOMATIC)
			.withInvestigationEntity(InvestigationEntity.create().withId("investigationId"))
			.withErrandProcessId("processId")
			.withVersion(4L)
			.withTerms(List.of(DecisionTermEntity.create().withId("termId").withText("text").withCategory("category")))
			.withAttachments(List.of(AttachmentEntity.create().withId("attachmentId")));

		// Act
		final var result = toDecision(entity);

		// Assert
		assertThat(result.getId()).isEqualTo("id");
		assertThat(result.getStatus()).isEqualTo("COMPLETED");
		assertThat(result.getOutcome()).isEqualTo("DISMISSAL");
		assertThat(result.getMethod()).isEqualTo("AUTOMATIC");
		assertThat(result.getInvestigationId()).isEqualTo("investigationId");
		assertThat(result.getErrandProcessId()).isEqualTo("processId");
		assertThat(result.getVersion()).isEqualTo(4L);
		assertThat(result.getTerms()).singleElement().satisfies(term -> assertThat(term.getText()).isEqualTo("text"));
		assertThat(result.getAttachments()).singleElement()
			.satisfies(attachment -> assertThat(attachment.getId()).isEqualTo("attachmentId"));
	}

	@Test
	void testToDecisionWithNull() {
		assertThat(toDecision(null)).isNull();
	}

	@Test
	void testToDecisionWithoutInvestigation() {
		assertThat(toDecision(DecisionEntity.create().withId("id")).getInvestigationId()).isNull();
	}

	@Test
	void testToDecisions() {
		assertThat(toDecisions(List.of(DecisionEntity.create().withId("id")))).hasSize(1);
	}

	@Test
	void testToDecisionsWithNull() {
		assertThat(toDecisions(null)).isEmpty();
	}

	@Test
	void testToDecisionTermEntity() {

		// Arrange
		final var decisionEntity = DecisionEntity.create().withId("decisionId");

		// Act
		final var result = toDecisionTermEntity(DecisionTerm.create().withSortOrder(2).withCategory("category").withText("text"), decisionEntity);

		// Assert
		assertThat(result.getDecisionEntity()).isSameAs(decisionEntity);
		assertThat(result.getSortOrder()).isEqualTo(2);
		assertThat(result.getCategory()).isEqualTo("category");
		assertThat(result.getText()).isEqualTo("text");
	}

	@Test
	void testUpdateDecisionTermEntity() {

		// Arrange
		final var entity = DecisionTermEntity.create().withText("old").withSortOrder(1);

		// Act
		final var result = updateDecisionTermEntity(entity, DecisionTerm.create().withText("new"));

		// Assert
		assertThat(result).isSameAs(entity);
		assertThat(result.getText()).isEqualTo("new");
		assertThat(result.getSortOrder()).isEqualTo(1);
	}

	@Test
	void testToDecisionTermWithNull() {
		assertThat(toDecisionTerm(null)).isNull();
	}

	@Test
	void testToDecisionTermsWithNull() {
		assertThat(toDecisionTerms(null)).isEmpty();
	}

	@Test
	void testToDecisionEntityWithParameters() {

		// Arrange
		final var decision = Decision.create().withParameters(List.of(
			Parameter.create().withKey("zone").withDisplayName("Zon").withGroup("place").withValues(List.of("A")),
			Parameter.create().withKey("Beverage").withValues(List.of("beer")),
			Parameter.create().withKey("zone").withDisplayName("ignored").withValues(List.of("B")),
			Parameter.create().withKey("area")));

		// Act
		final var result = toDecisionEntity(decision, ErrandEntity.create(), null, NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getParameters())
			.extracting(DecisionParameterEntity::getKey, DecisionParameterEntity::getDisplayName, DecisionParameterEntity::getParameterGroup, DecisionParameterEntity::getValues)
			.containsExactly(
				tuple("area", null, null, List.of()),
				tuple("Beverage", null, null, List.of("beer")),
				tuple("zone", "Zon", "place", List.of("A", "B")));
		assertThat(result.getParameters()).allSatisfy(parameter -> assertThat(parameter.getDecisionEntity()).isSameAs(result));
	}

	@Test
	void testToDecisionEntityTrimsKeysBeforeMerging() {

		// Arrange
		final var decision = Decision.create().withParameters(List.of(
			Parameter.create().withKey(" zone ").withDisplayName("Zon").withValues(List.of("A")),
			Parameter.create().withKey("zone").withGroup("ignored").withValues(List.of("B"))));

		// Act
		final var result = toDecisionEntity(decision, ErrandEntity.create(), null, NAMESPACE, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getParameters())
			.extracting(DecisionParameterEntity::getKey, DecisionParameterEntity::getDisplayName, DecisionParameterEntity::getParameterGroup, DecisionParameterEntity::getValues)
			.containsExactly(tuple("zone", "Zon", null, List.of("A", "B")));
		assertThat(decision.getParameters().getFirst().getKey()).as("the request is left as it was").isEqualTo(" zone ");
	}

	@Test
	void testToDecisionParametersOrdersKeysWhateverOrderTheyAreReadIn() {

		// Arrange
		final var entities = List.of(
			DecisionParameterEntity.create().withKey("ärende"),
			DecisionParameterEntity.create().withKey("zon"),
			DecisionParameterEntity.create().withKey("Area"),
			DecisionParameterEntity.create().withKey("Zon"));

		// Act
		final var result = toArtefactParameters(entities);

		// Assert
		assertThat(result).extracting(Parameter::getKey).containsExactly("Area", "Zon", "zon", "ärende");
	}

	@Test
	void testUpdateDecisionEntityLeavesParametersReadInAnotherOrderAlone() {

		// Arrange
		final var entity = DecisionEntity.create();
		final var stored = List.of(
			DecisionParameterEntity.create().withId("1").withDecisionEntity(entity).withKey("zon").withValues(List.of("a")),
			DecisionParameterEntity.create().withId("2").withDecisionEntity(entity).withKey("ärende").withValues(List.of("b")),
			DecisionParameterEntity.create().withId("3").withDecisionEntity(entity).withKey("Zon").withValues(List.of("c")));
		entity.setParameters(new ArrayList<>(stored));

		// Act
		updateDecisionEntity(entity, Decision.create().withParameters(List.of(
			Parameter.create().withKey("ärende").withValues(List.of("b")),
			Parameter.create().withKey("Zon").withValues(List.of("c")),
			Parameter.create().withKey("zon").withValues(List.of("a")))));

		// Assert
		assertThat(entity.getParameters()).containsExactlyElementsOf(stored);
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void testToDecisionEntityWithoutParameters() {
		assertThat(toDecisionEntity(Decision.create(), ErrandEntity.create(), null, NAMESPACE, MUNICIPALITY_ID).getParameters()).isEmpty();
	}

	@Test
	void testUpdateDecisionEntityReplacesParameters() {

		// Arrange
		final var entity = DecisionEntity.create();
		final var stored = new ArrayList<>(List.of(
			DecisionParameterEntity.create().withId("old").withDecisionEntity(entity).withKey("zone").withValues(List.of("A"))));
		entity.setParameters(stored);

		// Act
		updateDecisionEntity(entity, Decision.create().withParameters(List.of(
			Parameter.create().withKey("zone").withValues(List.of("B")),
			Parameter.create().withKey("area").withGroup("place").withValues(List.of("north")))));

		// Assert
		assertThat(entity.getParameters()).isSameAs(stored);
		assertThat(entity.getModified()).as("the decision is marked modified").isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getParameters())
			.extracting(DecisionParameterEntity::getId, DecisionParameterEntity::getKey, DecisionParameterEntity::getParameterGroup, DecisionParameterEntity::getValues)
			.containsExactly(
				tuple(null, "area", "place", List.of("north")),
				tuple(null, "zone", null, List.of("B")));
		assertThat(entity.getParameters()).allSatisfy(parameter -> assertThat(parameter.getDecisionEntity()).isSameAs(entity));
	}

	@Test
	void testUpdateDecisionEntityLeavesUnchangedParametersAlone() {

		// Arrange
		final var entity = DecisionEntity.create();
		final var parameter = DecisionParameterEntity.create().withId("id").withDecisionEntity(entity).withKey("zone").withDisplayName("Zon").withValues(List.of("A", "B"));
		entity.setParameters(new ArrayList<>(List.of(parameter)));

		// Act
		updateDecisionEntity(entity, Decision.create().withParameters(List.of(
			Parameter.create().withKey("zone").withDisplayName("Zon").withValues(List.of("A")),
			Parameter.create().withKey("zone").withValues(List.of("B")))));

		// Assert
		assertThat(entity.getParameters()).singleElement().isSameAs(parameter);
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void testUpdateDecisionEntityWithEmptyParametersRemovesThemAll() {

		// Arrange
		final var entity = DecisionEntity.create();
		entity.setParameters(new ArrayList<>(List.of(DecisionParameterEntity.create().withKey("zone").withValues(List.of("A")))));

		// Act
		updateDecisionEntity(entity, Decision.create().withParameters(List.of()));

		// Assert
		assertThat(entity.getParameters()).isEmpty();
	}

	@Test
	void testUpdateDecisionEntityWithParametersOnDecisionWithoutAny() {

		// Arrange
		final var entity = DecisionEntity.create();

		// Act
		updateDecisionEntity(entity, Decision.create().withParameters(List.of(Parameter.create().withKey("zone").withValues(List.of("A")))));

		// Assert
		assertThat(entity.getParameters()).singleElement().satisfies(parameter -> {
			assertThat(parameter.getKey()).isEqualTo("zone");
			assertThat(parameter.getValues()).containsExactly("A");
			assertThat(parameter.getDecisionEntity()).isSameAs(entity);
		});
	}

	@Test
	void testUpdateDecisionEntityWithoutParametersLeavesThemAlone() {

		// Arrange
		final var parameter = DecisionParameterEntity.create().withKey("zone").withValues(List.of("A"));
		final var entity = DecisionEntity.create().withParameters(new ArrayList<>(List.of(parameter)));

		// Act
		updateDecisionEntity(entity, Decision.create().withOutcome("APPROVAL"));

		// Assert
		assertThat(entity.getParameters()).singleElement().isSameAs(parameter);
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void testToDecisionWithParameters() {

		// Arrange
		final var entity = DecisionEntity.create().withParameters(List.of(
			DecisionParameterEntity.create().withId("id").withKey("zone").withDisplayName("Zon").withParameterGroup("place").withValues(List.of("A", "B"))));

		// Act
		final var result = toDecision(entity);

		// Assert
		assertThat(result.getParameters()).containsExactly(
			Parameter.create().withKey("zone").withDisplayName("Zon").withGroup("place").withValues(List.of("A", "B")));
	}

	@Test
	void testToDecisionWithoutParameters() {
		assertThat(toDecision(DecisionEntity.create()).getParameters()).isEmpty();
	}

	@Test
	void testToDecisionParameterWithNull() {
		assertThat(toArtefactParameter(null)).isNull();
	}

	@Test
	void testToDecisionParametersWithNull() {
		assertThat(toArtefactParameters(null)).isEmpty();
	}
}
