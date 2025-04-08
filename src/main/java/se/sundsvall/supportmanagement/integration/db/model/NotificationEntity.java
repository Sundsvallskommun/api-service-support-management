package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "notification",
	indexes = {
		@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id"),
		@Index(name = "idx_notification_municipality_id_namespace_owner_id", columnList = "municipality_id, namespace, owner_id")
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

	@Column(name = "created_by_full_name")
	private String createdByFullName;

	@Column(name = "type")
	private String type;

	@Column(name = "description")
	private String description;

	@Column(name = "content")
	private String content;

	@Column(name = "expires")
	private OffsetDateTime expires;

	@Column(name = "globalAcknowledged")
	@NotNull
	private boolean globalAcknowledged;

	@Column(name = "acknowledged")
	@NotNull
	private boolean acknowledged;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", foreignKey = @ForeignKey(name = "fk_notification_errand_id"))
	private ErrandEntity errandEntity;

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

	public String getCreatedByFullName() {
		return createdByFullName;
	}

	public void setCreatedByFullName(final String createdByFullName) {
		this.createdByFullName = createdByFullName;
	}

	public NotificationEntity withCreatedByFullName(final String createdByFullName) {
		this.createdByFullName = createdByFullName;
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

	public boolean isGlobalAcknowledged() {
		return globalAcknowledged;
	}

	public void setGlobalAcknowledged(boolean globalAcknowledged) {
		this.globalAcknowledged = globalAcknowledged;
	}

	public NotificationEntity withGlobalAcknowledged(boolean globalAcknowledged) {
		this.globalAcknowledged = globalAcknowledged;
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

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public NotificationEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
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
	public int hashCode() {
		return Objects.hash(acknowledged, content, created, createdBy, createdByFullName, description, errandEntity, expires, globalAcknowledged, id, modified, municipalityId, namespace, ownerFullName, ownerId, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		NotificationEntity other = (NotificationEntity) obj;
		return acknowledged == other.acknowledged && Objects.equals(content, other.content) && Objects.equals(created, other.created) && Objects.equals(createdBy, other.createdBy) && Objects.equals(createdByFullName, other.createdByFullName) && Objects
			.equals(description, other.description) && Objects.equals(errandEntity, other.errandEntity) && Objects.equals(expires, other.expires) && globalAcknowledged == other.globalAcknowledged && Objects.equals(id, other.id) && Objects.equals(modified,
				other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace) && Objects.equals(ownerFullName, other.ownerFullName) && Objects.equals(ownerId, other.ownerId) && Objects.equals(type,
					other.type);
	}

	@Override
	public String toString() {
		return "NotificationEntity [id=" + id + ", created=" + created + ", modified=" + modified + ", ownerFullName=" + ownerFullName + ", ownerId=" + ownerId + ", createdBy=" + createdBy + ", createdByFullName=" + createdByFullName + ", type=" + type
			+ ", description=" + description + ", content=" + content + ", expires=" + expires + ", globalAcknowledged=" + globalAcknowledged + ", acknowledged=" + acknowledged + ", errandEntity=" + errandEntity + ", municipalityId=" + municipalityId
			+ ", namespace=" + namespace + "]";
	}
}
