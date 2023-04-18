package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
		return GSON.toJson(entity);
	}
}
