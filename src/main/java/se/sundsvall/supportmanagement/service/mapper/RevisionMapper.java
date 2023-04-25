package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;

public class RevisionMapper {
	private RevisionMapper() {}

	private static final Gson GSON = new GsonBuilder()
		.registerTypeAdapter(OffsetDateTime.class, OffsetDateTimeSerializer.create())
		.addSerializationExclusionStrategy(CircularReferenceExclusionStrategy.create())
		.create();

	public static RevisionEntity toRevisionEntity(ErrandEntity entity, int version) {
		return RevisionEntity.create()
			.withEntityId(entity.getId())
			.withEntityType(entity.getClass().getSimpleName())
			.withVersion(version)
			.withSerializedSnapshot(toSerializedSnapshot(entity));
	}

	public static String toSerializedSnapshot(ErrandEntity entity) {
		return Optional.ofNullable(entity)
			.map(GSON::toJson)
			.orElse(null);
	}

	public static List<Revision> toRevisions(List<RevisionEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(RevisionMapper::toRevision)
			.filter(Objects::nonNull)
			.toList();
	}

	private static Revision toRevision(RevisionEntity entity) {
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
