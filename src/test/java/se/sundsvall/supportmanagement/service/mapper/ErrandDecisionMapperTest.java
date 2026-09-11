package se.sundsvall.supportmanagement.service.mapper;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Decision;
import se.sundsvall.supportmanagement.api.model.errand.DecisionTerm;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionTermEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionOutcome;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecision;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTerm;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTermEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTerms;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisions;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.updateDecisionEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.updateDecisionTermEntity;

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
		assertThat(result.getOutcome()).isEqualTo(DecisionOutcome.APPROVAL);
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
		final var entity = DecisionEntity.create().withOutcome(DecisionOutcome.REJECTION).withJustification("old");

		// Act
		final var result = updateDecisionEntity(entity, Decision.create()
			.withOutcome("APPROVAL")
			.withMethod("AUTOMATIC")
			.withStatus("COMPLETED")
			.withJustification("new"));

		// Assert
		assertThat(result).isSameAs(entity);
		assertThat(result.getOutcome()).isEqualTo(DecisionOutcome.APPROVAL);
		assertThat(result.getMethod()).isEqualTo(DecisionMethod.AUTOMATIC);
		assertThat(result.getStatus()).isEqualTo(ItemStatus.COMPLETED);
		assertThat(result.getJustification()).isEqualTo("new");
	}

	@Test
	void testUpdateDecisionEntityLeavesOmittedFieldsAlone() {

		// Arrange
		final var entity = DecisionEntity.create()
			.withOutcome(DecisionOutcome.APPROVAL)
			.withMethod(DecisionMethod.MANUAL)
			.withJustification("justification");

		// Act
		final var result = updateDecisionEntity(entity, Decision.create());

		// Assert
		assertThat(result.getOutcome()).isEqualTo(DecisionOutcome.APPROVAL);
		assertThat(result.getMethod()).isEqualTo(DecisionMethod.MANUAL);
		assertThat(result.getJustification()).isEqualTo("justification");
	}

	@Test
	void testToDecision() {

		// Arrange
		final var entity = DecisionEntity.create()
			.withId("id")
			.withStatus(ItemStatus.COMPLETED)
			.withOutcome(DecisionOutcome.DISMISSAL)
			.withMethod(DecisionMethod.AUTOMATIC)
			.withInvestigationEntity(InvestigationEntity.create().withId("investigationId"))
			.withErrandProcessId("processId")
			.withVersion(4L)
			.withTerms(List.of(DecisionTermEntity.create().withId("termId").withText("text").withCategory("category")))
			.withAttachments(List.of(DecisionAttachmentEntity.create()
				.withAttachmentEntity(AttachmentEntity.create().withId("attachmentId"))));

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
			.satisfies(attachment -> assertThat(attachment.getAttachmentId()).isEqualTo("attachmentId"));
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
}
