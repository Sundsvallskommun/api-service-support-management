package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * Whether a process may be started for an errand, and which one.
 */
@Schema(description = "Whether a process may be started for an errand, and which one")
public class ProcessStartable {

	@Schema(description = """
		AVAILABLE means a process may be started right now; every other value says why one cannot be. \
		LIVE_INSTANCE - a process is already running for this errand. \
		PROCESS_COMPLETED - a process has already run to its end. An errand has one process life; a new process means a \
		new errand. \
		NO_PROCESS_KEY - no label on the errand carries a process key, so there is nothing to start. Setting the right \
		label is the fix. \
		NO_PROCESS_ENGINE - this namespace does not run processes at all. \
		Treat any value you do not recognise as not startable - values may be added over time.""", examples = "AVAILABLE", accessMode = READ_ONLY)
	private ProcessStartability status;

	@Schema(description = """
		The process keys that are eligible to start, taken from the process key attribute on the labels of the errand. \
		One element is the normal case. Two or more elements mean the errand carries labels pointing at different \
		processes and a person has to choose. Empty whenever status is not AVAILABLE.""", accessMode = READ_ONLY)
	private List<String> processKeys;

	public static ProcessStartable create() {
		return new ProcessStartable();
	}

	public ProcessStartability getStatus() {
		return status;
	}

	public void setStatus(final ProcessStartability status) {
		this.status = status;
	}

	public ProcessStartable withStatus(final ProcessStartability status) {
		this.status = status;
		return this;
	}

	public List<String> getProcessKeys() {
		return processKeys;
	}

	public void setProcessKeys(final List<String> processKeys) {
		this.processKeys = processKeys;
	}

	public ProcessStartable withProcessKeys(final List<String> processKeys) {
		this.processKeys = processKeys;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(status, processKeys);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ProcessStartable other = (ProcessStartable) obj;
		return status == other.status && Objects.equals(processKeys, other.processKeys);
	}

	@Override
	public String toString() {
		return "ProcessStartable{" +
			"status=" + status +
			", processKeys=" + processKeys +
			'}';
	}
}
