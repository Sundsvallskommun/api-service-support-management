package se.sundsvall.supportmanagement.api.model.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

public class Notification {

	@Null(groups = {
		OnCreate.class
	})
	@ValidUuid(groups = {
		OnUpdate.class
	})
	@Schema(description = "Unique identifier for the notification", examples = "123e4567-e89b-12d3-a456-426614174000")
	private String id;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the notification was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the notification was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	@Schema(description = "Name of the owner of the notification", examples = "Test Testorsson", accessMode = READ_ONLY)
	private String ownerFullName;

	@NotBlank(groups = {
		OnCreate.class
	})
	@Schema(description = "Owner id of the notification", examples = "AD01")
	private String ownerId;

	@Schema(description = "User who created the notification", examples = "TestUser", accessMode = READ_ONLY)
	private String createdBy;

	@Schema(description = "Full name of the user who created the notification", examples = "Test Testorsson", accessMode = READ_ONLY)
	private String createdByFullName;

	@NotBlank(groups = {
		OnCreate.class
	})
	@Schema(description = "Type of the notification", examples = "CREATE")
	private String type;

	@Schema(description = "Subtype of the notification", examples = "ATTACHMENT")
	private String subtype;

	@NotBlank(groups = {
		OnCreate.class
	})
	@Schema(description = "Description of the notification", examples = "Some description of the notification")
	private String description;

	@Schema(description = "Content of the notification", examples = "Some content of the notification")
	private String content;

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the notification expires", examples = "2000-10-31T01:30:00.000+02:00")
	private OffsetDateTime expires;

	@Schema(description = "Acknowledged status of the notification (global level). I.e. this notification is acknowledged by anyone.", examples = "true")
	private boolean globalAcknowledged;

	@Schema(description = "Acknowledged status of the notification (owner level). I.e. this notification is acknowledged by the owner of this notification.", examples = "true")
	private boolean acknowledged;

	@Null(groups = {
		OnCreate.class
	})
	@ValidUuid(groups = {
		OnUpdate.class
	})
	@Schema(description = "Errand id of the notification", examples = "f0882f1d-06bc-47fd-b017-1d8307f5ce95", accessMode = READ_ONLY)
	private String errandId;

	@Schema(description = "Errand number of the notification", examples = "PRH-2022-000001", accessMode = READ_ONLY)
	private String errandNumber;

	public static Notification create() {
		return new Notification();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Notification withId(final String id) {
		this.id = id;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Notification withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Notification withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public String getOwnerFullName() {
		return ownerFullName;
	}

	public void setOwnerFullName(final String ownerFullName) {
		this.ownerFullName = ownerFullName;
	}

	public Notification withOwnerFullName(final String ownerFullName) {
		this.ownerFullName = ownerFullName;
		return this;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(final String ownerId) {
		this.ownerId = ownerId;
	}

	public Notification withOwnerId(final String ownerId) {
		this.ownerId = ownerId;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public Notification withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getCreatedByFullName() {
		return createdByFullName;
	}

	public void setCreatedByFullName(final String createdByFullName) {
		this.createdByFullName = createdByFullName;
	}

	public Notification withCreatedByFullName(final String createdByFullName) {
		this.createdByFullName = createdByFullName;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Notification withType(final String type) {
		this.type = type;
		return this;
	}

	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(final String subtype) {
		this.subtype = subtype;
	}

	public Notification withSubtype(final String subtype) {
		this.subtype = subtype;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Notification withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public Notification withContent(final String content) {
		this.content = content;
		return this;
	}

	public OffsetDateTime getExpires() {
		return expires;
	}

	public void setExpires(final OffsetDateTime expires) {
		this.expires = expires;
	}

	public Notification withExpires(final OffsetDateTime expires) {
		this.expires = expires;
		return this;
	}

	public boolean isGlobalAcknowledged() {
		return globalAcknowledged;
	}

	public void setGlobalAcknowledged(final boolean globalAcknowledged) {
		this.globalAcknowledged = globalAcknowledged;
	}

	public Notification withGlobalAcknowledged(final boolean globalAcknowledged) {
		this.globalAcknowledged = globalAcknowledged;
		return this;
	}

	public boolean isAcknowledged() {
		return acknowledged;
	}

	public void setAcknowledged(final boolean acknowledged) {
		this.acknowledged = acknowledged;
	}

	public Notification withAcknowledged(final boolean acknowledged) {
		this.acknowledged = acknowledged;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public Notification withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public Notification withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Notification that = (Notification) o;
		return globalAcknowledged == that.globalAcknowledged && acknowledged == that.acknowledged && Objects.equals(id, that.id) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified)
			&& Objects.equals(ownerFullName, that.ownerFullName) && Objects.equals(ownerId, that.ownerId) && Objects.equals(createdBy, that.createdBy) && Objects.equals(createdByFullName, that.createdByFullName)
			&& Objects.equals(type, that.type) && Objects.equals(subtype, that.subtype) && Objects.equals(description, that.description) && Objects.equals(content, that.content) && Objects.equals(expires,
				that.expires) && Objects.equals(errandId, that.errandId) && Objects.equals(errandNumber, that.errandNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, created, modified, ownerFullName, ownerId, createdBy, createdByFullName, type, subtype, description, content, expires, globalAcknowledged, acknowledged, errandId, errandNumber);
	}

	@Override
	public String
		toString() {
		return "Notification{" +
			"id='" + id + '\'' +
			", created=" + created +
			", modified=" + modified +
			", ownerFullName='" + ownerFullName + '\'' +
			", ownerId='" + ownerId + '\'' +
			", createdBy='" + createdBy + '\'' +
			", createdByFullName='" + createdByFullName + '\'' +
			", type='" + type + '\'' +
			", subtype='" + subtype + '\'' +
			", description='" + description + '\'' +
			", content='" + content + '\'' +
			", expires=" + expires +
			", globalAcknowledged=" + globalAcknowledged +
			", acknowledged=" + acknowledged +
			", errandId='" + errandId + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			'}';
	}
}
