package se.sundsvall.supportmanagement.api.model.errand;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "External tag model")
public class ExternalTag {

	@Schema(description = "Key for external tag", example = "caseid")
	@NotBlank
	private String key;

	@Schema(description = "Value for external tag", example = "8849-2848")
	@NotBlank
	private String value;

	public static ExternalTag create() {
		return new ExternalTag();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public ExternalTag withKey(String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ExternalTag withValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
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
		final var other = (ExternalTag) obj;
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("ExternalTag [key=").append(key).append(", value=").append(value).append("]");
		return builder.toString();
	}
}
