package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import java.util.Map;
import java.util.Set;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandPhaseEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.PhaseTransitionEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderParameterEntity;

public class CircularReferenceExclusionStrategy implements ExclusionStrategy {

	private static final String ERRAND_ENTITY = "errandEntity";
	private static final String ACTION_CONFIG_ENTITY = "actionConfigEntity";

	private static final String PHASE_ENTITY = "phaseEntity";

	private static final Map<Class<?>, Set<String>> EXCLUDED_FIELDS = Map.ofEntries(
		Map.entry(AttachmentEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(ErrandActionEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(ActionConfigConditionEntity.class, Set.of(ACTION_CONFIG_ENTITY)),
		Map.entry(ActionConfigParameterEntity.class, Set.of(ACTION_CONFIG_ENTITY)),
		Map.entry(JsonParameterEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(StakeholderEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(StakeholderParameterEntity.class, Set.of("stakeholderEntity")),
		Map.entry(ParameterEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(MetadataLabelEntity.class, Set.of("parent")),
		Map.entry(NotificationEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(ErrandPhaseEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(PhaseTransitionEntity.class, Set.of(PHASE_ENTITY)));

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
