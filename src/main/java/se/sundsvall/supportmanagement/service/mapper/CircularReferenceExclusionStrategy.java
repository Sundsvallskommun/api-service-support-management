package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

public class CircularReferenceExclusionStrategy implements ExclusionStrategy {

	public static CircularReferenceExclusionStrategy create() {
		return new CircularReferenceExclusionStrategy();
	}

	@Override
	public boolean shouldSkipField(FieldAttributes f) {
		return (f.getDeclaringClass() == AttachmentEntity.class && f.getName().equals("errandEntity")) ||
			(f.getDeclaringClass() == StakeholderEntity.class && f.getName().equals("errandEntity"));
	}

	@Override
	public boolean shouldSkipClass(Class<?> clazz) {
		return false;
	}
}
