package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;

public class MetadataMapper {

	private MetadataMapper() {}

	// =================================================================
	// Category and Type operations
	// =================================================================

	public static Category toCategory(final CategoryEntity entity) {
		return ofNullable(entity)
			.map(e -> Category.create()
				.withCreated(e.getCreated())
				.withDisplayName(e.getDisplayName())
				.withModified(e.getModified())
				.withName(e.getName())
				.withTypes(toTypes(e.getTypes()))).orElse(null);
	}

	public static CategoryEntity toCategoryEntity(final String namespace, final String municipalityId, final Category category) {
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

	private static List<Type> toTypes(final List<TypeEntity> typeEntities) {
		return Optional.ofNullable(typeEntities).orElse(emptyList()).stream()
			.map(MetadataMapper::toType)
			.filter(Objects::nonNull)
			.sorted(comparing(Type::getDisplayName, nullsFirst(naturalOrder())))
			.toList();
	}

	public static Type toType(final TypeEntity entity) {
		return ofNullable(entity)
			.map(e -> Type.create()
				.withCreated(e.getCreated())
				.withDisplayName(e.getDisplayName())
				.withEscalationEmail(e.getEscalationEmail())
				.withModified(e.getModified())
				.withName(e.getName()))
			.orElse(null);
	}

	public static List<TypeEntity> toTypeEntities(final List<Type> types) {
		return Optional.ofNullable(types).orElse(emptyList()).stream()
			.map(MetadataMapper::toTypeEntity)
			.filter(Objects::nonNull)
			.toList();
	}

	private static TypeEntity toTypeEntity(final Type type) {
		return ofNullable(type)
			.map(e -> TypeEntity.create()
				.withDisplayName(e.getDisplayName())
				.withEscalationEmail(e.getEscalationEmail())
				.withName(e.getName()))
			.orElse(null);
	}

	// =================================================================
	// Status operations
	// =================================================================

	public static Status toStatus(final StatusEntity entity) {
		return ofNullable(entity)
			.map(e -> Status.create()
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withName(e.getName()))
			.orElse(null);
	}

	public static StatusEntity toStatusEntity(final String namespace, final String municipalityId, final Status status) {
		if (anyNull(namespace, municipalityId, status)) {
			return null;
		}

		return StatusEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(status.getName())
			.withNamespace(namespace);
	}

	// =================================================================
	// Role operations
	// =================================================================

	public static Role toRole(final RoleEntity entity) {
		return ofNullable(entity)
			.map(e -> Role.create()
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withName(e.getName()))
			.orElse(null);
	}

	public static RoleEntity toRoleEntity(final String namespace, final String municipalityId, final Role role) {
		if (anyNull(namespace, municipalityId, role)) {
			return null;
		}

		return RoleEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(role.getName())
			.withNamespace(namespace);
	}

	// =================================================================
	// ExternalIdType operations
	// =================================================================

	public static ExternalIdType toExternalIdType(final ExternalIdTypeEntity entity) {
		return ofNullable(entity)
			.map(e -> ExternalIdType.create()
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withName(e.getName()))
			.orElse(null);
	}

	public static ExternalIdTypeEntity toExternalIdTypeEntity(final String namespace, final String municipalityId, final ExternalIdType externalIdType) {
		if (anyNull(namespace, municipalityId, externalIdType)) {
			return null;
		}

		return ExternalIdTypeEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(externalIdType.getName())
			.withNamespace(namespace);
	}

	public static CategoryEntity updateEntity(final CategoryEntity entity, final Category category) {
		if (isNull(category)) {
			return entity;
		}

		ofNullable(category.getName()).ifPresent(value -> entity.setName(isEmpty(value) ? null : value));
		ofNullable(category.getDisplayName()).ifPresent(value -> entity.setDisplayName(isEmpty(value) ? null : value));
		ofNullable(category.getTypes()).ifPresent(value -> updateTypes(entity, value));

		return entity;
	}

	private static void updateTypes(final CategoryEntity entity, final List<Type> types) {
		ofNullable(entity.getTypes()).ifPresentOrElse(List::clear, () -> entity.setTypes(new ArrayList<>()));
		entity.getTypes().addAll(toTypeEntities(types));
	}
}
