package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.errand.Statement;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.StatementOutcome;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactAttachmentMapper.toArtefactAttachments;

public final class ErrandStatementMapper {

	private ErrandStatementMapper() {}

	public static StatementEntity toStatementEntity(final Statement statement, final ErrandEntity errandEntity, final String namespace, final String municipalityId) {
		return StatementEntity.create()
			.withErrandEntity(errandEntity)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withType(statement.getType())
			.withStatus(ofNullable(statement.getStatus()).map(ItemStatus::valueOf).orElse(null))
			.withTitle(statement.getTitle())
			.withDescription(statement.getDescription())
			.withDueAt(statement.getDueAt())
			.withCompletedAt(statement.getCompletedAt())
			.withCounterpartyName(statement.getCounterpartyName())
			.withCounterpartyExternalId(statement.getCounterpartyExternalId())
			.withCounterpartyExternalIdType(statement.getCounterpartyExternalIdType())
			.withCounterpartyReference(statement.getCounterpartyReference())
			.withQuestion(statement.getQuestion())
			.withSentAt(statement.getSentAt())
			.withRemindedAt(statement.getRemindedAt())
			.withRespondedAt(statement.getRespondedAt())
			.withOutcome(ofNullable(statement.getOutcome()).map(StatementOutcome::valueOf).orElse(null))
			.withResponseText(statement.getResponseText())
			.withCommunicationId(statement.getCommunicationId());
	}

	/**
	 * Applies the fields the patch carries. A null says nothing about the field, which is what makes a patch able to
	 * leave everything it does not mention alone.
	 */
	public static StatementEntity updateStatementEntity(final StatementEntity entity, final Statement statement) {
		ofNullable(statement.getType()).ifPresent(entity::setType);
		ofNullable(statement.getStatus()).map(ItemStatus::valueOf).ifPresent(entity::setStatus);
		ofNullable(statement.getTitle()).ifPresent(entity::setTitle);
		ofNullable(statement.getDescription()).ifPresent(entity::setDescription);
		ofNullable(statement.getDueAt()).ifPresent(entity::setDueAt);
		ofNullable(statement.getCompletedAt()).ifPresent(entity::setCompletedAt);
		ofNullable(statement.getCounterpartyName()).ifPresent(entity::setCounterpartyName);
		ofNullable(statement.getCounterpartyExternalId()).ifPresent(entity::setCounterpartyExternalId);
		ofNullable(statement.getCounterpartyExternalIdType()).ifPresent(entity::setCounterpartyExternalIdType);
		ofNullable(statement.getCounterpartyReference()).ifPresent(entity::setCounterpartyReference);
		ofNullable(statement.getQuestion()).ifPresent(entity::setQuestion);
		ofNullable(statement.getSentAt()).ifPresent(entity::setSentAt);
		ofNullable(statement.getRemindedAt()).ifPresent(entity::setRemindedAt);
		ofNullable(statement.getRespondedAt()).ifPresent(entity::setRespondedAt);
		ofNullable(statement.getOutcome()).map(StatementOutcome::valueOf).ifPresent(entity::setOutcome);
		ofNullable(statement.getResponseText()).ifPresent(entity::setResponseText);
		ofNullable(statement.getCommunicationId()).ifPresent(entity::setCommunicationId);
		return entity;
	}

	public static Statement toStatement(final StatementEntity entity) {
		return ofNullable(entity)
			.map(e -> Statement.create()
				.withId(e.getId())
				.withType(e.getType())
				.withStatus(ofNullable(e.getStatus()).map(Enum::name).orElse(null))
				.withTitle(e.getTitle())
				.withDescription(e.getDescription())
				.withDueAt(e.getDueAt())
				.withCompletedAt(e.getCompletedAt())
				.withCounterpartyName(e.getCounterpartyName())
				.withCounterpartyExternalId(e.getCounterpartyExternalId())
				.withCounterpartyExternalIdType(e.getCounterpartyExternalIdType())
				.withCounterpartyReference(e.getCounterpartyReference())
				.withQuestion(e.getQuestion())
				.withSentAt(e.getSentAt())
				.withRemindedAt(e.getRemindedAt())
				.withRespondedAt(e.getRespondedAt())
				.withOutcome(ofNullable(e.getOutcome()).map(Enum::name).orElse(null))
				.withResponseText(e.getResponseText())
				.withCommunicationId(e.getCommunicationId())
				.withAttachments(toArtefactAttachments(e.getAttachments()))
				.withCreatedBy(e.getCreatedBy())
				.withModifiedBy(e.getModifiedBy())
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withVersion(e.getVersion()))
			.orElse(null);
	}

	public static List<Statement> toStatements(final List<StatementEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandStatementMapper::toStatement)
			.toList();
	}
}
