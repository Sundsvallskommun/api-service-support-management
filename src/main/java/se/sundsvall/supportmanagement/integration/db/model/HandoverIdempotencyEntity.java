package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "handover_idempotency",
	indexes = @Index(name = "idx_handover_idempotency_expires_at", columnList = "expires_at"),
	uniqueConstraints = @UniqueConstraint(name = "uq_handover_idempotency_key", columnNames = "idempotency_key"))
public class HandoverIdempotencyEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "idempotency_key", nullable = false, length = 255)
	private String idempotencyKey;

	@Column(name = "new_errand_id", length = 36)
	private String newErrandId;

	@Column(name = "new_errand_number", length = 255)
	private String newErrandNumber;

	@Column(name = "target_namespace", length = 255)
	private String targetNamespace;

	@Column(name = "target_municipality_id", length = 16)
	private String targetMunicipalityId;

	@Column(name = "relation_id", length = 255)
	private String relationId;

	@Column(name = "warnings", length = LONG32)
	private String warnings;

	@Column(name = "created_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime createdAt;

	@Column(name = "expires_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime expiresAt;

	public static HandoverIdempotencyEntity create() {
		return new HandoverIdempotencyEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public HandoverIdempotencyEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public void setIdempotencyKey(final String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}

	public HandoverIdempotencyEntity withIdempotencyKey(final String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
		return this;
	}

	public String getNewErrandId() {
		return newErrandId;
	}

	public void setNewErrandId(final String newErrandId) {
		this.newErrandId = newErrandId;
	}

	public HandoverIdempotencyEntity withNewErrandId(final String newErrandId) {
		this.newErrandId = newErrandId;
		return this;
	}

	public String getNewErrandNumber() {
		return newErrandNumber;
	}

	public void setNewErrandNumber(final String newErrandNumber) {
		this.newErrandNumber = newErrandNumber;
	}

	public HandoverIdempotencyEntity withNewErrandNumber(final String newErrandNumber) {
		this.newErrandNumber = newErrandNumber;
		return this;
	}

	public String getTargetNamespace() {
		return targetNamespace;
	}

	public void setTargetNamespace(final String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}

	public HandoverIdempotencyEntity withTargetNamespace(final String targetNamespace) {
		this.targetNamespace = targetNamespace;
		return this;
	}

	public String getTargetMunicipalityId() {
		return targetMunicipalityId;
	}

	public void setTargetMunicipalityId(final String targetMunicipalityId) {
		this.targetMunicipalityId = targetMunicipalityId;
	}

	public HandoverIdempotencyEntity withTargetMunicipalityId(final String targetMunicipalityId) {
		this.targetMunicipalityId = targetMunicipalityId;
		return this;
	}

	public String getRelationId() {
		return relationId;
	}

	public void setRelationId(final String relationId) {
		this.relationId = relationId;
	}

	public HandoverIdempotencyEntity withRelationId(final String relationId) {
		this.relationId = relationId;
		return this;
	}

	public String getWarnings() {
		return warnings;
	}

	public void setWarnings(final String warnings) {
		this.warnings = warnings;
	}

	public HandoverIdempotencyEntity withWarnings(final String warnings) {
		this.warnings = warnings;
		return this;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(final OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public HandoverIdempotencyEntity withCreatedAt(final OffsetDateTime createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(final OffsetDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public HandoverIdempotencyEntity withExpiresAt(final OffsetDateTime expiresAt) {
		this.expiresAt = expiresAt;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final HandoverIdempotencyEntity that = (HandoverIdempotencyEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(idempotencyKey, that.idempotencyKey)
			&& Objects.equals(newErrandId, that.newErrandId) && Objects.equals(newErrandNumber, that.newErrandNumber)
			&& Objects.equals(targetNamespace, that.targetNamespace) && Objects.equals(targetMunicipalityId, that.targetMunicipalityId)
			&& Objects.equals(relationId, that.relationId) && Objects.equals(warnings, that.warnings)
			&& Objects.equals(createdAt, that.createdAt) && Objects.equals(expiresAt, that.expiresAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, idempotencyKey, newErrandId, newErrandNumber, targetNamespace, targetMunicipalityId, relationId, warnings, createdAt, expiresAt);
	}

	@Override
	public String toString() {
		return "HandoverIdempotencyEntity{id='" + id + "', idempotencyKey='" + idempotencyKey + "', newErrandId='" + newErrandId
			+ "', newErrandNumber='" + newErrandNumber + "', targetNamespace='" + targetNamespace + "', targetMunicipalityId='"
			+ targetMunicipalityId + "', relationId='" + relationId + "', warnings='" + warnings + "', createdAt=" + createdAt + ", expiresAt=" + expiresAt + "}";
	}
}
