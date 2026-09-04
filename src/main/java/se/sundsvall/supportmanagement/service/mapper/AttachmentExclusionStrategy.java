package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import java.util.Set;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

public class AttachmentExclusionStrategy implements ExclusionStrategy {

	// The data an attachment holds, and the id of the row holding it. Both name the same thing, and a snapshot has no
	// use for either: what a revision keeps is the errand as it was, not the file somebody attached to it or where in
	// the database that file happens to sit.
	private static final Set<String> EXCLUDED_FIELDS = Set.of("attachmentData", "attachmentDataId");

	public static ExclusionStrategy create() {
		return new AttachmentExclusionStrategy();
	}

	@Override
	public boolean shouldSkipField(FieldAttributes f) {
		return f.getDeclaringClass() == AttachmentEntity.class && EXCLUDED_FIELDS.contains(f.getName());
	}

	@Override
	public boolean shouldSkipClass(Class<?> clazz) {
		return clazz == AttachmentDataEntity.class;
	}
}
