package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.api.validation.ValidEnumValue;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static java.util.Optional.ofNullable;

/**
 * What a process reports about itself: the state it is in, what it did and what it waits for. The body of both write
 * paths under {@code /processes}.
 * <p>
 * A process is read as {@link ErrandProcess}. Three fields belong to the report only: the external task, the errand
 * version and the activities.
 */
@Schema(description = "What a process reports about itself: the state it is in, what it did and what it waits for")
public class ErrandProcessReport {

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
	@Pattern(regexp = "\\S+", message = "must be an id, without blanks")
	private String processInstanceId;

	@Schema(description = """
		The state the process is in: RUNNING, WAITING, RETRYING, COMPLETED or FAILED. Carried as a string rather than as \
		an enum so that a value added later does not break a client that generated one from this schema; an unknown value \
		is still refused.""", examples = "RUNNING")
	@NotBlank
	@ValidEnumValue(ProcessStatus.class)
	private String processStatus;

	@Schema(description = "Identifier of the activity the process is at, as the process model names it", examples = "investigation_phase")
	@Size(max = 255)
	private String currentActivityId;

	@Schema(description = "Display name of the activity the process is at", examples = "Utredning")
	@Size(max = 255)
	private String currentActivityName;

	@Schema(description = """
		The external task the report was made from. Together with the activity ids of the report it is what makes a \
		replayed report add no duplicate activities.""", examples = "a91c7f30-4d2b-11f0-9e21-0242ac120004")
	@Size(max = 64)
	private String externalTaskId;

	@Schema(description = """
		The version of the errand the work step read. Optional, and meant for a step that reads the errand without \
		writing back to it and therefore has no If-Match of its own.""", examples = "7")
	private Long errandVersion;

	@Schema(description = "When the process started. A later report that leaves it out does not clear it", examples = "2026-09-14T08:55:11.004+02:00")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime started;

	@Schema(description = "Why the process failed, set when the state says it did")
	@Valid
	private ProcessError error;

	@Schema(description = """
		What the process did, appended to the activity log of the errand in the same call as the state. Read back \
		through GET /errands/{errandId}/process-activities.""")
	@Valid
	@Size(max = 100, message = "may contain at most 100 activities")
	private List<@NotNull ProcessActivity> activities;

	@Schema(description = """
		What the process waits for from a handler right now: the signals a handler can send to step it past the gate it \
		stands at. Replaces what the previous report said, and a report that leaves it out or sends it empty says the \
		process waits for no person, which is the normal case for a gate the process passes by itself.""")
	@Valid
	@Size(max = 50, message = "may contain at most 50 signals")
	private List<@NotNull ProcessSignal> awaitingSignals;

	public static ErrandProcessReport create() {
		return new ErrandProcessReport();
	}

	public String getProcessService() {
		return processService;
	}

	public void setProcessService(final String processService) {
		this.processService = processService;
	}

	public ErrandProcessReport withProcessService(final String processService) {
		this.processService = processService;
		return this;
	}

	public String getProcessKey() {
		return processKey;
	}

	public void setProcessKey(final String processKey) {
		this.processKey = processKey;
	}

	public ErrandProcessReport withProcessKey(final String processKey) {
		this.processKey = processKey;
		return this;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public ErrandProcessReport withProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
		return this;
	}

	public String getProcessStatus() {
		return processStatus;
	}

	public void setProcessStatus(final String processStatus) {
		this.processStatus = processStatus;
	}

	/**
	 * Sets the process status from the enum, so that a caller in this service can only report a state that exists.
	 */
	public ErrandProcessReport withProcessStatus(final ProcessStatus processStatus) {
		this.processStatus = ofNullable(processStatus).map(Enum::name).orElse(null);
		return this;
	}

	public String getCurrentActivityId() {
		return currentActivityId;
	}

	public void setCurrentActivityId(final String currentActivityId) {
		this.currentActivityId = currentActivityId;
	}

