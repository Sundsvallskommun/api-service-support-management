package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import java.util.Set;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;

public class AttachmentExclusionStrategy implements ExclusionStrategy {

	// The data an attachment holds, and the id of the row holding it. Both name the same thing, and a snapshot has no
	// use for either: what a revision keeps is the errand as it was, not the file somebody attached to it or where in
	// the database that file happens to sit.
	private static final Set<String> EXCLUDED_FIELDS = Set.of("attachmentData", "attachmentDataId");

	// The purpose an attachment was given is kept by what it is. What the metadata says about it besides - its display
	// name, its order, whether it is deprecated - belongs to the metadata, and letting that in would make an edit there
	// show up as a change to every errand using the purpose.
	private static final Set<String> KEPT_PURPOSE_FIELDS = Set.of("id", "name");

	public static ExclusionStrategy create() {
		return new AttachmentExclusionStrategy();
	}

	@Override
	public boolean shouldSkipField(FieldAttributes f) {
		return ((f.getDeclaringClass() == AttachmentEntity.class) && EXCLUDED_FIELDS.contains(f.getName()))
			|| ((f.getDeclaringClass() == AttachmentPurposeEntity.class) && !KEPT_PURPOSE_FIELDS.contains(f.getName()));
	}

	@Override
	public boolean shouldSkipClass(Class<?> clazz) {
		return clazz == AttachmentDataEntity.class;
	}
}
