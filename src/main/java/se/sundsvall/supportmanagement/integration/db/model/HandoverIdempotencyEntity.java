package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.Length.LONG32;

@Entity
@Table(name = "handover_idempotency",
	uniqueConstraints = @UniqueConstraint(name = "uq_handover_source_target", columnNames = {
		"source_errand_id", "target_namespace", "target_municipality_id"
	}))
public class HandoverIdempotencyEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "source_errand_id", nullable = false, length = 36)
	private String sourceErrandId;

	@Column(name = "new_errand_id", length = 36)
	private String newErrandId;

	@Column(name = "new_errand_number", length = 255)
	private String newErrandNumber;

	@Column(name = "target_namespace", nullable = false, length = 255)
	private String targetNamespace;

	@Column(name = "target_municipality_id", nullable = false, length = 16)
	private String targetMunicipalityId;

	@Column(name = "relation_id", length = 255)
	private String relationId;

	@Column(name = "warnings", length = LONG32)
	private String warnings;

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

	public String getSourceErrandId() {
		return sourceErrandId;
	}

	public void setSourceErrandId(final String sourceErrandId) {
		this.sourceErrandId = sourceErrandId;
	}

	public HandoverIdempotencyEntity withSourceErrandId(final String sourceErrandId) {
		this.sourceErrandId = sourceErrandId;
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

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final HandoverIdempotencyEntity that = (HandoverIdempotencyEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(sourceErrandId, that.sourceErrandId)
			&& Objects.equals(newErrandId, that.newErrandId) && Objects.equals(newErrandNumber, that.newErrandNumber)
			&& Objects.equals(targetNamespace, that.targetNamespace) && Objects.equals(targetMunicipalityId, that.targetMunicipalityId)
			&& Objects.equals(relationId, that.relationId) && Objects.equals(warnings, that.warnings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, sourceErrandId, newErrandId, newErrandNumber, targetNamespace, targetMunicipalityId, relationId, warnings);
	}

	@Override
	public String toString() {
		return "HandoverIdempotencyEntity{id='" + id + "', sourceErrandId='" + sourceErrandId + "', newErrandId='" + newErrandId
			+ "', newErrandNumber='" + newErrandNumber + "', targetNamespace='" + targetNamespace + "', targetMunicipalityId='"
			+ targetMunicipalityId + "', relationId='" + relationId + "', warnings='" + warnings + "'}";
	}
}
