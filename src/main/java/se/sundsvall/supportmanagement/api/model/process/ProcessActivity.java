package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.api.validation.ValidEnumValue;
import se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static java.util.Optional.ofNullable;

/**
 * One entry in the log of what the process did, and of what kept it from doing anything.
 * <p>
 * The type and the identifier of an activity are free strings that SM stores without interpreting them, so a process
 * engine can log whatever its model calls things without a change here.
 */
@Schema(description = "An entry in the activity log of an errand")
public class ProcessActivity {

	@Schema(description = "Unique id of the entry", examples = "e1ec7cf3-1c85-4f0a-9dbc-6e3f3d3d0a41", accessMode = READ_ONLY)
	private String id;

	@Schema(description = """
		The process instance the entry belongs to. Null for entries written when the errand had no process instance at \
		all, which is precisely the case for the entries explaining why no process started.""", examples = "8f1c2b6e-1f4a-4d61-9a0e-2b7c1f0a5e33", accessMode = READ_ONLY)
	private String processInstanceId;

	@Schema(description = "Kind of entry. A free string that this service stores without interpreting it", examples = "PHASE")
	@NotBlank
	@Size(min = 1, max = 64)
	private String activityType;

	@Schema(description = "Identifier of the activity in the process model", examples = "review_phase")
	@Size(max = 255)
	private String activityId;

	@Schema(description = "Display name of the activity", examples = "Granskning")
	@Size(max = 255)
	private String activityName;

	@Schema(description = """
		Severity of the entry: INFO, WARN or ERROR. Defaults to INFO. Carried as a string rather than as an enum so that \
		a value added later does not break a client that generated one from this schema; an unknown value is still \
		refused.""", examples = "INFO")
	@ValidEnumValue(ActivitySeverity.class)
	private String severity;

	@Schema(description = "Free text describing what happened. Must not carry personal data", examples = "Concurrent external tasks detected")
	@Size(max = 2048)
	private String message;

	@Schema(description = "Error code, set for entries reporting a failure", examples = "INCIDENT")
	@Size(max = 64)
	private String errorCode;

	@Schema(description = "When the entry happened, read from the clock of the process", examples = "2026-09-14T09:02:44.910+02:00")
	@NotNull
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime occurredAt;

	@Schema(description = "When the entry was stored, read from the clock of this service", examples = "2026-09-14T09:02:45.121+02:00", accessMode = READ_ONLY)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	public static ProcessActivity create() {
		return new ProcessActivity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ProcessActivity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public ProcessActivity withProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
		return this;
	}

	public String getActivityType() {
		return activityType;
	}

	public void setActivityType(final String activityType) {
		this.activityType = activityType;
	}

	public ProcessActivity withActivityType(final String activityType) {
		this.activityType = activityType;
		return this;
	}

	public String getActivityId() {
		return activityId;
	}

	public void setActivityId(final String activityId) {
		this.activityId = activityId;
	}

	public ProcessActivity withActivityId(final String activityId) {
		this.activityId = activityId;
		return this;
	}

	public String getActivityName() {
		return activityName;
	}

	public void setActivityName(final String activityName) {
		this.activityName = activityName;
	}

	public ProcessActivity withActivityName(final String activityName) {
		this.activityName = activityName;
		return this;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(final String severity) {
		this.severity = severity;
	}

	/**
	 * Takes the enum, which is what keeps this service from publishing a severity it does not have.
	 */
	public ProcessActivity withSeverity(final ActivitySeverity severity) {
		this.severity = ofNullable(severity).map(Enum::name).orElse(null);
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public ProcessActivity withMessage(final String message) {
		this.message = message;
		return this;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(final String errorCode) {
		this.errorCode = errorCode;
	}

	public ProcessActivity withErrorCode(final String errorCode) {
		this.errorCode = errorCode;
		return this;
	}

	public OffsetDateTime getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(final OffsetDateTime occurredAt) {
		this.occurredAt = occurredAt;
	}

	public ProcessActivity withOccurredAt(final OffsetDateTime occurredAt) {
		this.occurredAt = occurredAt;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ProcessActivity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, processInstanceId, activityType, activityId, activityName, severity, message, errorCode, occurredAt, created);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ProcessActivity other = (ProcessActivity) obj;
		return Objects.equals(id, other.id)
			&& Objects.equals(processInstanceId, other.processInstanceId)
			&& Objects.equals(activityType, other.activityType)
			&& Objects.equals(activityId, other.activityId)
			&& Objects.equals(activityName, other.activityName)
			&& Objects.equals(severity, other.severity)
			&& Objects.equals(message, other.message)
			&& Objects.equals(errorCode, other.errorCode)
			&& Objects.equals(occurredAt, other.occurredAt)
			&& Objects.equals(created, other.created);
	}

	@Override
	public String toString() {
		return "ProcessActivity{" +
			"id='" + id + '\'' +
			", processInstanceId='" + processInstanceId + '\'' +
			", activityType='" + activityType + '\'' +
			", activityId='" + activityId + '\'' +
			", activityName='" + activityName + '\'' +
			", severity=" + severity +
			", message='" + message + '\'' +
			", errorCode='" + errorCode + '\'' +
			", occurredAt=" + occurredAt +
			", created=" + created +
			'}';
	}
}
