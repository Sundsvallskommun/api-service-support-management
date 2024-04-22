package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "notification",
	indexes = {
		@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
	})
public class NotificationEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@Column(name = "owner_full_name")
	private String ownerFullName;

	@Column(name = "owner_id")
	private String ownerId;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "type")
	private String type;

	@Column(name = "description")
	private String description;

	@Column(name = "content")
	private String content;

	@Column(name = "expires")
	private OffsetDateTime expires;

	@NotNull
	@Column(name = "acknowledged")
	private boolean acknowledged;

	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	public static NotificationEntity create() {
		return new NotificationEntity();
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public NotificationEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NotificationEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public NotificationEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public String getOwnerFullName() {
		return ownerFullName;
	}

	public void setOwnerFullName(final String ownerFullName) {
		this.ownerFullName = ownerFullName;
	}

	public NotificationEntity withOwnerFullName(final String ownerFullName) {
		this.ownerFullName = ownerFullName;
		return this;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(final String ownerId) {
		this.ownerId = ownerId;
	}

	public NotificationEntity withOwnerId(final String ownerId) {
		this.ownerId = ownerId;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public NotificationEntity withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public NotificationEntity withType(final String type) {
		this.type = type;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public NotificationEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public NotificationEntity withContent(final String content) {
		this.content = content;
		return this;
	}

	public OffsetDateTime getExpires() {
		return expires;
	}

	public void setExpires(final OffsetDateTime expires) {
		this.expires = expires;
	}

	public NotificationEntity withExpires(final OffsetDateTime expires) {
		this.expires = expires;
		return this;
	}

	public boolean isAcknowledged() {
		return acknowledged;
	}

	public void setAcknowledged(final boolean acknowledged) {
		this.acknowledged = acknowledged;
	}

	public NotificationEntity withAcknowledged(final boolean acknowledged) {
		this.acknowledged = acknowledged;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public NotificationEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public NotificationEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}


	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public NotificationEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final NotificationEntity that = (NotificationEntity) o;
		return acknowledged == that.acknowledged && Objects.equals(id, that.id) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified) && Objects.equals(ownerFullName, that.ownerFullName) && Objects.equals(ownerId, that.ownerId) && Objects.equals(createdBy, that.createdBy) && Objects.equals(type, that.type) && Objects.equals(description, that.description) && Objects.equals(content, that.content) && Objects.equals(expires, that.expires) && Objects.equals(errandId, that.errandId) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, created, modified, ownerFullName, ownerId, createdBy, type, description, content, expires, acknowledged, errandId, municipalityId, namespace);
	}

	@Override
	public String toString() {
		return "NotificationEntity{" +
			"id='" + id + '\'' +
			", created=" + created +
			", modified=" + modified +
			", ownerFullName='" + ownerFullName + '\'' +
			", ownerId='" + ownerId + '\'' +
			", createdBy='" + createdBy + '\'' +
			", type='" + type + '\'' +
			", description='" + description + '\'' +
			", content='" + content + '\'' +
			", expires=" + expires +
			", acknowledged=" + acknowledged +
			", errandId='" + errandId + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			'}';
	}

}
