package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "phase_transition",
	indexes = {
		@Index(name = "idx_phase_transition_phase_id", columnList = "phase_id")
	})
public class PhaseTransitionEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "phase_id", nullable = false, foreignKey = @ForeignKey(name = "fk_phase_transition_phase_id"))
	private PhaseEntity phaseEntity;

	@Column(name = "target_phase_id", nullable = false)
	private String targetPhaseId;

	@Column(name = "description")
	private String description;

	@Column(name = "deprecated", nullable = false)
	private boolean deprecated;

	public static PhaseTransitionEntity create() {
		return new PhaseTransitionEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public PhaseTransitionEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public PhaseEntity getPhaseEntity() {
		return phaseEntity;
	}

	public void setPhaseEntity(final PhaseEntity phaseEntity) {
		this.phaseEntity = phaseEntity;
	}

	public PhaseTransitionEntity withPhaseEntity(final PhaseEntity phaseEntity) {
		this.phaseEntity = phaseEntity;
		return this;
	}

	public String getTargetPhaseId() {
		return targetPhaseId;
	}

	public void setTargetPhaseId(final String targetPhaseId) {
		this.targetPhaseId = targetPhaseId;
	}

	public PhaseTransitionEntity withTargetPhaseId(final String targetPhaseId) {
		this.targetPhaseId = targetPhaseId;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public PhaseTransitionEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
	}

	public PhaseTransitionEntity withDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, targetPhaseId, description, deprecated);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final PhaseTransitionEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(targetPhaseId, other.targetPhaseId) && Objects.equals(description, other.description) && deprecated == other.deprecated;
	}

	@Override
	public String toString() {
		return "PhaseTransitionEntity{" +
			"id='" + id + '\'' +
			", targetPhaseId='" + targetPhaseId + '\'' +
			", description='" + description + '\'' +
			", deprecated=" + deprecated +
			'}';
	}
}
