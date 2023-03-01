package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import se.sundsvall.supportmanagement.api.model.errand.*;
import se.sundsvall.supportmanagement.integration.db.model.ContactChannelEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

public class ErrandMapper {

	private ErrandMapper() {}

	public static ErrandEntity toErrandEntity(String namespace, String municipalityId, Errand errand) {
		if (anyNull(namespace, municipalityId, errand)) {
			return null;
		}

		return ErrandEntity.create()
			.withAssignedGroupId(errand.getAssignedGroupId())
			.withAssignedUserId(errand.getAssignedUserId())
			.withCategoryTag(errand.getCategoryTag())
			.withStakeholders(toStakeholderEntities(errand.getStakeholders()))
			.withExternalTags(toExternalTag(errand.getExternalTags()))
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withPriority(errand.getPriority().name())
			.withReporterUserId(errand.getReporterUserId())
			.withStatusTag(errand.getStatusTag())
			.withTitle(errand.getTitle())
			.withTypeTag(errand.getTypeTag())
			.withResolution(errand.getResolution())
			.withDescription(errand.getDescription());
	}

	public static ErrandEntity updateEntity(ErrandEntity entity, Errand errand) {
		if (isNull(errand)) {
			return entity;
		}

		ofNullable(errand.getAssignedGroupId()).ifPresent(value -> entity.setAssignedGroupId(isEmpty(value) ? null : value));
		ofNullable(errand.getAssignedUserId()).ifPresent(value -> entity.setAssignedUserId(isEmpty(value) ? null : value));
		ofNullable(errand.getCategoryTag()).ifPresent(entity::setCategoryTag);
		ofNullable(errand.getStakeholders()).ifPresent(value -> entity.setStakeholders(toStakeholderEntities(value)));
		ofNullable(errand.getExternalTags()).ifPresent(value -> entity.setExternalTags(toExternalTag(value)));
		ofNullable(errand.getPriority()).ifPresent(value -> entity.setPriority(value.name()));
		ofNullable(errand.getStatusTag()).ifPresent(entity::setStatusTag);
		ofNullable(errand.getTitle()).ifPresent(entity::setTitle);
		ofNullable(errand.getTypeTag()).ifPresent(entity::setTypeTag);
		ofNullable(errand.getResolution()).ifPresent(value -> entity.setResolution(isEmpty(value) ? null : value));
		ofNullable(errand.getDescription()).ifPresent(value -> entity.setDescription(isEmpty(value) ? null : value));

		return entity;
	}
	
	private static List<DbExternalTag> toExternalTag(List<ExternalTag> tags) {
		return ofNullable(tags).orElse(emptyList()).stream()
			.map(ErrandMapper::toExternalTagEntity)
			.collect(toCollection(ArrayList::new));
	}

	private static DbExternalTag toExternalTagEntity(ExternalTag tag) {
		return DbExternalTag.create()
			.withKey(tag.getKey())
			.withValue(tag.getValue());
	}

	public static List<Errand> toErrands(List<ErrandEntity> entities) {
		return ofNullable(entities).orElse(emptyList())
			.stream()
			.map(ErrandMapper::toErrand)
			.toList();
	}

	public static Errand toErrand(ErrandEntity entity) {
		return Optional.ofNullable(entity)
			.map(e -> Errand.create()
				.withAssignedGroupId(e.getAssignedGroupId())
				.withAssignedUserId(e.getAssignedUserId())
				.withCategoryTag(e.getCategoryTag())
				.withCreated(e.getCreated())
				.withStakeholders(toStakeholders(e.getStakeholders()))
				.withExternalTags(toExternalTags(e.getExternalTags()))
				.withId(e.getId())
				.withModified(e.getModified())
				.withPriority(Priority.valueOf(e.getPriority()))
				.withReporterUserId(e.getReporterUserId())
				.withStatusTag(e.getStatusTag())
				.withTitle(e.getTitle())
				.withTouched(e.getTouched())
				.withTypeTag(e.getTypeTag())
				.withResolution(e.getResolution())
				.withDescription(e.getDescription()))
			.orElse(null);
	}

	private static List<Stakeholder> toStakeholders(List<StakeholderEntity> stakeholderEntities) {
		return Optional.ofNullable(stakeholderEntities)
			.map(s -> s.stream()
					.map(stakeholderEntity -> Stakeholder.create()
							.withStakeholderId(stakeholderEntity.getStakeholderId())
							.withType(StakeholderType.valueOf(stakeholderEntity.getType()))
							.withFirstName(stakeholderEntity.getFirstName())
							.withLastName(stakeholderEntity.getLastName())
							.withAddress(stakeholderEntity.getAddress())
							.withCareOf(stakeholderEntity.getCareOf())
							.withZipCode(stakeholderEntity.getZipCode())
							.withCountry(stakeholderEntity.getCountry())
							.withContactChannels(toContactChannels(stakeholderEntity.getContactChannels())))
					.toList())
			.orElse(null);
	}

	private static List<StakeholderEntity> toStakeholderEntities(List<Stakeholder> stakeholders) {
		return Optional.ofNullable(stakeholders)
			.map(s -> s.stream()
					.map(stakeholder -> StakeholderEntity.create()
							.withStakeholderId(stakeholder.getStakeholderId())
							.withType(stakeholder.getType().toString())
							.withFirstName(stakeholder.getFirstName())
							.withLastName(stakeholder.getLastName())
							.withAddress(stakeholder.getAddress())
							.withCareOf(stakeholder.getCareOf())
							.withZipCode(stakeholder.getZipCode())
							.withCountry(stakeholder.getCountry())
							.withContactChannels(toContactChannelEntities(stakeholder.getContactChannels())))
					.toList())
				.orElse(null);
	}

	private static List<ContactChannelEntity> toContactChannelEntities(List<ContactChannel> contactChannels) {
		return Optional.ofNullable(contactChannels)
				.map(ch -> ch.stream()
						.map(contactChannel -> ContactChannelEntity.create()
								.withType(contactChannel.getType())
								.withValue(contactChannel.getValue()))
						.toList())
				.orElse(null);
	}

	private static List<ContactChannel> toContactChannels(List<ContactChannelEntity> contactChannelEntities) {
		return Optional.ofNullable(contactChannelEntities)
				.map(ch -> ch.stream()
						.map(contactChannelEntity -> ContactChannel.create()
								.withType(contactChannelEntity.getType())
								.withValue(contactChannelEntity.getValue()))
						.toList())
				.orElse(null);
	}

	private static List<ExternalTag> toExternalTags(List<DbExternalTag> entities) {
		return ofNullable(entities).orElse(emptyList())
			.stream()
			.map(ErrandMapper::toExternalTag)
			.toList();
	}

	private static ExternalTag toExternalTag(DbExternalTag entity) {
		return ExternalTag.create()
			.withKey(entity.getKey())
			.withValue(entity.getValue());
	}
}
