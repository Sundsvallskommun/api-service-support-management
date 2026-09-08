package se.sundsvall.supportmanagement.api.model.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;

/**
 * A process attached to an errand, and its state.
 * <p>
 * One model, not two: the same class is the subresource under {@code /processes} and the {@code process} projection on
 * the errand. The three fields belonging to a report and to nothing else - the external task, the errand version and
 * the activities - are write only, and since nulls are left out of the serialised form they never appear in a read.
 * Two near identical models would drift apart the first time a field was added to only one of them.
 */
@JsonInclude(NON_NULL)
@Schema(description = "A process attached to an errand, and its state")
public class ErrandProcess {

	@Schema(description = "Unique id of the process row", examples = "1f0e4c21-4ba9-4f39-8a3b-9a6a1a9d1b77", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "The service running the process, matching the process consumer configured for the namespace", examples = "pw-alkt")
	@NotBlank
	@Size(min = 1, max = 64)
	private String processService;

	@Schema(description = "The process model this instance runs, as the process engine names it", examples = "alkt-ansokan")
	@NotBlank
	@Size(min = 1, max = 128)
	private String processKey;

	@Schema(description = """
		Id of the process instance in the process engine. Required when registering a start that succeeded, and left out \
		when registering one that failed, since a start that never happened has no instance. Taken from the path when \
		reporting on an instance; sending a different one there is rejected.""", examples = "8f1c2b6e-1f4a-4d61-9a0e-2b7c1f0a5e33")
	@Size(max = 64)
	private String processInstanceId;

	@Schema(implementation = ProcessStatus.class, description = "The state the process is in")
	@NotNull
	private ProcessStatus processStatus;

	@Schema(description = "Identifier of the activity the process is at, as the process model names it", examples = "investigation_phase")
	@Size(max = 255)
	private String currentActivityId;

	@Schema(description = "Display name of the activity the process is at", examples = "Utredning")
	@Size(max = 255)
	private String currentActivityName;

	@Schema(description = """
		The external task the report was made from. Together with the activity ids of the report it is what makes a \
		replayed report add no duplicate activities.""", examples = "a91c7f30-4d2b-11f0-9e21-0242ac120004", accessMode = WRITE_ONLY)
	@Size(max = 64)
	private String externalTaskId;

	@Schema(description = """
		The version of the errand the work step read. Optional, and meant for a step that reads the errand without \
		writing back to it and therefore has no If-Match of its own.""", examples = "7", accessMode = WRITE_ONLY)
	private Long errandVersion;

	@Schema(description = "When the process started", examples = "2026-09-14T08:55:11.004+02:00")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime started;

	@Schema(description = """
		When the process reached a state it does not leave. Set by this service from the reported state rather than \
		taken from the report, and cleared again if the instance comes back to life.""", examples = "2026-09-20T14:03:11.882+02:00")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime ended;

	@Schema(description = "Why the process failed, set when the state says it did")
	private ProcessError error;

	@Schema(description = """
		What the process did, appended to the activity log of the errand in the same call as the state. Read back \
		through GET /errands/{errandId}/process-activities rather than here.""", accessMode = WRITE_ONLY)
	@Valid
	@Size(max = 100, message = "may contain at most 100 activities")
	private List<ProcessActivity> activities;

