package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;

@Schema(description = "Field of an errand exposed to a role")
public class FieldAccess {

	@NotNull
	@Schema(description = "Field to expose", examples = "PARAMETERS")
	private ErrandField field;

	@Schema(description = "Keys to expose when the field is a keyed collection. The whole collection is exposed when left empty", examples = "[\"contactChannel\"]")
	private List<String> keys;

	public static FieldAccess create() {
		return new FieldAccess();
	}

	public ErrandField getField() {
		return field;
	}

	public void setField(final ErrandField field) {
		this.field = field;
	}

	public FieldAccess withField(final ErrandField field) {
		this.field = field;
		return this;
	}

	public List<String> getKeys() {
		return keys;
	}

	public void setKeys(final List<String> keys) {
		this.keys = keys;
	}

	public FieldAccess withKeys(final List<String> keys) {
		this.keys = keys;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(field, keys);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final FieldAccess other)) { return false; }
		return field == other.field && Objects.equals(keys, other.keys);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("FieldAccess [field=").append(field).append(", keys=").append(keys).append("]");
		return builder.toString();
	}
}
