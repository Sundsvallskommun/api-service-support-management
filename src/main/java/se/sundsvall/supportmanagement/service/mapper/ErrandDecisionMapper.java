package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.errand.Decision;
import se.sundsvall.supportmanagement.api.model.errand.DecisionTerm;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionTermEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionOutcome;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactAttachmentMapper.toArtefactAttachments;

public final class ErrandDecisionMapper {

	private ErrandDecisionMapper() {}

	/**
	 * The investigation is passed in already resolved rather than looked up from the id on the model, so that the
	 * caller has had to fetch it through the errand and a reference across errands cannot be written.
	 */
	public static DecisionEntity toDecisionEntity(final Decision decision, final ErrandEntity errandEntity, final InvestigationEntity investigationEntity, final String namespace,
		final String municipalityId) {
		return DecisionEntity.create()
			.withErrandEntity(errandEntity)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withType(decision.getType())
			.withStatus(ofNullable(decision.getStatus()).map(ItemStatus::valueOf).orElse(null))
			.withTitle(decision.getTitle())
			.withDescription(decision.getDescription())
			.withDueAt(decision.getDueAt())
			.withCompletedAt(decision.getCompletedAt())
			.withOutcome(ofNullable(decision.getOutcome()).map(DecisionOutcome::valueOf).orElse(null))
			.withMethod(ofNullable(decision.getMethod()).map(DecisionMethod::valueOf).orElse(null))
			.withDecidedBy(decision.getDecidedBy())
			.withDecidedByRole(decision.getDecidedByRole())
			.withDecidedAt(decision.getDecidedAt())
			.withLegalBasis(decision.getLegalBasis())
			.withDelegationReference(decision.getDelegationReference())
			.withJustification(decision.getJustification())
			.withAppealable(decision.getAppealable())
			.withValidFrom(decision.getValidFrom())
			.withValidTo(decision.getValidTo())
			.withInvestigationEntity(investigationEntity);
	}

	public static DecisionEntity updateDecisionEntity(final DecisionEntity entity, final Decision decision) {
		ofNullable(decision.getType()).ifPresent(entity::setType);
		ofNullable(decision.getStatus()).map(ItemStatus::valueOf).ifPresent(entity::setStatus);
		ofNullable(decision.getTitle()).ifPresent(entity::setTitle);
		ofNullable(decision.getDescription()).ifPresent(entity::setDescription);
		ofNullable(decision.getDueAt()).ifPresent(entity::setDueAt);
		ofNullable(decision.getCompletedAt()).ifPresent(entity::setCompletedAt);
		ofNullable(decision.getOutcome()).map(DecisionOutcome::valueOf).ifPresent(entity::setOutcome);
		ofNullable(decision.getMethod()).map(DecisionMethod::valueOf).ifPresent(entity::setMethod);
		ofNullable(decision.getDecidedBy()).ifPresent(entity::setDecidedBy);
		ofNullable(decision.getDecidedByRole()).ifPresent(entity::setDecidedByRole);
		ofNullable(decision.getDecidedAt()).ifPresent(entity::setDecidedAt);
		ofNullable(decision.getLegalBasis()).ifPresent(entity::setLegalBasis);
		ofNullable(decision.getDelegationReference()).ifPresent(entity::setDelegationReference);
		ofNullable(decision.getJustification()).ifPresent(entity::setJustification);
		ofNullable(decision.getAppealable()).ifPresent(entity::setAppealable);
		ofNullable(decision.getValidFrom()).ifPresent(entity::setValidFrom);
		ofNullable(decision.getValidTo()).ifPresent(entity::setValidTo);
		return entity;
	}

	public static Decision toDecision(final DecisionEntity entity) {
		return ofNullable(entity)
			.map(e -> Decision.create()
				.withId(e.getId())
				.withType(e.getType())
				.withStatus(ofNullable(e.getStatus()).map(Enum::name).orElse(null))
				.withTitle(e.getTitle())
				.withDescription(e.getDescription())
				.withDueAt(e.getDueAt())
				.withCompletedAt(e.getCompletedAt())
				.withOutcome(ofNullable(e.getOutcome()).map(Enum::name).orElse(null))
				.withMethod(ofNullable(e.getMethod()).map(Enum::name).orElse(null))
				.withDecidedBy(e.getDecidedBy())
				.withDecidedByRole(e.getDecidedByRole())
				.withDecidedAt(e.getDecidedAt())
				.withLegalBasis(e.getLegalBasis())
				.withDelegationReference(e.getDelegationReference())
				.withJustification(e.getJustification())
				.withAppealable(e.getAppealable())
				.withValidFrom(e.getValidFrom())
				.withValidTo(e.getValidTo())
				.withInvestigationId(ofNullable(e.getInvestigationEntity()).map(InvestigationEntity::getId).orElse(null))
				.withErrandProcessId(e.getErrandProcessId())
				.withTerms(toDecisionTerms(e.getTerms()))
				.withAttachments(toArtefactAttachments(e.getAttachments()))
				.withCreatedBy(e.getCreatedBy())
				.withModifiedBy(e.getModifiedBy())
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withVersion(e.getVersion()))
			.orElse(null);
	}

	public static List<Decision> toDecisions(final List<DecisionEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandDecisionMapper::toDecision)
			.toList();
	}

	public static DecisionTermEntity toDecisionTermEntity(final DecisionTerm term, final DecisionEntity decisionEntity) {
		return DecisionTermEntity.create()
			.withDecisionEntity(decisionEntity)
			.withSortOrder(term.getSortOrder())
			.withCategory(term.getCategory())
			.withText(term.getText());
	}

	public static DecisionTermEntity updateDecisionTermEntity(final DecisionTermEntity entity, final DecisionTerm term) {
		ofNullable(term.getSortOrder()).ifPresent(entity::setSortOrder);
		ofNullable(term.getCategory()).ifPresent(entity::setCategory);
		ofNullable(term.getText()).ifPresent(entity::setText);
		return entity;
	}

	public static DecisionTerm toDecisionTerm(final DecisionTermEntity entity) {
		return ofNullable(entity)
			.map(e -> DecisionTerm.create()
				.withId(e.getId())
				.withSortOrder(e.getSortOrder())
				.withCategory(e.getCategory())
				.withText(e.getText()))
			.orElse(null);
	}

	public static List<DecisionTerm> toDecisionTerms(final List<DecisionTermEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandDecisionMapper::toDecisionTerm)
			.toList();
	}
}
