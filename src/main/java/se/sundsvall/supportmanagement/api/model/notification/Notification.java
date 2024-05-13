package se.sundsvall.supportmanagement.api.model.notification;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;

import org.springframework.format.annotation.DateTimeFormat;

import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

import io.swagger.v3.oas.annotations.media.Schema;

public class Notification {

	@Null(groups = {OnCreate.class})
	@Schema(description = "Unique identifier for the notification", example = "123e4567-e89b-12d3-a456-426614174000")
	private String id;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@Schema(description = "Timestamp when the notification was created", example = "2000-10-31T01:30:00.000+02:00")
	private OffsetDateTime created;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@Schema(description = "Timestamp when the notification was last modified", example = "2000-10-31T01:30:00.000+02:00")
	private OffsetDateTime modified;

	@NotBlank
	@Schema(description = "Name of the owner of the notification", example = "Test Testorsson")
	private String ownerFullName;

	@NotBlank
	@Schema(description = "Owner id of the notification", example = "AD01")
	private String ownerId;

	@Schema(description = "User who created the notification", example = "TestUser")
	private String createdBy;

	@Schema(description = "Full name of the user who created the notification", example = "Test Testorsson", accessMode = READ_ONLY)
	@Null(groups = {OnCreate.class, OnUpdate.class})
	private String createdByFullName;

	@NotBlank
	@Schema(description = "Type of the notification", example = "CREATE")
	private String type;

	@NotBlank
	@Schema(description = "Description of the notification", example = "Some description of the notification")
	private String description;

	@Schema(description = "Content of the notification", example = "Some content of the notification")
	private String content;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@Schema(description = "Timestamp when the notification expires", example = "2000-10-31T01:30:00.000+02:00")
	private OffsetDateTime expires;

	@Schema(description = "Acknowledged status of the notification", example = "true")
	private boolean acknowledged;

	@ValidUuid
	@Schema(description = "Errand id of the notification", example = "f0882f1d-06bc-47fd-b017-1d8307f5ce95")
	private String errandId;

	@Schema(description = "Errand number of the notification", example = "PRH-2022-000001", accessMode = READ_ONLY)
	@Null(groups = {OnCreate.class, OnUpdate.class})
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
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Notification that = (Notification) o;
		return acknowledged == that.acknowledged && Objects.equals(id, that.id) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified) && Objects.equals(ownerFullName, that.ownerFullName) && Objects.equals(ownerId, that.ownerId) && Objects.equals(createdBy, that.createdBy) && Objects.equals(createdByFullName, that.createdByFullName) && Objects.equals(type, that.type) && Objects.equals(description, that.description) && Objects.equals(content, that.content) && Objects.equals(expires, that.expires) && Objects.equals(errandId, that.errandId) && Objects.equals(errandNumber, that.errandNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, created, modified, ownerFullName, ownerId, createdBy, createdByFullName, type, description, content, expires, acknowledged, errandId, errandNumber);
	}

	@Override
	public String toString() {
		return "Notification{" +
			"id=" + id +
			", created=" + created +
			", modified=" + modified +
			", ownerFullName='" + ownerFullName + '\'' +
			", ownerId='" + ownerId + '\'' +
			", createdBy='" + createdBy + '\'' +
			", createdByFullName='" + createdByFullName + '\'' +
			", type='" + type + '\'' +
			", description='" + description + '\'' +
			", content='" + content + '\'' +
			", expires=" + expires +
			", acknowledged=" + acknowledged +
			", errandId='" + errandId + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			'}';
	}

}
