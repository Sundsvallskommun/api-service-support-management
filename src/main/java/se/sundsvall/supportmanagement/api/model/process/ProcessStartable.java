package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static java.util.Optional.ofNullable;

/**
 * Whether a process may be started for an errand, and which one.
 * <p>
 * The status is published as a string, and values may be added over time: a client treats a value it does not
 * recognise as not startable. {@link ProcessStartability} is the set of values this service answers with, and
 * {@link #withStatus(ProcessStartability)} sets the status from it.
 */
@Schema(description = "Whether a process may be started for an errand, and which one")
public class ProcessStartable {

	@Schema(description = """
		AVAILABLE means a process may be started right now; every other value says why one cannot be. \
		LIVE_INSTANCE - a process is already running for this errand. \
		PROCESS_COMPLETED - a process has already run to its end. An errand has one process life; a new process means a \
		new errand. \
		START_PENDING - a start is already on its way to the process engine, and the process shows up among the \
		processes once the process engine has registered it, normally within seconds. Show that the start is on its way \
		rather than offering it again. \
		NO_PROCESS_KEY - no label on the errand carries a processKey attribute the errand can be started with, so there \
		is nothing to start. Setting the right label is the fix. \
		NO_PROCESS_ENGINE - this namespace does not run processes at all. \
		The answer is the same whether or not the labels start the process on their own: an errand whose process starts \
		by itself is AVAILABLE too, and starting it by hand is how a start that failed is tried again. \
		Treat any value you do not recognise as not startable - values may be added over time.""", examples = "AVAILABLE", accessMode = READ_ONLY)
	private String status;

	@Schema(description = """
		The process keys that are eligible to start, taken from the processKey attribute on the labels of the errand. \
		One element is the normal case: send it - or send nothing - to POST .../processes/start. Two or more elements \
		mean the errand carries labels pointing at different processes and a person has to choose: ask the user and send \
		the chosen key, otherwise the request is rejected with 400. An errand runs one process for the whole of its life, \
		so once it has had one - a start that failed included - only the key of that process is offered. Empty whenever \
		status is not AVAILABLE.""", accessMode = READ_ONLY)
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
	 * Sets the status to the name of the given {@link ProcessStartability}, or to null when it is null.
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
