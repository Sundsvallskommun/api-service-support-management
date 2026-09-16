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

	@Schema(
		description = "What the holder may do with the field, narrowing it below the level the errand itself is held at. Left unset the field simply follows the errand, which is what every grant did before this was added, and a level may only ever restrict further - it can never make a readable errand writable. Only a field holding a keyed collection may carry one, and limited read is not a level a field can be held at",
		examples = "R")
	private AccessLevel level;

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

	public AccessLevel getLevel() {
		return level;
	}

	public void setLevel(final AccessLevel level) {
		this.level = level;
	}

	public FieldAccess withLevel(final AccessLevel level) {
		this.level = level;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(field, keys, level);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final FieldAccess other)) { return false; }
		return field == other.field && Objects.equals(keys, other.keys) && level == other.level;
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("FieldAccess [field=").append(field).append(", keys=").append(keys).append(", level=").append(level).append("]");
		return builder.toString();
	}
}
