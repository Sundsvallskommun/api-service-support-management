package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import java.util.Map;
import java.util.Set;
import se.sundsvall.supportmanagement.integration.db.model.AbstractErrandItemEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandPhaseEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.PhaseTransitionEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.TimeMeasurementEntity;

public class CircularReferenceExclusionStrategy implements ExclusionStrategy {

	private static final String ERRAND_ENTITY = "errandEntity";
	private static final String ACTION_CONFIG_ENTITY = "actionConfigEntity";

	private static final String PHASE_ENTITY = "phaseEntity";

	private static final Map<Class<?>, Set<String>> EXCLUDED_FIELDS = Map.ofEntries(
		// The links to the handling artefacts, here and on the JSON parameter below, are left out rather than followed. Each
		// points at an artefact that points back at the errand, and a revision is a copy of the errand, which the artefacts
		// are deliberately not part of.
		Map.entry(AttachmentEntity.class, Set.of(ERRAND_ENTITY, "statementLinks", "investigationLinks", "decisionLinks", "measureLinks")),
		Map.entry(ErrandActionEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(ActionConfigConditionEntity.class, Set.of(ACTION_CONFIG_ENTITY)),
		Map.entry(ActionConfigParameterEntity.class, Set.of(ACTION_CONFIG_ENTITY)),
		Map.entry(JsonParameterEntity.class, Set.of(ERRAND_ENTITY, "statementLinks", "investigationLinks", "investigationSectionLinks", "decisionLinks", "measureLinks")),
		Map.entry(StakeholderEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(StakeholderParameterEntity.class, Set.of("stakeholderEntity")),
		Map.entry(ParameterEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(MetadataLabelEntity.class, Set.of("parent")),
		Map.entry(NotificationEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(ErrandPhaseEntity.class, Set.of(ERRAND_ENTITY)),
		Map.entry(PhaseTransitionEntity.class, Set.of(PHASE_ENTITY)),
		// The reference back to the errand is declared once, in the base class of the handling artefacts, and Gson asks
		// about a field under the class that DECLARES it rather than the one that inherits it. Naming MeasureEntity here
		// would therefore exclude nothing, and the snapshot would walk errand to measure to errand until the stack ran
		// out. Anything else moved up into the base class has to be named here for the same reason.
		Map.entry(AbstractErrandItemEntity.class, Set.of(ERRAND_ENTITY)),

		// What the measure itself declares: the links it holds, and the artefact it follows from, which points back at
		// the errand.
		Map.entry(MeasureEntity.class, Set.of("attachments", "jsonParameterLinks", "decisionEntity", "statementEntity")),
		Map.entry(TimeMeasurementEntity.class, Set.of(ERRAND_ENTITY)));

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
