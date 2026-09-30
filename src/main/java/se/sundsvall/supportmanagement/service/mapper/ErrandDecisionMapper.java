package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import se.sundsvall.supportmanagement.api.model.errand.Decision;
import se.sundsvall.supportmanagement.api.model.errand.DecisionTerm;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionTermEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachments;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toUniqueKeyList;

public final class ErrandDecisionMapper {

	/** Keys regardless of case, and keys that differ only in case in their natural order. */
	private static final Comparator<Parameter> KEY_ORDER = comparing(Parameter::getKey, CASE_INSENSITIVE_ORDER).thenComparing(Parameter::getKey);

	private ErrandDecisionMapper() {}

	/**
	 * Maps a decision to a new entity of the errand. The investigation the decision rests on is passed in already
	 * resolved, and must have been fetched through the errand.
	 */
	public static DecisionEntity toDecisionEntity(final Decision decision, final ErrandEntity errandEntity, final InvestigationEntity investigationEntity, final String namespace,
		final String municipalityId) {
		final var entity = DecisionEntity.create()
			.withErrandEntity(errandEntity)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withType(decision.getType())
			.withStatus(ofNullable(decision.getStatus()).map(ItemStatus::valueOf).orElse(null))
			.withTitle(decision.getTitle())
			.withDescription(decision.getDescription())
			.withDueAt(decision.getDueAt())
			.withCompletedAt(decision.getCompletedAt())
			.withOutcome(decision.getOutcome())
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
		return entity.withParameters(toDecisionParameterEntities(decision.getParameters(), entity));
	}

	/**
	 * Applies the fields the decision carries to the entity. Sent in parameters replace the stored ones, and leave them
	 * untouched when they come out the same.
	 */
	public static DecisionEntity updateDecisionEntity(final DecisionEntity entity, final Decision decision) {
		ofNullable(decision.getType()).ifPresent(entity::setType);
		ofNullable(decision.getStatus()).map(ItemStatus::valueOf).ifPresent(entity::setStatus);
		ofNullable(decision.getTitle()).ifPresent(entity::setTitle);
		ofNullable(decision.getDescription()).ifPresent(entity::setDescription);
		ofNullable(decision.getDueAt()).ifPresent(entity::setDueAt);
		ofNullable(decision.getCompletedAt()).ifPresent(entity::setCompletedAt);
		ofNullable(decision.getOutcome()).ifPresent(entity::setOutcome);
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
		ofNullable(decision.getParameters()).ifPresent(parameters -> replaceParameters(entity, parameters));
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
				.withOutcome(e.getOutcome())
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
				.withAttachments(toErrandAttachments(e.getAttachments()))
				.withParameters(toDecisionParameters(e.getParameters()))
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

	/**
	 * Maps parameters to entities of the decision, one per key with the values of every parameter sent for it, in the
	 * order of the keys. Keys are trimmed before they are compared, and the display name and group are those of the first
	 * parameter sent for a key.
	 */
	public static List<DecisionParameterEntity> toDecisionParameterEntities(final List<Parameter> parameters, final DecisionEntity decisionEntity) {
		final var trimmed = ofNullable(parameters).orElse(emptyList()).stream()
			.map(parameter -> Parameter.create()
				.withKey(parameter.getKey().trim())
				.withDisplayName(parameter.getDisplayName())
				.withGroup(parameter.getGroup())
				.withValues(parameter.getValues()))
			.toList();

		return new ArrayList<>(toUniqueKeyList(trimmed).stream()
			.sorted(KEY_ORDER)
			.map(parameter -> DecisionParameterEntity.create()
				.withDecisionEntity(decisionEntity)
				.withKey(parameter.getKey())
				.withDisplayName(parameter.getDisplayName())
				.withParameterGroup(parameter.getGroup())
				.withValues(parameter.getValues()))
			.toList());
	}

	public static Parameter toDecisionParameter(final DecisionParameterEntity entity) {
		return ofNullable(entity)
			.map(e -> Parameter.create()
				.withKey(e.getKey())
				.withDisplayName(e.getDisplayName())
				.withGroup(e.getParameterGroup())
				.withValues(e.getValues()))
			.orElse(null);
	}

	/**
	 * Maps the parameters of a decision in the order of their keys, the same order whatever order the database reads them
	 * in.
	 */
	public static List<Parameter> toDecisionParameters(final List<DecisionParameterEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandDecisionMapper::toDecisionParameter)
			.sorted(KEY_ORDER)
			.toList();
	}

	/**
	 * Replaces the parameters of the decision in place, as the collection is held by JPA, and marks the decision modified
	 * so that its version moves. Parameters that come out the same as the stored ones, in whatever order they are sent,
	 * leave the decision untouched.
	 */
	private static void replaceParameters(final DecisionEntity entity, final List<Parameter> parameters) {
		final var replacements = toDecisionParameterEntities(parameters, entity);
		if (toDecisionParameters(replacements).equals(toDecisionParameters(entity.getParameters()))) {
			return;
		}
		if (entity.getParameters() == null) {
			entity.setParameters(new ArrayList<>());
		}
		entity.getParameters().clear();
		entity.getParameters().addAll(replacements);
		entity.markModified();
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
