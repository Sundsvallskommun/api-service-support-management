package se.sundsvall.supportmanagement.api.model.revision;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "DifferenceResponse model")
public class DifferenceResponse {

	@ArraySchema(schema = @Schema(implementation = Operation.class, accessMode = READ_ONLY))
	private List<Operation> operations;

	public static DifferenceResponse create() {
		return new DifferenceResponse();
	}

	public List<Operation> getEvents() {
		return operations;
	}

	public void setEvents(List<Operation> operations) {
		this.operations = operations;
	}

	public DifferenceResponse withEvents(List<Operation> operations) {
		this.operations = operations;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(operations);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DifferenceResponse other = (DifferenceResponse) obj;
		return Objects.equals(operations, other.operations);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DifferenceResponse [events=").append(operations).append("]");
		return builder.toString();
	}
}
