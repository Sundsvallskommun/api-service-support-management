package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * The processes an errand has had, together with whether a new one may be started for it right now and why not when it
 * cannot be. The response of {@code GET .../errands/{errandId}/processes}.
 */
@Schema(description = "The processes attached to an errand, and whether a new one may be started right now")
public class ErrandProcessOverview {

	@Schema(description = """
		Whether a process may be started for this errand right now, and why not when it cannot be. Read this before \
		offering a start action to the user. The same rules are enforced by POST .../processes/start, which answers 400 \
		or 409 when they are not met - so a client that ignores this field can never start something it should not. It \
		can only show a button that fails.""", accessMode = READ_ONLY)
	private ProcessStartable startable;

	@Schema(description = """
		Every process this errand has had, most recent first. Normally exactly one element. An empty list is not an \
		error and does not mean the errand is broken: see startable for whether a process can be started, and why not \
		if it cannot.""", accessMode = READ_ONLY)
	private List<ErrandProcess> processes;

	public static ErrandProcessOverview create() {
		return new ErrandProcessOverview();
	}

	public ProcessStartable getStartable() {
		return startable;
	}

	public void setStartable(final ProcessStartable startable) {
		this.startable = startable;
	}

	public ErrandProcessOverview withStartable(final ProcessStartable startable) {
		this.startable = startable;
		return this;
	}

	public List<ErrandProcess> getProcesses() {
		return processes;
	}

	public void setProcesses(final List<ErrandProcess> processes) {
		this.processes = processes;
	}

	public ErrandProcessOverview withProcesses(final List<ErrandProcess> processes) {
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
		final ErrandProcessOverview other = (ErrandProcessOverview) obj;
		return Objects.equals(startable, other.startable) && Objects.equals(processes, other.processes);
	}

	@Override
	public String toString() {
		return "ErrandProcessOverview{" +
			"startable=" + startable +
			", processes=" + processes +
			'}';
	}
}
