package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Parameter model")
public class Parameter {

	@Schema(description = "Parameter key")
	@NotBlank
	private String key;

	@Schema(description = "Parameter display name")
	private String displayName;

	@Schema(description = "Parameter group name")
	private String group;

	@Schema(description = "Parameter values. Each value can have a maximum length of 3000 characters")
	@Valid
	private List<@Size(max = 3000) String> values;

	@Schema(description = "Optimistic locking version of the parameter", accessMode = READ_ONLY)
	private Long version;

	public static Parameter create() {
		return new Parameter();
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(final Long version) {
		this.version = version;
	}

	public Parameter withVersion(final Long version) {
		this.version = version;
		return this;
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
		return Objects.hash(displayName, group, key, values, version);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Parameter other)) {
			return false;
		}
		return Objects.equals(displayName, other.displayName) && Objects.equals(group, other.group) && Objects.equals(key, other.key) && Objects.equals(values, other.values) && Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "Parameter{" +
			"key='" + key + '\'' +
			", displayName='" + displayName + '\'' +
			", group='" + group + '\'' +
			", values=" + values +
			", version=" + version +
			'}';
	}
}
