package se.sundsvall.supportmanagement.service.mapper;

import org.apache.commons.lang3.ObjectUtils;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class MetadataMapper {

	private MetadataMapper() {}

	public static Category toCategory(CategoryEntity entity) {
		return ofNullable(entity)
			.map(e -> Category.create()
				.withCreated(e.getCreated())
				.withDisplayName(e.getDisplayName())
				.withModified(e.getModified())
				.withName(e.getName())
				.withTypes(toTypes(e.getTypes()))
			).orElse(null);

	}

	public static CategoryEntity toCategoryEntity(String namespace, String municipalityId, Category category) {
		if (anyNull(namespace, municipalityId, category)) {
			return null;
		}
		return CategoryEntity.create()
				.withNamespace(namespace)
				.withMunicipalityId(municipalityId)
				.withDisplayName(category.getDisplayName())
				.withName(category.getName())
				.withTypes(toTypeEntities(category.getTypes()));
	}

	private static List<Type> toTypes(List<TypeEntity> typeEntities) {
		return Optional.ofNullable(typeEntities).orElse(emptyList()).stream()
			.map(MetadataMapper::toType)
			.filter(Objects::nonNull)
			.sorted((o1, o2) -> ObjectUtils.compare(
				o1.getDisplayName(),
				o2.getDisplayName()))
			.toList();
	}

	public static Type toType(TypeEntity entity) {
		return ofNullable(entity)
			.map(e -> Type.create()
				.withCreated(e.getCreated())
				.withDisplayName(e.getDisplayName())
				.withEscalationEmail(e.getEscalationEmail())
				.withModified(e.getModified())
				.withName(e.getName()))
			.orElse(null);
	}

	public static List<TypeEntity> toTypeEntities(List<Type> types) {
		return Optional.ofNullable(types).orElse(emptyList()).stream()
			.map(MetadataMapper::toTypeEntity)
			.filter(Objects::nonNull)
			.toList();
	}

	private static TypeEntity toTypeEntity(Type type) {
		return ofNullable(type)
			.map(e -> TypeEntity.create()
				.withDisplayName(e.getDisplayName())
				.withEscalationEmail(e.getEscalationEmail())
				.withName(e.getName()))
			.orElse(null);
	}

	public static Status toStatus(StatusEntity entity) {
		return ofNullable(entity)
			.map(e -> Status.create()
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withName(e.getName()))
			.orElse(null);
	}

	public static StatusEntity toStatusEntity(String namespace, String municipalityId, Status status) {
		if (anyNull(namespace, municipalityId, status)) {
			return null;
		}

		return StatusEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(status.getName())
			.withNamespace(namespace);
	}

	public static ExternalIdType toExternalIdType(ExternalIdTypeEntity entity) {
		return ofNullable(entity)
			.map(e -> ExternalIdType.create()
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withName(e.getName()))
			.orElse(null);
	}

	public static ExternalIdTypeEntity toExternalIdTypeEntity(String namespace, String municipalityId, ExternalIdType externalIdType) {
		if (anyNull(namespace, municipalityId, externalIdType)) {
			return null;
		}

		return ExternalIdTypeEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(externalIdType.getName())
			.withNamespace(namespace);
	}

	public static CategoryEntity updateEntity(CategoryEntity entity, Category category) {
		if (isNull(category)) {
			return entity;
		}

		ofNullable(category.getName()).ifPresent(value -> entity.setName(isEmpty(value) ? null : value));
		ofNullable(category.getDisplayName()).ifPresent(value -> entity.setDisplayName(isEmpty(value) ? null : value));
		ofNullable(category.getTypes()).ifPresent(value -> updateTypes(entity, value));

		return entity;
	}

	private static void updateTypes(CategoryEntity entity, List<Type> types) {
		ofNullable(entity.getTypes()).ifPresentOrElse(List::clear, () -> entity.setTypes(new ArrayList<>()));
		entity.getTypes().addAll(toTypeEntities(types));
	}
}
