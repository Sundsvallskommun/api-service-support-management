package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

import se.sundsvall.supportmanagement.api.model.errand.Customer;
import se.sundsvall.supportmanagement.api.model.errand.CustomerType;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.EmbeddableCustomer;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
public class ErrandMapper {

	private ErrandMapper() {}

	public static ErrandEntity toErrandEntity(Errand errand) {
		if (isNull(errand)) {
			return null;
		}

		return ErrandEntity.create()
			.withAssignedGroupId(errand.getAssignedGroupId())
			.withAssignedUserId(errand.getAssignedUserId())
			.withCategoryTag(errand.getCategoryTag())
			.withClientIdTag(errand.getClientIdTag())
			.withCustomer(toCustomer(errand.getCustomer()))
			.withExternalTags(toExternalTag(errand.getExternalTags()))
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
		ofNullable(errand.getCustomer()).ifPresent(value -> entity.setCustomer(toCustomer(value)));
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
		if (isNull(entities)) {
			return emptyList();
		}

		return ofNullable(entities).orElse(emptyList())
			.stream()
			.map(ErrandMapper::toErrand)
			.toList();
	}

	public static Errand toErrand(ErrandEntity entity) {
		if (isNull(entity)) {
			return null;
		}

		return Errand.create()
			.withAssignedGroupId(entity.getAssignedGroupId())
			.withAssignedUserId(entity.getAssignedUserId())
			.withCategoryTag(entity.getCategoryTag())
			.withClientIdTag(entity.getClientIdTag())
			.withCreated(entity.getCreated())
			.withCustomer(toCustomer(entity.getCustomer()))
			.withExternalTags(toExternalTags(entity.getExternalTags()))
			.withId(entity.getId())
			.withModified(entity.getModified())
			.withPriority(Priority.valueOf(entity.getPriority()))
			.withReporterUserId(entity.getReporterUserId())
			.withStatusTag(entity.getStatusTag())
			.withTitle(entity.getTitle())
			.withTouched(entity.getTouched())
			.withTypeTag(entity.getTypeTag())
			.withResolution(entity.getResolution())
			.withDescription(entity.getDescription());
	}

	private static Customer toCustomer(EmbeddableCustomer customer) {
		if (isNull(customer)) {
			return null;
		}

		return Customer.create()
			.withId(customer.getId())
			.withType(CustomerType.valueOf(customer.getType()));
	}

	private static EmbeddableCustomer toCustomer(Customer customer) {
		if (isNull(customer)) {
			return null;
		}

		return EmbeddableCustomer.create()
			.withId(customer.getId())
			.withType(customer.getType().toString());
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
