package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

@Schema(description = "External tag model")
public class ExternalTag {

	@Schema(description = "Key for external tag", example = "caseId")
	@NotBlank(groups = {
		OnCreate.class, OnUpdate.class
	})
	private String key;

	@Schema(description = "Value for external tag", example = "8849-2848")
	@NotBlank(groups = {
		OnCreate.class, OnUpdate.class
	})
	private String value;

	public static ExternalTag create() {
		return new ExternalTag();
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public ExternalTag withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public ExternalTag withValue(final String value) {
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
		final var other = (ExternalTag) obj;
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "ExternalTag [key=" + key + ", value=" + value + "]";
	}
}
