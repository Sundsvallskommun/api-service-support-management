package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * A handler stepping a process past a gate. It carries the name of the gate and nothing else; a reason for the step
 * belongs in a note on the errand.
 */
@Schema(description = "A request to step a process past a gate it waits at")
public class ProcessSignalRequest {

	@Schema(description = """
		The name of the signal to send, which has to be one of awaitingSignals on the process right now, exactly as \
		given there. The process decides what the signal means where it stands - sending one steps past nothing the \
		process does not allow.""", examples = "granskning-godkand")
	@NotBlank
	@Size(max = 128)
	private String signal;

	public static ProcessSignalRequest create() {
		return new ProcessSignalRequest();
	}

	public String getSignal() {
		return signal;
	}

	public void setSignal(final String signal) {
		this.signal = signal;
	}

	public ProcessSignalRequest withSignal(final String signal) {
		this.signal = signal;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(signal);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ProcessSignalRequest other = (ProcessSignalRequest) obj;
		return Objects.equals(signal, other.signal);
	}

	@Override
	public String toString() {
		return "ProcessSignalRequest{" +
			"signal='" + signal + '\'' +
			'}';
	}
}
