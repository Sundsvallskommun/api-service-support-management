package se.sundsvall.supportmanagement.api.model.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Optional.ofNullable;

/**
 * A process attached to an errand, and its state, as it is read - both under {@code /processes} and as the
 * {@code process} field of an errand. What a process reports about itself is {@link ErrandProcessReport}.
 */
@JsonInclude(NON_NULL)
@Schema(description = "A process attached to an errand, and its state")
public class ErrandProcess {

	@Schema(description = "Unique id of the process row", examples = "1f0e4c21-4ba9-4f39-8a3b-9a6a1a9d1b77")
	private String id;

	@Schema(description = "The service running the process", examples = "pw-alkt")
	private String processService;

	@Schema(description = "The process model this instance runs, as the process engine names it", examples = "alkt-ansokan")
	private String processKey;

	@Schema(description = "Id of the process instance in the process engine. Left out for a start that failed, since it never produced one",
		examples = "8f1c2b6e-1f4a-4d61-9a0e-2b7c1f0a5e33")
	private String processInstanceId;

	@Schema(description = """
		The state the process is in: RUNNING, WAITING, RETRYING, COMPLETED or FAILED. Carried as a string rather than as \
		an enum so that a value added later does not break a client that generated one from this schema.""", examples = "RUNNING")
	private String processStatus;

	@Schema(description = "Identifier of the activity the process is at, as the process model names it", examples = "investigation_phase")
	private String currentActivityId;

	@Schema(description = "Display name of the activity the process is at", examples = "Utredning")
	private String currentActivityName;

	@Schema(description = "When the process started", examples = "2026-09-14T08:55:11.004+02:00")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime started;

	@Schema(description = """
		When the process reached a state it does not leave. Set by this service from the reported state, and cleared \
		again if the instance comes back to life.""", examples = "2026-09-20T14:03:11.882+02:00")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime ended;

	@Schema(description = "Why the process failed, set when the state says it did")
	private ProcessError error;

	@Schema(description = """
		What the process waits for from a handler right now: the signals a handler can send to step it past the gate it \
		stands at, through POST .../processes/{processInstanceId}/signals. Empty when the process waits for no person, \
		which is the normal case for a gate the process passes by itself, and always empty for a process that has ended.""")
	private List<ProcessSignal> awaitingSignals;

	@Schema(description = "When the process was first registered on the errand", examples = "2026-09-14T08:55:11.121+02:00")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "When the state of the process was last updated", examples = "2026-09-14T09:02:45.004+02:00")
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

	public String getProcessStatus() {
		return processStatus;
	}

	public void setProcessStatus(final String processStatus) {
		this.processStatus = processStatus;
	}

	/**
	 * Sets the process status to the name of the given enum constant, or to null when none is given.
	 */
	public ErrandProcess withProcessStatus(final ProcessStatus processStatus) {
		this.processStatus = ofNullable(processStatus).map(Enum::name).orElse(null);
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

	public List<ProcessSignal> getAwaitingSignals() {
		return awaitingSignals;
	}

	public void setAwaitingSignals(final List<ProcessSignal> awaitingSignals) {
		this.awaitingSignals = awaitingSignals;
	}

	public ErrandProcess withAwaitingSignals(final List<ProcessSignal> awaitingSignals) {
		this.awaitingSignals = awaitingSignals;
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
		return Objects.hash(id, processService, processKey, processInstanceId, processStatus, currentActivityId, currentActivityName, started, ended, error, awaitingSignals, created, modified);
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
			&& Objects.equals(processStatus, other.processStatus)
			&& Objects.equals(currentActivityId, other.currentActivityId)
			&& Objects.equals(currentActivityName, other.currentActivityName)
			&& Objects.equals(started, other.started)
			&& Objects.equals(ended, other.ended)
			&& Objects.equals(error, other.error)
			&& Objects.equals(awaitingSignals, other.awaitingSignals)
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
			", started=" + started +
			", ended=" + ended +
			", error=" + error +
			", awaitingSignals=" + awaitingSignals +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
