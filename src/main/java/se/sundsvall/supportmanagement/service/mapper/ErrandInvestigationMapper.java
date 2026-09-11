package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.errand.Investigation;
import se.sundsvall.supportmanagement.api.model.errand.InvestigationSection;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionOutcome;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.SectionAssessment;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactAttachmentMapper.toArtefactAttachments;

public final class ErrandInvestigationMapper {

	private ErrandInvestigationMapper() {}

	public static InvestigationEntity toInvestigationEntity(final Investigation investigation, final ErrandEntity errandEntity, final String namespace, final String municipalityId) {
		return InvestigationEntity.create()
			.withErrandEntity(errandEntity)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withType(investigation.getType())
			.withStatus(ofNullable(investigation.getStatus()).map(ItemStatus::valueOf).orElse(null))
			.withTitle(investigation.getTitle())
			.withDescription(investigation.getDescription())
			.withDueAt(investigation.getDueAt())
			.withCompletedAt(investigation.getCompletedAt())
			.withInvestigatorUserId(investigation.getInvestigatorUserId())
			.withStartedAt(investigation.getStartedAt())
			.withSummary(investigation.getSummary())
			.withConclusion(investigation.getConclusion())
			.withRecommendation(ofNullable(investigation.getRecommendation()).map(DecisionOutcome::valueOf).orElse(null))
			.withRecommendationMotivation(investigation.getRecommendationMotivation());
	}

	public static InvestigationEntity updateInvestigationEntity(final InvestigationEntity entity, final Investigation investigation) {
		ofNullable(investigation.getType()).ifPresent(entity::setType);
		ofNullable(investigation.getStatus()).map(ItemStatus::valueOf).ifPresent(entity::setStatus);
		ofNullable(investigation.getTitle()).ifPresent(entity::setTitle);
		ofNullable(investigation.getDescription()).ifPresent(entity::setDescription);
		ofNullable(investigation.getDueAt()).ifPresent(entity::setDueAt);
		ofNullable(investigation.getCompletedAt()).ifPresent(entity::setCompletedAt);
		ofNullable(investigation.getInvestigatorUserId()).ifPresent(entity::setInvestigatorUserId);
		ofNullable(investigation.getStartedAt()).ifPresent(entity::setStartedAt);
		ofNullable(investigation.getSummary()).ifPresent(entity::setSummary);
		ofNullable(investigation.getConclusion()).ifPresent(entity::setConclusion);
		ofNullable(investigation.getRecommendation()).map(DecisionOutcome::valueOf).ifPresent(entity::setRecommendation);
		ofNullable(investigation.getRecommendationMotivation()).ifPresent(entity::setRecommendationMotivation);
		return entity;
	}

	public static Investigation toInvestigation(final InvestigationEntity entity) {
		return ofNullable(entity)
			.map(e -> Investigation.create()
				.withId(e.getId())
				.withType(e.getType())
				.withStatus(ofNullable(e.getStatus()).map(Enum::name).orElse(null))
				.withTitle(e.getTitle())
				.withDescription(e.getDescription())
				.withDueAt(e.getDueAt())
				.withCompletedAt(e.getCompletedAt())
				.withInvestigatorUserId(e.getInvestigatorUserId())
				.withStartedAt(e.getStartedAt())
				.withSummary(e.getSummary())
				.withConclusion(e.getConclusion())
				.withRecommendation(ofNullable(e.getRecommendation()).map(Enum::name).orElse(null))
				.withRecommendationMotivation(e.getRecommendationMotivation())
				.withSections(toInvestigationSections(e.getSections()))
				.withAttachments(toArtefactAttachments(e.getAttachments()))
				.withCreatedBy(e.getCreatedBy())
				.withModifiedBy(e.getModifiedBy())
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withVersion(e.getVersion()))
			.orElse(null);
	}

	public static List<Investigation> toInvestigations(final List<InvestigationEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandInvestigationMapper::toInvestigation)
			.toList();
	}

	public static InvestigationSectionEntity toInvestigationSectionEntity(final InvestigationSection section, final InvestigationEntity investigationEntity) {
		return InvestigationSectionEntity.create()
			.withInvestigationEntity(investigationEntity)
			.withSectionKey(section.getSectionKey())
			.withHeading(section.getHeading())
			.withSortOrder(section.getSortOrder())
			.withAssessment(ofNullable(section.getAssessment()).map(SectionAssessment::valueOf).orElse(null))
			.withText(section.getText())
			.withCompletedBy(section.getCompletedBy())
			.withCompletedAt(section.getCompletedAt());
	}

	public static InvestigationSectionEntity updateInvestigationSectionEntity(final InvestigationSectionEntity entity, final InvestigationSection section) {
		ofNullable(section.getSectionKey()).ifPresent(entity::setSectionKey);
		ofNullable(section.getHeading()).ifPresent(entity::setHeading);
		ofNullable(section.getSortOrder()).ifPresent(entity::setSortOrder);
		ofNullable(section.getAssessment()).map(SectionAssessment::valueOf).ifPresent(entity::setAssessment);
		ofNullable(section.getText()).ifPresent(entity::setText);
		ofNullable(section.getCompletedBy()).ifPresent(entity::setCompletedBy);
		ofNullable(section.getCompletedAt()).ifPresent(entity::setCompletedAt);
		return entity;
	}

	public static InvestigationSection toInvestigationSection(final InvestigationSectionEntity entity) {
		return ofNullable(entity)
			.map(e -> InvestigationSection.create()
				.withId(e.getId())
				.withSectionKey(e.getSectionKey())
				.withHeading(e.getHeading())
				.withSortOrder(e.getSortOrder())
				.withAssessment(ofNullable(e.getAssessment()).map(Enum::name).orElse(null))
				.withText(e.getText())
				.withCompletedBy(e.getCompletedBy())
				.withCompletedAt(e.getCompletedAt()))
			.orElse(null);
	}

	public static List<InvestigationSection> toInvestigationSections(final List<InvestigationSectionEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandInvestigationMapper::toInvestigationSection)
			.toList();
	}
}
