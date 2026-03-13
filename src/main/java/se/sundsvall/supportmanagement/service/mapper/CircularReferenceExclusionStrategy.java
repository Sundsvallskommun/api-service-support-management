package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import java.util.Map;
import java.util.Set;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderParameterEntity;

public class CircularReferenceExclusionStrategy implements ExclusionStrategy {

	private static final String ERRAND_ENTITY = "errandEntity";
	private static final String ACTION_CONFIG_ENTITY = "actionConfigEntity";

	private static final Map<Class<?>, Set<String>> EXCLUDED_FIELDS = Map.of(
		AttachmentEntity.class, Set.of(ERRAND_ENTITY),
		ErrandActionEntity.class, Set.of(ERRAND_ENTITY),
		ActionConfigConditionEntity.class, Set.of(ACTION_CONFIG_ENTITY),
		ActionConfigParameterEntity.class, Set.of(ACTION_CONFIG_ENTITY),
		JsonParameterEntity.class, Set.of(ERRAND_ENTITY),
		StakeholderEntity.class, Set.of(ERRAND_ENTITY),
		StakeholderParameterEntity.class, Set.of("stakeholderEntity"),
		ParameterEntity.class, Set.of(ERRAND_ENTITY),
		MetadataLabelEntity.class, Set.of("parent"),
		NotificationEntity.class, Set.of(ERRAND_ENTITY));

	public static CircularReferenceExclusionStrategy create() {
		return new CircularReferenceExclusionStrategy();
	}

	@Override
	public boolean shouldSkipField(final FieldAttributes f) {
		return EXCLUDED_FIELDS.getOrDefault(f.getDeclaringClass(), Set.of()).contains(f.getName());
	}

	@Override
	public boolean shouldSkipClass(final Class<?> clazz) {
		return false;
	}
}
