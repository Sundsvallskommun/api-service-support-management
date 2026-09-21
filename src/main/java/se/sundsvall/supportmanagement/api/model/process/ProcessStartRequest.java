package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * A handler's request to start the handling of an errand. The body may be left out altogether when the labels of the
 * errand point at one process only.
 */
@Schema(description = "A request to start a process for an errand")
public class ProcessStartRequest {

	@Schema(description = """
		Which process to start. May be omitted when startable.processKeys holds exactly one key, and is required when it \
		holds several. The value must be one of those keys, exactly as given there: a request cannot name a process that \
		the labels of the errand do not point at.""", examples = "supervision")
	@Size(max = 128)
	private String processKey;

	public static ProcessStartRequest create() {
		return new ProcessStartRequest();
	}

	public String getProcessKey() {
		return processKey;
	}

	public void setProcessKey(final String processKey) {
		this.processKey = processKey;
	}

	public ProcessStartRequest withProcessKey(final String processKey) {
		this.processKey = processKey;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(processKey);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ProcessStartRequest other = (ProcessStartRequest) obj;
		return Objects.equals(processKey, other.processKey);
	}

	@Override
	public String toString() {
		return "ProcessStartRequest{" +
			"processKey='" + processKey + '\'' +
			'}';
	}
}
