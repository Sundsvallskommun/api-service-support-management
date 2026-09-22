package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

@Schema(
	description = "Label attribute model. Free-form key/value data owned by the client. Keys are conventions agreed between clients (e.g. 'escalationEmail'), except processKey and processStartMode, which the service reads itself - see attributes on the label.")
public class LabelAttribute {

	@Schema(description = "Attribute key", examples = "escalationEmail")
	@NotBlank
	@Size(max = 255)
	private String key;

	// The value is kept in a TEXT column of 65 535 bytes, and a character takes up to four of them
	@Schema(description = "Attribute value", examples = "escalation@example.com")
	@NotBlank
	@Size(max = 16383)
	private String value;

	public static LabelAttribute create() {
		return new LabelAttribute();
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public LabelAttribute withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public LabelAttribute withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
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
		final var other = (LabelAttribute) obj;
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "LabelAttribute [key=" + key + ", value=" + value + "]";
	}
}
