package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A warning raised while building the handover preview")
public class Warning {

	@Schema(description = "Type of warning")
	private WarningType type;

	@Schema(description = "Key the warning relates to, when applicable", examples = "orgUnit")
	private String key;

	@Schema(description = "Human readable detail describing the warning", examples = "jsonSchema 'orgUnit-v2' not registered in target")
	private String detail;

	@Schema(description = "Value the warning relates to, when applicable", examples = "EXTERNAL_REPORTER")
	private String value;

	public static Warning create() {
		return new Warning();
	}

	public WarningType getType() {
		return type;
	}

	public void setType(final WarningType type) {
		this.type = type;
	}

	public Warning withType(final WarningType type) {
		this.type = type;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public Warning withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(final String detail) {
		this.detail = detail;
	}

	public Warning withDetail(final String detail) {
		this.detail = detail;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public Warning withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, key, detail, value);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Warning other)) {
			return false;
		}
		return type == other.type && Objects.equals(key, other.key) && Objects.equals(detail, other.detail) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "Warning [type=" + type + ", key=" + key + ", detail=" + detail + ", value=" + value + "]";
	}
}
