package se.sundsvall.supportmanagement.api.model.revision;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.model.revision.deserializer.RawDataDeserializer;

@Schema(description = "Operation model", accessMode = READ_ONLY)
public class Operation {

	@Schema(description = "Type of operation", example = "replace")
	private String op;

	@Schema(description = "Path to attribute", example = "/name/firstName")
	private String path;

	@Schema(description = "Value of attribute", example = "Jane")
	@JsonDeserialize(using = RawDataDeserializer.class)
	private String value;

	@Schema(description = "Previous value of attribute", example = "John")
	private String fromValue;

	public static Operation create() {
		return new Operation();
	}

	public String getOp() {
		return op;
	}

	public void setOp(final String op) {
		this.op = op;
	}

	public Operation withOp(final String op) {
		this.op = op;
		return this;
	}

	public String getPath() {
		return path;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public Operation withPath(final String path) {
		this.path = path;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public Operation withValue(final String value) {
		this.value = value;
		return this;
	}

	public String getFromValue() {
		return fromValue;
	}

	public void setFromValue(final String fromValue) {
		this.fromValue = fromValue;
	}

	public Operation withFromValue(final String fromValue) {
		this.fromValue = fromValue;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(op, path, value, fromValue);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Operation other = (Operation) obj;
		return Objects.equals(op, other.op) && Objects.equals(path, other.path) && Objects.equals(value, other.value) && Objects.equals(fromValue, other.fromValue);
	}

	@Override
	public String toString() {
		return "Operation{" +
			"op='" + op + '\'' +
			", path='" + path + '\'' +
			", value='" + value + '\'' +
			", fromValue='" + fromValue + '\'' +
			'}';
	}
}
