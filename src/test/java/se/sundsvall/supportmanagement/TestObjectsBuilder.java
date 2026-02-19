package se.sundsvall.supportmanagement;

import generated.se.sundsvall.citizen.CitizenExtended;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

import static java.time.OffsetDateTime.now;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.HIGH;

public class TestObjectsBuilder {

	private static final String MUNICIPALITY_ID = "2281";
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
	private static final String FILE_NAME = "fileName";
	private static final String MIME_TYPE = "mimeType";
	private static final String ID = "id";
	private static final String NOTIFICATION_OWNER = "Test Owner";
	private static final String NOTIFICATION_OWNER_ID = "TE07ST";
	private static final String NOTIFICATION_CREATED_BY = "TestUser";
	private static final String NOTIFICATION_TYPE = "TestType";
	private static final String NOTIFICATION_SUBTYPE = "TestSubtype";
	private static final String NOTIFICATION_DESCRIPTION = "TestDescription";
	private static final String NOTIFICATION_CONTENT = "TestContent";
	private static final String PARAMETER_KEY = "parameterKey";
	private static final String PARAMETER_VALUE = "parameterValue";
	private static final String PARAMETER_ID = "parameterId";
	private static final boolean NOTIFICATION_ACKNOWLEDGED = false;
	private static final boolean NOTIFICATION_GLOBAL_ACKNOWLEDGED = false;

	private static final ErrandEntity NOTIFICATION_ERRAND_ENTITY = ErrandEntity.create()
		.withAssignedUserId(NOTIFICATION_OWNER_ID)
		.withId("cb20c51f-fcf3-42c0-b613-de563634a8ec")
		.withErrandNumber("ERRAND-NUMBER");

	private static final OffsetDateTime NOTIFICATION_CREATED = OffsetDateTime.now();

	private static final OffsetDateTime NOTIFICATION_MODIFIED = OffsetDateTime.now();

	private static final OffsetDateTime NOTIFICATION_EXPIRES = OffsetDateTime.now().plusDays(1);

	private static final String CONTACT_REASON = "reason";

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
			.withParameters(List.of(createParameterEntity()))
			.withNotifications(List.of(createNotificationEntity(null)))
			.withType(TYPE);
	}

	public static ParameterEntity createParameterEntity() {
		return ParameterEntity.create()
			.withId(PARAMETER_ID)
			.withKey(PARAMETER_KEY)
			.withValues(List.of(PARAMETER_VALUE));
	}

	public static AttachmentEntity buildAttachmentEntity(final ErrandEntity errandEntity) {
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
			.withTitle(TITLE)
			.withContactReason(CONTACT_REASON);
	}

	public static Notification createNotification(final Consumer<Notification> modifier) {
		final var notification = Notification.create()
			.withCreated(NOTIFICATION_CREATED)
			.withModified(NOTIFICATION_MODIFIED)
			.withOwnerFullName(NOTIFICATION_OWNER)
			.withOwnerId(NOTIFICATION_OWNER_ID)
			.withCreatedBy(NOTIFICATION_CREATED_BY)
			.withType(NOTIFICATION_TYPE)
			.withSubtype(NOTIFICATION_SUBTYPE)
			.withDescription(NOTIFICATION_DESCRIPTION)
			.withContent(NOTIFICATION_CONTENT)
			.withExpires(NOTIFICATION_EXPIRES)
			.withAcknowledged(NOTIFICATION_ACKNOWLEDGED)
			.withGlobalAcknowledged(NOTIFICATION_GLOBAL_ACKNOWLEDGED)
			.withErrandId(NOTIFICATION_ERRAND_ENTITY.getId());

		Optional.ofNullable(modifier).ifPresent(m -> m.accept(notification));

		return notification;
	}

	public static NotificationEntity createNotificationEntity(final Consumer<NotificationEntity> modifier) {
		final var notification = NotificationEntity.create()
			.withCreated(NOTIFICATION_CREATED)
			.withModified(NOTIFICATION_MODIFIED)
			.withOwnerFullName(NOTIFICATION_OWNER)
			.withOwnerId(NOTIFICATION_OWNER_ID)
			.withCreatedBy(NOTIFICATION_CREATED_BY)
			.withType(NOTIFICATION_TYPE)
			.withSubtype(NOTIFICATION_SUBTYPE)
			.withDescription(NOTIFICATION_DESCRIPTION)
			.withContent(NOTIFICATION_CONTENT)
			.withExpires(NOTIFICATION_EXPIRES)
			.withAcknowledged(NOTIFICATION_ACKNOWLEDGED)
			.withGlobalAcknowledged(NOTIFICATION_GLOBAL_ACKNOWLEDGED)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withErrandEntity(NOTIFICATION_ERRAND_ENTITY);

		Optional.ofNullable(modifier).ifPresent(m -> m.accept(notification));

		return notification;
	}

	public static CitizenExtended createCitizenExtended(final String firstName, final String lastName) {
		final var citizenExtended = new CitizenExtended();
		citizenExtended.setGivenname(firstName);
		citizenExtended.setLastname(lastName);
		return citizenExtended;
	}
}
