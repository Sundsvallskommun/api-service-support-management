package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

@Schema(description = "Parameter model")
public class Parameter {

	@Schema(description = "Parameter key")
	@NotBlank
	private String key;

	@Schema(description = "Parameter display name")
	private String displayName;

	@Schema(description = "Parameter group name")
	private String group;

	@Schema(description = "Parameter values. Each value can have a maximum length of 2000 characters")
	@Valid
	private List<@Size(max = 2000) String> values;

	public static Parameter create() {
		return new Parameter();
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public Parameter withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public Parameter withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(final String group) {
		this.group = group;
	}

	public Parameter withGroup(final String group) {
		this.group = group;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(final List<String> values) {
		this.values = values;
	}

	public Parameter withValues(final List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(displayName, group, key, values);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Parameter other)) {
			return false;
		}
		return Objects.equals(displayName, other.displayName) && Objects.equals(group, other.group) && Objects.equals(key, other.key) && Objects.equals(values, other.values);
	}

	@Override
	public String toString() {
		return "Parameter{" +
			"key='" + key + '\'' +
			", displayName='" + displayName + '\'' +
			", group='" + group + '\'' +
			", values=" + values +
			'}';
	}
}
