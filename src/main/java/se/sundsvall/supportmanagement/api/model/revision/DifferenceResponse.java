package se.sundsvall.supportmanagement.api.model.revision;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DifferenceResponse model", accessMode = READ_ONLY)
public class DifferenceResponse {

	@ArraySchema(schema = @Schema(implementation = Operation.class))
	private List<Operation> operations;

	public static DifferenceResponse create() {
		return new DifferenceResponse();
	}

	public List<Operation> getOperations() {
		return operations;
	}

	public void setOperations(List<Operation> operations) {
		this.operations = operations;
	}

	public DifferenceResponse withOperations(List<Operation> operations) {
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
		builder.append("DifferenceResponse [operations=").append(operations).append("]");
		return builder.toString();
	}
}
