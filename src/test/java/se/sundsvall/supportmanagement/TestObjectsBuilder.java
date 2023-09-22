package se.sundsvall.supportmanagement;

import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.HIGH;

public class TestObjectsBuilder {

	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String ASSIGNED_GROUP_ID = "assignedGroupId";
	private static final String ASSIGNED_USER_ID = "assignedUserId";
	private static final String CATEGORY = "category";
	private static final String NAMESPACE = "namespace";
	private static final OffsetDateTime CREATED = now().minusWeeks(1);
	private static final String EXTERNAL_ID = "externalId";
	private static final String EXTERNAL_ID_TYPE = "PRIVATE";
	private static final String TAG_KEY = "tagKey";
	private static final String TAG_VALUE = "tagValue";
	private static final OffsetDateTime MODIFIED = now();
	private static final String PRIORITY = HIGH.name();
	private static final String REPORTER_USER_ID = "reporterUserId";
	private static final String STATUS = "status";
	private static final String TITLE = "title";
	private static final String TYPE = "type";
	private static final String ATTACHMENT_ID = "attachmentId";
	private static final String FILE = "file";
	private static final String FILE_NAME = "fileName";
	private static final String MIME_TYPE = "mimeType";
	private static final String ID = "id";

	public static ErrandEntity buildErrandEntity() {
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withAssignedGroupId(ASSIGNED_GROUP_ID)
			.withAssignedUserId(ASSIGNED_USER_ID)
			.withAttachments(null)
			.withCategory(CATEGORY)
			.withCreated(CREATED)
			.withNamespace(NAMESPACE)
			.withStakeholders(new ArrayList<>(List.of(StakeholderEntity.create().withExternalId(EXTERNAL_ID).withExternalIdType(EXTERNAL_ID_TYPE))))
			.withExternalTags(List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)))
			.withModified(MODIFIED)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withPriority(PRIORITY)
			.withReporterUserId(REPORTER_USER_ID)
			.withStatus(STATUS)
			.withTitle(TITLE)
			.withType(TYPE);
	}

	public static AttachmentEntity buildAttachmentEntity(ErrandEntity errandEntity) {
		return AttachmentEntity.create()
			.withId(ATTACHMENT_ID)
			.withFileName(FILE_NAME)
			.withAttachmentData(null)
			.withMimeType(MIME_TYPE)
			.withErrandEntity(errandEntity);
	}

	public static Errand buildErrand() {
		return Errand.create()
			.withAssignedGroupId(ASSIGNED_GROUP_ID)
			.withAssignedUserId(ASSIGNED_USER_ID)
			.withClassification(Classification.create().withCategory(CATEGORY).withType(TYPE))
			.withCreated(CREATED)
			.withStakeholders(List.of(Stakeholder.create().withExternalId(EXTERNAL_ID).withExternalIdType(EXTERNAL_ID_TYPE)))
			.withExternalTags(List.of(ExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)))
			.withId(ID)
			.withModified(MODIFIED)
			.withPriority(Priority.valueOf(PRIORITY))
			.withReporterUserId(REPORTER_USER_ID)
			.withStatus(STATUS)
			.withTitle(TITLE);
	}
}
