package se.sundsvall.supportmanagement.service.mapper;

import static java.time.OffsetDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Labels;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.LabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;

public class MetadataMapper {

	private static final Gson GSON = new Gson();
	private static final java.lang.reflect.Type LABEL_LIST_TYPE = new TypeToken<List<Label>>() {
	}.getType();

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
				.withName(e.getName())
				.withDisplayName(e.getDisplayName()))
			.orElse(null);
	}

	public static RoleEntity toRoleEntity(final String namespace, final String municipalityId, final Role role) {
		if (anyNull(namespace, municipalityId, role)) {
			return null;
		}

		return RoleEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(role.getName())
			.withDisplayName(role.getDisplayName())
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
		final var existingTypes = Stream.ofNullable(entity.getTypes())
			.flatMap(Collection::stream)
			.collect(Collectors.toMap(TypeEntity::getName, Function.identity()));

		final var updatedTypes = toTypeEntities(types);
		updatedTypes.stream()
			.filter(type -> existingTypes.containsKey(type.getName()))
			.forEach(type -> {
				type.setId(existingTypes.get(type.getName()).getId());
				type.setCreated(existingTypes.get(type.getName()).getCreated());
			});

		entity.setTypes(updatedTypes);
	}

	// =================================================================
	// Label operations
	// =================================================================

	public static LabelEntity toLabelEntity(String namespace, String municipalityId, final List<Label> labels) {
		if (anyNull(namespace, municipalityId, labels)) {
			return null;
		}

		return LabelEntity.create()
			.withJsonStructure(GSON.toJson(labels, LABEL_LIST_TYPE))
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace);
	}

	public static LabelEntity updateLabelEntity(LabelEntity entity, final List<Label> labels) {
		return entity.withJsonStructure(GSON.toJson(labels, LABEL_LIST_TYPE));
	}

	public static Labels toLabels(LabelEntity entity) {
		if (isNull(entity)) {
			return null;
		}

		return Labels.create()
			.withCreated(entity.getCreated())
			.withModified(entity.getModified())
			.withLabelStructure(GSON.fromJson(entity.getJsonStructure(), LABEL_LIST_TYPE));
	}

	// =================================================================
	// Contact Reason operations
	// =================================================================

	public static ContactReason toContactReason(final ContactReasonEntity contactReasonEntity) {
		return ofNullable(contactReasonEntity)
			.map(entity -> ContactReason.create()
				.withId(contactReasonEntity.getId())
				.withReason(entity.getReason())
				.withModified(entity.getModified())
				.withCreated(entity.getCreated()))
			.orElse(null);
	}

	public static ContactReasonEntity toContactReasonEntity(final String namespace, final String municipalityId, final ContactReason contactReason) {
		return Optional.ofNullable(contactReason)
			.map(request -> ContactReasonEntity.create()
				.withReason(request.getReason())
				.withNamespace(namespace)
				.withMunicipalityId(municipalityId)
				.withCreated(now())
				.withModified(now()))
			.orElse(null);
	}

	public static ContactReasonEntity updateContactReason(final ContactReasonEntity entity, final ContactReason contactReason) {
		if (isNull(contactReason)) {
			return entity;
		}

		Optional.ofNullable(contactReason.getReason()).ifPresent(value -> entity.setReason(contactReason.getReason()));

		return entity;
	}
}
