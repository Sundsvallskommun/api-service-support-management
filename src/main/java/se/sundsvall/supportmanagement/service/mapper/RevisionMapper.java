package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;

public class RevisionMapper {

	private static final Gson GSON = new GsonBuilder()
		.registerTypeAdapter(OffsetDateTime.class, OffsetDateTimeSerializer.create())
		.addSerializationExclusionStrategy(CircularReferenceExclusionStrategy.create())
		.addSerializationExclusionStrategy(AttachmentExclusionStrategy.create())
		.create();

	private RevisionMapper() {}

	public static RevisionEntity toRevisionEntity(final ErrandEntity entity, final int version) {
		return RevisionEntity.create()
			.withNamespace(entity.getNamespace())
			.withMunicipalityId(entity.getMunicipalityId())
			.withEntityId(entity.getId())
			.withEntityType(entity.getClass().getSimpleName())
			.withVersion(version)
			.withSerializedSnapshot(toSerializedSnapshot(entity));
	}

	public static String toSerializedSnapshot(final ErrandEntity entity) {

		return Optional.ofNullable(entity)
			.map(GSON::toJson)
			.orElse(null);
	}

	public static List<Revision> toRevisions(final List<RevisionEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(RevisionMapper::toRevision)
			.filter(Objects::nonNull)
			.toList();
	}

	public static Revision toRevision(final RevisionEntity entity) {
		return ofNullable(entity)
			.map(e -> Revision.create()
				.withCreated(e.getCreated())
				.withEntityId(e.getEntityId())
				.withEntityType(e.getEntityType())
				.withId(e.getId())
				.withVersion(e.getVersion()))
			.orElse(null);
	}

}
