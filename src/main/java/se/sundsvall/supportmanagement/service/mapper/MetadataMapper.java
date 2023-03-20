package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;

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

	private static List<Type> toTypes(List<TypeEntity> typeEntities) {
		return Optional.ofNullable(typeEntities).orElse(Collections.emptyList()).stream()
			.map(MetadataMapper::toType)
			.filter(Objects::nonNull)
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

	public static Status toStatus(StatusEntity entity) {
		return ofNullable(entity)
			.map(e -> Status.create()
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withName(e.getName()))
			.orElse(null);
	}

	public static ExternalIdType toExternalIdType(ExternalIdTypeEntity entity) {
		return ofNullable(entity)
			.map(e -> ExternalIdType.create()
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withName(e.getName()))
			.orElse(null);
	}
}
