package se.sundsvall.supportmanagement.api.model.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.model.metadata.AffectedAction;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobType;

@Schema(description = "Job response")
public class JobResponse {

	@Schema(description = "Job ID")
	private String jobId;

	@Schema(description = "Job type")
	private JobType type;

	@Schema(description = "Job status")
	private JobStatus status;

	@Schema(description = "Progress percentage (0-100)")
	private Integer progress;

	@Schema(description = "Total number of items to process")
	private Integer total;

	@Schema(description = "Number of items processed so far")
	private Integer processed;

	@Schema(description = "Error message, populated on FAILED status")
	private String message;

	@Schema(description = "When the job was created")
	private OffsetDateTime created;

	@Schema(description = "When the job was last updated")
	private OffsetDateTime modified;

	@Schema(description = "Actions affected by the operation this job carries out - populated only on the response "
		+ "returned when the job is started, not on later reads through GET, since it reflects a dry-run-style "
		+ "computation done once at start time rather than state stored on the job itself")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private List<AffectedAction> affectedActions;

	public static JobResponse create() {
		return new JobResponse();
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(final String jobId) {
		this.jobId = jobId;
	}

	public JobResponse withJobId(final String jobId) {
		this.jobId = jobId;
		return this;
	}

	public JobType getType() {
		return type;
	}

	public void setType(final JobType type) {
		this.type = type;
	}

	public JobResponse withType(final JobType type) {
		this.type = type;
		return this;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(final JobStatus status) {
		this.status = status;
	}

	public JobResponse withStatus(final JobStatus status) {
		this.status = status;
		return this;
	}

	public Integer getProgress() {
		return progress;
	}

	public void setProgress(final Integer progress) {
		this.progress = progress;
	}

	public JobResponse withProgress(final Integer progress) {
		this.progress = progress;
		return this;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(final Integer total) {
		this.total = total;
	}

	public JobResponse withTotal(final Integer total) {
		this.total = total;
		return this;
	}

	public Integer getProcessed() {
		return processed;
	}

	public void setProcessed(final Integer processed) {
		this.processed = processed;
	}

	public JobResponse withProcessed(final Integer processed) {
		this.processed = processed;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public JobResponse withMessage(final String message) {
		this.message = message;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public JobResponse withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public JobResponse withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public List<AffectedAction> getAffectedActions() {
		return affectedActions;
	}

	public void setAffectedActions(final List<AffectedAction> affectedActions) {
		this.affectedActions = affectedActions;
	}

	public JobResponse withAffectedActions(final List<AffectedAction> affectedActions) {
		this.affectedActions = affectedActions;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobId, type, status, progress, total, processed, message, created, modified, affectedActions);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final JobResponse other)) {
			return false;
		}
		return Objects.equals(jobId, other.jobId) && type == other.type && status == other.status
			&& Objects.equals(progress, other.progress) && Objects.equals(total, other.total)
			&& Objects.equals(processed, other.processed) && Objects.equals(message, other.message)
			&& Objects.equals(created, other.created) && Objects.equals(modified, other.modified)
			&& Objects.equals(affectedActions, other.affectedActions);
	}

	@Override
	public String toString() {
		return "JobResponse [jobId=" + jobId + ", type=" + type + ", status=" + status + ", progress=" + progress
			+ ", total=" + total + ", processed=" + processed + ", message=" + message
			+ ", created=" + created + ", modified=" + modified + ", affectedActions=" + affectedActions + "]";
	}
}
