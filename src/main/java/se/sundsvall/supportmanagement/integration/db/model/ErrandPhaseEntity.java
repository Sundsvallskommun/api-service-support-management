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
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "errand_phase",
	indexes = {
		@Index(name = "idx_errand_phase_errand_id", columnList = "errand_id"),
		@Index(name = "idx_errand_phase_phase_id", columnList = "phase_id")
	})
public class ErrandPhaseEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_errand_phase_errand_id"))
	private ErrandEntity errandEntity;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "phase_id", nullable = false, foreignKey = @ForeignKey(name = "fk_errand_phase_phase_id"))
	private PhaseEntity phaseEntity;

	@Column(name = "started")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime started;

	@Column(name = "ended")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime ended;

	public static ErrandPhaseEntity create() {
		return new ErrandPhaseEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandPhaseEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public ErrandPhaseEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public PhaseEntity getPhaseEntity() {
		return phaseEntity;
	}

	public void setPhaseEntity(final PhaseEntity phaseEntity) {
		this.phaseEntity = phaseEntity;
	}

	public ErrandPhaseEntity withPhaseEntity(final PhaseEntity phaseEntity) {
		this.phaseEntity = phaseEntity;
		return this;
	}

	public OffsetDateTime getStarted() {
		return started;
	}

	public void setStarted(final OffsetDateTime started) {
		this.started = started;
	}

	public ErrandPhaseEntity withStarted(final OffsetDateTime started) {
		this.started = started;
		return this;
	}

	public OffsetDateTime getEnded() {
		return ended;
	}

	public void setEnded(final OffsetDateTime ended) {
		this.ended = ended;
	}

	public ErrandPhaseEntity withEnded(final OffsetDateTime ended) {
		this.ended = ended;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, started, ended);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ErrandPhaseEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(started, other.started) && Objects.equals(ended, other.ended);
	}

	@Override
	public String toString() {
		return "ErrandPhaseEntity{" +
			"id='" + id + '\'' +
			", started=" + started +
			", ended=" + ended +
			'}';
	}
}
