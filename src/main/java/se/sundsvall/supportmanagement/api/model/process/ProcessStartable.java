package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static java.util.Optional.ofNullable;

/**
 * Whether a process may be started for an errand, and which one.
 * <p>
 * The status is carried as a string rather than as an enum, because its own contract is that values are added over
 * time: a client is told to treat what it does not recognise as not startable, and a generated enum would throw on the
 * value instead of letting it. {@link ProcessStartability} remains the set this service may answer with - it is what
 * {@link #withStatus(ProcessStartability)} takes, so a value outside it cannot be published by mistake - but it is kept
 * off the wire so that adding one is not a new version of this API.
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
	private String status;

	@Schema(description = """
		The process keys that are eligible to start, taken from the process key attribute on the labels of the errand. \
		One element is the normal case. Two or more elements mean the errand carries labels pointing at different \
		processes and a person has to choose. Empty whenever status is not AVAILABLE.""", accessMode = READ_ONLY)
	private List<String> processKeys;

	public static ProcessStartable create() {
		return new ProcessStartable();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	/**
	 * Takes the enum rather than a string, which is what keeps the published values a closed set on this side of the wire
	 * while leaving them open on the other.
	 */
	public ProcessStartable withStatus(final ProcessStartability status) {
		this.status = ofNullable(status).map(Enum::name).orElse(null);
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
		return Objects.equals(status, other.status) && Objects.equals(processKeys, other.processKeys);
	}

	@Override
	public String toString() {
		return "ProcessStartable{" +
			"status=" + status +
			", processKeys=" + processKeys +
			'}';
	}
}