	@Schema(description = "When the process was first registered on the errand", examples = "2026-09-14T08:55:11.121+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "When the state of the process was last updated", examples = "2026-09-14T09:02:45.004+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime modified;

	public static ErrandProcess create() {
		return new ErrandProcess();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandProcess withId(final String id) {
		this.id = id;
		return this;
	}

	public String getProcessService() {
		return processService;
	}

	public void setProcessService(final String processService) {
		this.processService = processService;
	}

	public ErrandProcess withProcessService(final String processService) {
		this.processService = processService;
		return this;
	}

	public String getProcessKey() {
		return processKey;
	}

	public void setProcessKey(final String processKey) {
		this.processKey = processKey;
	}

	public ErrandProcess withProcessKey(final String processKey) {
		this.processKey = processKey;
		return this;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public ErrandProcess withProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
		return this;
	}

	public ProcessStatus getProcessStatus() {
		return processStatus;
	}

	public void setProcessStatus(final ProcessStatus processStatus) {
		this.processStatus = processStatus;
	}

	public ErrandProcess withProcessStatus(final ProcessStatus processStatus) {
		this.processStatus = processStatus;
		return this;
	}

	public String getCurrentActivityId() {
		return currentActivityId;
	}

	public void setCurrentActivityId(final String currentActivityId) {
		this.currentActivityId = currentActivityId;
	}

	public ErrandProcess withCurrentActivityId(final String currentActivityId) {
		this.currentActivityId = currentActivityId;
		return this;
	}

	public String getCurrentActivityName() {
		return currentActivityName;
	}

	public void setCurrentActivityName(final String currentActivityName) {
		this.currentActivityName = currentActivityName;
	}

	public ErrandProcess withCurrentActivityName(final String currentActivityName) {
		this.currentActivityName = currentActivityName;
		return this;
	}

	public String getExternalTaskId() {
		return externalTaskId;
	}

	public void setExternalTaskId(final String externalTaskId) {
		this.externalTaskId = externalTaskId;
	}

	public ErrandProcess withExternalTaskId(final String externalTaskId) {
		this.externalTaskId = externalTaskId;
		return this;
	}

	public Long getErrandVersion() {
		return errandVersion;
	}

	public void setErrandVersion(final Long errandVersion) {
		this.errandVersion = errandVersion;
	}

	public ErrandProcess withErrandVersion(final Long errandVersion) {
		this.errandVersion = errandVersion;
		return this;
	}

	public OffsetDateTime getStarted() {
		return started;
	}

	public void setStarted(final OffsetDateTime started) {
		this.started = started;
	}

	public ErrandProcess withStarted(final OffsetDateTime started) {
		this.started = started;
		return this;
	}

	public OffsetDateTime getEnded() {
		return ended;
	}

	public void setEnded(final OffsetDateTime ended) {
		this.ended = ended;
	}

	public ErrandProcess withEnded(final OffsetDateTime ended) {
		this.ended = ended;
		return this;
	}

	public ProcessError getError() {
		return error;
	}

	public void setError(final ProcessError error) {
		this.error = error;
	}

	public ErrandProcess withError(final ProcessError error) {
		this.error = error;
		return this;
	}

	public List<ProcessActivity> getActivities() {
		return activities;
	}

	public void setActivities(final List<ProcessActivity> activities) {
		this.activities = activities;
	}

	public ErrandProcess withActivities(final List<ProcessActivity> activities) {
		this.activities = activities;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ErrandProcess withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public ErrandProcess withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, processService, processKey, processInstanceId, processStatus, currentActivityId, currentActivityName, externalTaskId, errandVersion, started, ended, error, activities, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ErrandProcess other = (ErrandProcess) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(processService, other.processService)
			&& Objects.equals(processKey, other.processKey)
			&& Objects.equals(processInstanceId, other.processInstanceId)
			&& processStatus == other.processStatus
			&& Objects.equals(currentActivityId, other.currentActivityId)
			&& Objects.equals(currentActivityName, other.currentActivityName)
			&& Objects.equals(externalTaskId, other.externalTaskId)
			&& Objects.equals(errandVersion, other.errandVersion)
			&& Objects.equals(started, other.started)
			&& Objects.equals(ended, other.ended)
			&& Objects.equals(error, other.error)
			&& Objects.equals(activities, other.activities)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "ErrandProcess{" +
			"id='" + id + '\'' +
			", processService='" + processService + '\'' +
			", processKey='" + processKey + '\'' +
			", processInstanceId='" + processInstanceId + '\'' +
			", processStatus=" + processStatus +
			", currentActivityId='" + currentActivityId + '\'' +
			", currentActivityName='" + currentActivityName + '\'' +
			", externalTaskId='" + externalTaskId + '\'' +
			", errandVersion=" + errandVersion +
			", started=" + started +
			", ended=" + ended +
			", error=" + error +
			", activities=" + activities +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