	public ErrandProcessReport withCurrentActivityId(final String currentActivityId) {
		this.currentActivityId = currentActivityId;
		return this;
	}

	public String getCurrentActivityName() {
		return currentActivityName;
	}

	public void setCurrentActivityName(final String currentActivityName) {
		this.currentActivityName = currentActivityName;
	}

	public ErrandProcessReport withCurrentActivityName(final String currentActivityName) {
		this.currentActivityName = currentActivityName;
		return this;
	}

	public String getExternalTaskId() {
		return externalTaskId;
	}

	public void setExternalTaskId(final String externalTaskId) {
		this.externalTaskId = externalTaskId;
	}

	public ErrandProcessReport withExternalTaskId(final String externalTaskId) {
		this.externalTaskId = externalTaskId;
		return this;
	}

	public Long getErrandVersion() {
		return errandVersion;
	}

	public void setErrandVersion(final Long errandVersion) {
		this.errandVersion = errandVersion;
	}

	public ErrandProcessReport withErrandVersion(final Long errandVersion) {
		this.errandVersion = errandVersion;
		return this;
	}

	public OffsetDateTime getStarted() {
		return started;
	}

	public void setStarted(final OffsetDateTime started) {
		this.started = started;
	}

	public ErrandProcessReport withStarted(final OffsetDateTime started) {
		this.started = started;
		return this;
	}

	public ProcessError getError() {
		return error;
	}

	public void setError(final ProcessError error) {
		this.error = error;
	}

	public ErrandProcessReport withError(final ProcessError error) {
		this.error = error;
		return this;
	}

	public List<ProcessActivity> getActivities() {
		return activities;
	}

	public void setActivities(final List<ProcessActivity> activities) {
		this.activities = activities;
	}

	public ErrandProcessReport withActivities(final List<ProcessActivity> activities) {
		this.activities = activities;
		return this;
	}

	public List<ProcessSignal> getAwaitingSignals() {
		return awaitingSignals;
	}

	public void setAwaitingSignals(final List<ProcessSignal> awaitingSignals) {
		this.awaitingSignals = awaitingSignals;
	}

	public ErrandProcessReport withAwaitingSignals(final List<ProcessSignal> awaitingSignals) {
		this.awaitingSignals = awaitingSignals;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(processService, processKey, processInstanceId, processStatus, currentActivityId, currentActivityName, externalTaskId, errandVersion, started, error, activities, awaitingSignals);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ErrandProcessReport other = (ErrandProcessReport) obj;
		return Objects.equals(processService, other.processService)
			&& Objects.equals(processKey, other.processKey)
			&& Objects.equals(processInstanceId, other.processInstanceId)
			&& Objects.equals(processStatus, other.processStatus)
			&& Objects.equals(currentActivityId, other.currentActivityId)
			&& Objects.equals(currentActivityName, other.currentActivityName)
			&& Objects.equals(externalTaskId, other.externalTaskId)
			&& Objects.equals(errandVersion, other.errandVersion)
			&& Objects.equals(started, other.started)
			&& Objects.equals(error, other.error)
			&& Objects.equals(activities, other.activities)
			&& Objects.equals(awaitingSignals, other.awaitingSignals);
	}

	@Override
	public String toString() {
		return "ErrandProcessReport{" +
			"processService='" + processService + '\'' +
			", processKey='" + processKey + '\'' +
			", processInstanceId='" + processInstanceId + '\'' +
			", processStatus=" + processStatus +
			", currentActivityId='" + currentActivityId + '\'' +
			", currentActivityName='" + currentActivityName + '\'' +
			", externalTaskId='" + externalTaskId + '\'' +
			", errandVersion=" + errandVersion +
			", started=" + started +
			", error=" + error +
			", activities=" + activities +
			", awaitingSignals=" + awaitingSignals +
			'}';
	}
}
