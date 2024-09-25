package se.sundsvall.supportmanagement.integration.db.model;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "revision",
	indexes = {
		@Index(name = "revision_entity_id_index", columnList = "entity_id"),
		@Index(name = "revision_entity_type_index", columnList = "entity_type"),
		@Index(name = "revision_municipality_id_index", columnList = "municipality_id"),
		@Index(name = "revision_namespace_index", columnList = "namespace")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_entity_id_version", columnNames = {"entity_id", "version"})
	})
public class RevisionEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "entity_id")
	private String entityId;

	@Column(name = "namespace")
	private String namespace;

	@Column(name = "municipality_id")
	private String municipalityId;

	@Column(name = "entity_type")
	private String entityType;

	@Column(name = "version")
	private Integer version;

	@Column(name = "serialized_snapshot", length = LONG32)
	private String serializedSnapshot;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
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

	public void setId(final String id) {
		this.id = id;
	}

	public RevisionEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(final String entityId) {
		this.entityId = entityId;
	}

	public RevisionEntity withEntityId(final String entityId) {
		this.entityId = entityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public RevisionEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public RevisionEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(final String entityType) {
		this.entityType = entityType;
	}

	public RevisionEntity withEntityType(final String entityType) {
		this.entityType = entityType;
		return this;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(final Integer version) {
		this.version = version;
	}

	public RevisionEntity withVersion(final Integer version) {
		this.version = version;
		return this;
	}

	public String getSerializedSnapshot() {
		return serializedSnapshot;
	}

	public void setSerializedSnapshot(final String serializedSnapshot) {
		this.serializedSnapshot = serializedSnapshot;
	}

	public RevisionEntity withSerializedSnapshot(final String serializedSnapshot) {
		this.serializedSnapshot = serializedSnapshot;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public RevisionEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final RevisionEntity that = (RevisionEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(entityId, that.entityId) && Objects.equals(namespace, that.namespace) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(entityType, that.entityType) && Objects.equals(version, that.version) && Objects.equals(serializedSnapshot, that.serializedSnapshot) && Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, entityId, namespace, municipalityId, entityType, version, serializedSnapshot, created);
	}

	@Override
	public String toString() {
		return "RevisionEntity{" +
			"id='" + id + '\'' +
			", entityId='" + entityId + '\'' +
			", namespace='" + namespace + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", entityType='" + entityType + '\'' +
			", version=" + version +
			", serializedSnapshot='" + serializedSnapshot + '\'' +
			", created=" + created +
			'}';
	}

}
