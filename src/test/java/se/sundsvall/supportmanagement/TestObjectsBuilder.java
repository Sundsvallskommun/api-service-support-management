package se.sundsvall.supportmanagement;

import static java.time.OffsetDateTime.now;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static se.sundsvall.supportmanagement.api.model.errand.StakeholderType.PRIVATE;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.HIGH;

import java.time.OffsetDateTime;
import java.util.List;

import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.api.model.errand.StakeholderType;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.integration.db.model.*;

public class TestObjectsBuilder {
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String ASSIGNED_GROUP_ID = "assignedGroupId";
	private static final String ASSIGNED_USER_ID = "assignedUserId";
	private static final String CATEGORY_TAG = "categoryTag";
	private static final String NAMESPACE = "namespace";
	private static final OffsetDateTime CREATED = now().minusWeeks(1);
	private static final String EXTERNAL_ID = "externalId";
	private static final String STAKEHOLDER_TYPE_STRING = PRIVATE.toString();
	private static final String TAG_KEY = "tagKey";
	private static final String TAG_VALUE = "tagValue";
	private static final OffsetDateTime MODIFIED = now();
	private static final String PRIORITY = HIGH.name();
	private static final String REPORTER_USER_ID = "reporterUserId";
	private static final String STATUS_TAG = "statusTag";
	private static final String TITLE = "title";
	private static final String TYPE_TAG = "typeTag";
	private static final String ATTACHMENT_ID = "attachmentId";
	private static final String FILE = "file";
	private static final String FILE_NAME = "fileName";
	private static final String MIME_TYPE = "mimeType";
	private static final StakeholderType STAKEHOLDER_TYPE = PRIVATE;
	private static final String ID = "id";

	public static ErrandEntity buildErrandEntity() {
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withAssignedGroupId(ASSIGNED_GROUP_ID)
			.withAssignedUserId(ASSIGNED_USER_ID)
			.withAttachments(List.of(AttachmentEntity.create().withId(ATTACHMENT_ID).withFileName(FILE_NAME).withFile(FILE.getBytes()).withMimeType(MIME_TYPE)))
			.withCategoryTag(CATEGORY_TAG)
			.withCreated(CREATED)
			.withNamespace(NAMESPACE)
			.withStakeholders(List.of(StakeholderEntity.create().withExternalId(EXTERNAL_ID).withType(STAKEHOLDER_TYPE_STRING)))
			.withExternalTags(List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)))
			.withModified(MODIFIED)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withPriority(PRIORITY)
			.withReporterUserId(REPORTER_USER_ID)
			.withStatusTag(STATUS_TAG)
			.withTitle(TITLE)
			.withTypeTag(TYPE_TAG);
	}

	public static AttachmentEntity buildAttachmentEntity(ErrandEntity errandEntity) {
		return AttachmentEntity.create()
			.withId(ATTACHMENT_ID)
			.withFileName(FILE_NAME)
			.withFile(FILE.getBytes())
			.withMimeType(MIME_TYPE)
			.withErrandEntity(errandEntity);
	}

	public static ErrandAttachment buildErrandAttachment() {
		return ErrandAttachment.create()
			.withErrandAttachmentHeader(ErrandAttachmentHeader.create()
				.withId(ATTACHMENT_ID)
				.withFileName(FILE_NAME)
				.withMimeType(MIME_TYPE))
			.withBase64EncodedString(encodeBase64String(FILE.getBytes()));
	}

	public static Errand buildErrand() {
		return Errand.create()
			.withAssignedGroupId(ASSIGNED_GROUP_ID)
			.withAssignedUserId(ASSIGNED_USER_ID)
			.withCategoryTag(CATEGORY_TAG)
			.withCreated(CREATED)
			.withStakeholders(List.of(Stakeholder.create().withExternalId(EXTERNAL_ID).withType(STAKEHOLDER_TYPE)))
			.withExternalTags(List.of(ExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)))
			.withId(ID)
			.withModified(MODIFIED)
			.withPriority(Priority.valueOf(PRIORITY))
			.withReporterUserId(REPORTER_USER_ID)
			.withStatusTag(STATUS_TAG)
			.withTitle(TITLE)
			.withTypeTag(TYPE_TAG);
	}
}
