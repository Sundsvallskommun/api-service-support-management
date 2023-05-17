package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.Length.LONG32;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "revision",
	indexes = {
		@Index(name = "revision_entity_id_index", columnList = "entity_id"),
		@Index(name = "revision_entity_type_index", columnList = "entity_type")
	})
public class RevisionEntity implements Serializable {

	private static final long serialVersionUID = 7389828147898967316L;

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "id")
	private String id;

	@Column(name = "entity_id")
	private String entityId;

	@Column(name = "entity_type")
	private String entityType;

	@Column(name = "version")
	private Integer version;

	@Column(name = "serialized_snapshot", length = LONG32)
	private String serializedSnapshot;

	@Column(name = "created")
	private OffsetDateTime created;

	public static RevisionEntity create() {
		return new RevisionEntity();
	}

	@PrePersist
	void prePersist() {
		created = now(ZoneId.systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public RevisionEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public RevisionEntity withEntityId(String entityId) {
		this.entityId = entityId;
		return this;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public RevisionEntity withEntityType(String entityType) {
		this.entityType = entityType;
		return this;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public RevisionEntity withVersion(Integer version) {
		this.version = version;
		return this;
	}

	public String getSerializedSnapshot() {
		return serializedSnapshot;
	}

	public void setSerializedSnapshot(String serializedSnapshot) {
		this.serializedSnapshot = serializedSnapshot;
	}

	public RevisionEntity withSerializedSnapshot(String serializedSnapshot) {
		this.serializedSnapshot = serializedSnapshot;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public RevisionEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, entityId, entityType, id, serializedSnapshot, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RevisionEntity other)) {
			return false;
		}
		return Objects.equals(created, other.created) && Objects.equals(entityId, other.entityId) && Objects.equals(entityType, other.entityType) && Objects.equals(id, other.id) && Objects.equals(serializedSnapshot, other.serializedSnapshot)
			&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("RevisionEntity [id=").append(id).append(", entityId=").append(entityId).append(", entityType=").append(entityType).append(", version=").append(version).append(", serializedSnapshot=").append(serializedSnapshot).append(
			", created=").append(created).append("]");
		return builder.toString();
	}

}
