package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderParameterEntity;

public class CircularReferenceExclusionStrategy implements ExclusionStrategy {

	private static final String ERRAND_ENTITY = "errandEntity";
	private static final String STAKEHOLDER_ENTITY = "stakeholderEntity";

	public static CircularReferenceExclusionStrategy create() {
		return new CircularReferenceExclusionStrategy();
	}

	@Override
	public boolean shouldSkipField(final FieldAttributes f) {
		return ((f.getDeclaringClass() == AttachmentEntity.class) && ERRAND_ENTITY.equals(f.getName())) ||
			((f.getDeclaringClass() == StakeholderEntity.class) && ERRAND_ENTITY.equals(f.getName())) ||
			((f.getDeclaringClass() == StakeholderParameterEntity.class) && STAKEHOLDER_ENTITY.equals(f.getName())) ||
			((f.getDeclaringClass() == ParameterEntity.class) && ERRAND_ENTITY.equals(f.getName())) ||
			((f.getDeclaringClass() == MetadataLabelEntity.class) && "parent".equals(f.getName())) ||
			((f.getDeclaringClass() == NotificationEntity.class) && ERRAND_ENTITY.equals(f.getName()));
	}

	@Override
	public boolean shouldSkipClass(final Class<?> clazz) {
		return false;
	}
}
