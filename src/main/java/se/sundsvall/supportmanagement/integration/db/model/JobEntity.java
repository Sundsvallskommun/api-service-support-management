package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobType;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "job",
	indexes = {
		@Index(name = "idx_job_namespace_municipality_id_status", columnList = "namespace, municipality_id, status")
	})
public class JobEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private JobType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private JobStatus status;

	@Column(name = "progress")
	private Integer progress;

	@Column(name = "total")
	private Integer total;

	@Column(name = "processed")
	private Integer processed;

	@Column(name = "message", columnDefinition = "text")
	private String message;

	@Column(name = "created", updatable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static JobEntity create() {
		return new JobEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public JobEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public JobEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public JobEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public JobType getType() {
		return type;
	}

	public void setType(final JobType type) {
		this.type = type;
	}

	public JobEntity withType(final JobType type) {
		this.type = type;
		return this;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(final JobStatus status) {
		this.status = status;
	}

	public JobEntity withStatus(final JobStatus status) {
		this.status = status;
		return this;
	}

	public Integer getProgress() {
		return progress;
	}

	public void setProgress(final Integer progress) {
		this.progress = progress;
	}

	public JobEntity withProgress(final Integer progress) {
		this.progress = progress;
		return this;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(final Integer total) {
		this.total = total;
	}

	public JobEntity withTotal(final Integer total) {
		this.total = total;
		return this;
	}

	public Integer getProcessed() {
		return processed;
	}

	public void setProcessed(final Integer processed) {
		this.processed = processed;
	}

	public JobEntity withProcessed(final Integer processed) {
		this.processed = processed;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public JobEntity withMessage(final String message) {
		this.message = message;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public JobEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public JobEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@PrePersist
	void onCreate() {
		created = now(systemDefault()).truncatedTo(MILLIS);
		if (status == null) {
			status = JobStatus.PENDING;
		}
		if (progress == null) {
			progress = 0;
		}
		if (processed == null) {
			processed = 0;
		}
	}

	@PreUpdate
	void onUpdate() {
		modified = now(systemDefault()).truncatedTo(MILLIS);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, type, status, progress, total, processed, message, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final JobEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace)
			&& type == other.type && status == other.status && Objects.equals(progress, other.progress) && Objects.equals(total, other.total)
			&& Objects.equals(processed, other.processed) && Objects.equals(message, other.message)
			&& Objects.equals(created, other.created) && Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "JobEntity [id=" + id + ", municipalityId=" + municipalityId + ", namespace=" + namespace
			+ ", type=" + type + ", status=" + status + ", progress=" + progress + ", total=" + total
			+ ", processed=" + processed + ", message=" + message + ", created=" + created + ", modified=" + modified + "]";
	}
}
