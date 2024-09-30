package se.sundsvall.supportmanagement.api.model.errand;

import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Parameter model")
public class Parameter {

	@Schema(description = "Parameter key")
	@NotBlank
	private String key;

	@Schema(description = "Parameter display name")
	private String displayName;

	@Schema(description = "Parameter values")
	private List<String> values;

	public static Parameter create() {
		return new Parameter();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Parameter withKey(String key) {
		this.key = key;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Parameter withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public Parameter withValues(List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(displayName, key, values);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final Parameter other)) { return false; }
		return Objects.equals(displayName, other.displayName) && Objects.equals(key, other.key) && Objects.equals(values, other.values);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Parameter [key=").append(key).append(", displayName=").append(displayName).append(", values=").append(values).append("]");
		return builder.toString();
	}
}
