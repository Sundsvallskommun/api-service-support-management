package se.sundsvall.supportmanagement.api.model.errand.purge;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * Progress of a purge run, as returned when a run is started, polled or stopped.
 * <p>
 * The counters are the whole of what a caller can learn about a run. Ids of the errands that failed are deliberately
 * left out; they are written to the log instead, where the full set survives beyond the lifetime of the job.
 */
@Schema(description = "Errand purge status model")
public class ErrandPurgeStatus {

	@Schema(description = "Id of the purge job", examples = "b82bd8ac-1507-4d9a-958d-369261eecc15", accessMode = READ_ONLY)
	private String jobId;

	@Schema(description = "Namespace being purged", examples = "CONTACTCENTER", accessMode = READ_ONLY)
	private String namespace;

	@Schema(description = "Id of the municipality being purged", examples = "2281", accessMode = READ_ONLY)
	private String municipalityId;

	@Schema(description = "Cutoff the run was started with", examples = "2024-08-28T00:00:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime olderThan;

	@Schema(description = "Whether the run only counts errands instead of deleting them", examples = "true", accessMode = READ_ONLY)
	private boolean dryRun;

	@Schema(description = "State of the run", accessMode = READ_ONLY)
	private PurgeState state;

	@Schema(description = "Timestamp when the run started", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime started;

	@Schema(description = "Timestamp when the run finished. Null while it is still running.", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime finished;

	@Schema(description = "Number of errands handled so far, successfully or not", examples = "1000", accessMode = READ_ONLY)
	private long processed;

	@Schema(description = "Number of errands deleted so far. Stays at zero for a dry run.", examples = "998", accessMode = READ_ONLY)
	private long deleted;

	@Schema(description = "Number of errands that could not be deleted. The log holds their ids.", examples = "2", accessMode = READ_ONLY)
	private long failed;

	@Schema(description = "Reason the run aborted. Null unless the state is FAILED.", examples = "Access control is active for the namespace", accessMode = READ_ONLY)
	private String message;

	public static ErrandPurgeStatus create() {
		return new ErrandPurgeStatus();
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(final String jobId) {
		this.jobId = jobId;
	}

	public ErrandPurgeStatus withJobId(final String jobId) {
		this.jobId = jobId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public ErrandPurgeStatus withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ErrandPurgeStatus withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public OffsetDateTime getOlderThan() {
		return olderThan;
	}

	public void setOlderThan(final OffsetDateTime olderThan) {
		this.olderThan = olderThan;
	}

	public ErrandPurgeStatus withOlderThan(final OffsetDateTime olderThan) {
		this.olderThan = olderThan;
		return this;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(final boolean dryRun) {
		this.dryRun = dryRun;
	}

	public ErrandPurgeStatus withDryRun(final boolean dryRun) {
		this.dryRun = dryRun;
		return this;
	}

	public PurgeState getState() {
		return state;
	}

	public void setState(final PurgeState state) {
		this.state = state;
	}

	public ErrandPurgeStatus withState(final PurgeState state) {
		this.state = state;
		return this;
	}

	public OffsetDateTime getStarted() {
		return started;
	}

	public void setStarted(final OffsetDateTime started) {
		this.started = started;
	}

	public ErrandPurgeStatus withStarted(final OffsetDateTime started) {
		this.started = started;
		return this;
	}

	public OffsetDateTime getFinished() {
		return finished;
	}

	public void setFinished(final OffsetDateTime finished) {
		this.finished = finished;
	}

	public ErrandPurgeStatus withFinished(final OffsetDateTime finished) {
		this.finished = finished;
		return this;
	}

	public long getProcessed() {
		return processed;
	}

	public void setProcessed(final long processed) {
		this.processed = processed;
	}

	public ErrandPurgeStatus withProcessed(final long processed) {
		this.processed = processed;
		return this;
	}

	public long getDeleted() {
		return deleted;
	}

	public void setDeleted(final long deleted) {
		this.deleted = deleted;
	}

	public ErrandPurgeStatus withDeleted(final long deleted) {
		this.deleted = deleted;
		return this;
	}

	public long getFailed() {
		return failed;
	}

	public void setFailed(final long failed) {
		this.failed = failed;
	}

	public ErrandPurgeStatus withFailed(final long failed) {
		this.failed = failed;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public ErrandPurgeStatus withMessage(final String message) {
		this.message = message;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobId, namespace, municipalityId, olderThan, dryRun, state, started, finished, processed, deleted, failed, message);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ErrandPurgeStatus that = (ErrandPurgeStatus) o;
		return dryRun == that.dryRun
			&& processed == that.processed
			&& deleted == that.deleted
			&& failed == that.failed
			&& Objects.equals(jobId, that.jobId)
			&& Objects.equals(namespace, that.namespace)
			&& Objects.equals(municipalityId, that.municipalityId)
			&& Objects.equals(olderThan, that.olderThan)
			&& state == that.state
			&& Objects.equals(started, that.started)
			&& Objects.equals(finished, that.finished)
			&& Objects.equals(message, that.message);
	}

	@Override
	public String toString() {
		return "ErrandPurgeStatus{" +
			"jobId='" + jobId + '\'' +
			", namespace='" + namespace + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", olderThan=" + olderThan +
			", dryRun=" + dryRun +
			", state=" + state +
			", started=" + started +
			", finished=" + finished +
			", processed=" + processed +
			", deleted=" + deleted +
			", failed=" + failed +
			", message='" + message + '\'' +
			'}';
	}
}
