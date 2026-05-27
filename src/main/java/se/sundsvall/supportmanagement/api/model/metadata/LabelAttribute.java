package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

@Schema(description = "Label attribute model. Free-form key/value data owned by the client; not interpreted by the service. Keys are conventions agreed between clients (e.g. 'escalationEmail').")
public class LabelAttribute {

	@Schema(description = "Attribute key", examples = "escalationEmail")
	@NotBlank
	private String key;

	@Schema(description = "Attribute value", examples = "escalation@example.com")
	@NotBlank
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
