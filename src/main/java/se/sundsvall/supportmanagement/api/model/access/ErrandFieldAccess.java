package se.sundsvall.supportmanagement.api.model.access;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "What the requesting user may do with one field of an errand")
public class ErrandFieldAccess {

	@Schema(
		description = "Property of the errand, named as it is written in the errand payload. A field the user does not reach at all is not listed",
		examples = "parameters")
	private String field;

	@Schema(
		description = "If the field is reached without any key restriction, in which case every key of it follows the level of the errand and 'keys' is empty. False means the namespace restricts the field to the keys listed. Only set for the keyed fields PARAMETERS, JSON_PARAMETERS and EXTERNAL_TAGS",
		examples = "false")
	private Boolean allKeys;

	@Schema(
		description = "Every key of the collection the user reaches, and what they may do with each. A key that is not listed is not reachable at all, whether it is stored on the errand yet or not - so a key listed as 'RW' may be created as well as changed. Empty when 'allKeys' is true. Only set for keyed fields")
	private List<ErrandFieldKeyAccess> keys;

	public static ErrandFieldAccess create() {
		return new ErrandFieldAccess();
	}

	public String getField() {
		return field;
	}

	public void setField(final String field) {
		this.field = field;
	}

	public ErrandFieldAccess withField(final String field) {
		this.field = field;
		return this;
	}

	public Boolean getAllKeys() {
		return allKeys;
	}

	public void setAllKeys(final Boolean allKeys) {
		this.allKeys = allKeys;
	}

	public ErrandFieldAccess withAllKeys(final Boolean allKeys) {
		this.allKeys = allKeys;
		return this;
	}

	public List<ErrandFieldKeyAccess> getKeys() {
		return keys;
	}

	public void setKeys(final List<ErrandFieldKeyAccess> keys) {
		this.keys = keys;
	}

	public ErrandFieldAccess withKeys(final List<ErrandFieldKeyAccess> keys) {
		this.keys = keys;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(allKeys, field, keys);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final ErrandFieldAccess other)) { return false; }
		return Objects.equals(allKeys, other.allKeys) && Objects.equals(field, other.field) && Objects.equals(keys, other.keys);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("ErrandFieldAccess [field=").append(field).append(", allKeys=").append(allKeys).append(", keys=").append(keys).append("]");
		return builder.toString();
	}
}
