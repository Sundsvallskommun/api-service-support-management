package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

public class CircularReferenceExclusionStrategy implements ExclusionStrategy {

	private static final String ERRAND_ENTITY = "errandEntity";

	public static CircularReferenceExclusionStrategy create() {
		return new CircularReferenceExclusionStrategy();
	}

	@Override
	public boolean shouldSkipField(final FieldAttributes f) {
		return (f.getDeclaringClass() == AttachmentEntity.class && f.getName().equals(ERRAND_ENTITY)) ||
			(f.getDeclaringClass() == StakeholderEntity.class && f.getName().equals(ERRAND_ENTITY)) ||
			(f.getDeclaringClass() == ParameterEntity.class && f.getName().equals(ERRAND_ENTITY));
	}

	@Override
	public boolean shouldSkipClass(final Class<?> clazz) {
		return false;
	}

}
