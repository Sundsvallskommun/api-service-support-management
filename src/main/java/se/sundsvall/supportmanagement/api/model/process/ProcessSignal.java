package se.sundsvall.supportmanagement.api.model.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * A choice a handler can make to step a process past a gate it waits at.
 * <p>
 * Both fields come out of the process model and are relayed as they came, without being interpreted by this service.
 */
@Schema(description = "A signal the process waits for from a handler: a gate it can be stepped past by hand")
public class ProcessSignal {

	@Schema(description = """
		The message name of the signal in the process model. It is what a handler sends to step the process on, and \
		what the process correlates on - exactly as given, case included.""", examples = "granskning-godkand")
	@NotBlank
	@Size(max = 128)
	private String name;

	@Schema(description = "Display text for the choice, taken from the process model", examples = "Godkänn granskning")
	@Size(max = 255)
	private String label;

	public static ProcessSignal create() {
		return new ProcessSignal();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ProcessSignal withName(final String name) {
		this.name = name;
		return this;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public ProcessSignal withLabel(final String label) {
		this.label = label;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, label);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ProcessSignal other = (ProcessSignal) obj;
		return Objects.equals(name, other.name) && Objects.equals(label, other.label);
	}

	@Override
	public String toString() {
		return "ProcessSignal{" +
			"name='" + name + '\'' +
			", label='" + label + '\'' +
			'}';
	}
}
