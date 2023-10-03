package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

public class AttachmentExclusionStrategy implements ExclusionStrategy {

	public static ExclusionStrategy create() {
		return new AttachmentExclusionStrategy();
	}

	@Override
	public boolean shouldSkipField(FieldAttributes f) {
		return f.getDeclaringClass() == AttachmentEntity.class && f.getName().equals("attachmentData");
	}

	@Override
	public boolean shouldSkipClass(Class<?> clazz) {
		return clazz == AttachmentDataEntity.class;
	}
}
