package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * The processes attached to an errand.
 * <p>
 * An envelope rather than a bare list, because the interesting case is the errand that has no process at all. An empty
 * list says that none is running, but not whether that is because the errand is waiting for someone to press a button,
 * because its process already reached its end and may never be started again, or because it carries no process label.
 * Only a field beside the list can carry that, and giving the list an envelope from the start is what lets the answer
 * be added without the response changing type.
 */
@Schema(description = "The processes attached to an errand, and whether a new one may be started right now")
public class ErrandProcesses {

	@Schema(description = """
		Whether a process may be started for this errand right now, and why not when it cannot be. Not answered yet: \
		the field is absent until starting a process is offered by this API. Read it before offering a start action to \
		the user, and treat its absence as unknown rather than as available.""", accessMode = READ_ONLY)
	private ProcessStartable startable;

	@Schema(description = """
		Every process this errand has had, most recent first. Normally exactly one element. An empty list is not an \
		error and does not mean the errand is broken.""", accessMode = READ_ONLY)
	private List<ErrandProcess> processes;

	public static ErrandProcesses create() {
		return new ErrandProcesses();
	}

	public ProcessStartable getStartable() {
		return startable;
	}

	public void setStartable(final ProcessStartable startable) {
		this.startable = startable;
	}

	public ErrandProcesses withStartable(final ProcessStartable startable) {
		this.startable = startable;
		return this;
	}

	public List<ErrandProcess> getProcesses() {
		return processes;
	}

	public void setProcesses(final List<ErrandProcess> processes) {
		this.processes = processes;
	}

	public ErrandProcesses withProcesses(final List<ErrandProcess> processes) {
		this.processes = processes;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(startable, processes);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ErrandProcesses other = (ErrandProcesses) obj;
		return Objects.equals(startable, other.startable) && Objects.equals(processes, other.processes);
	}

	@Override
	public String toString() {
		return "ErrandProcesses{" +
			"startable=" + startable +
			", processes=" + processes +
			'}';
	}
}
