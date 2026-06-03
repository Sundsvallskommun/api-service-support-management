package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * A warning raised while building a handover preview.
 *
 * <p>
 * The applicable fields depend on {@link #type}: {@code PARAMETER_SCHEMA_MISMATCH} populates {@code key} and
 * {@code detail}, while {@code ROLE_NOT_IN_TARGET} populates {@code value}. Prefer the type-specific factory methods
 * {@link #parameterSchemaMismatch(String, String)} and {@link #roleNotInTarget(String)} over the generic builder to
 * avoid populating fields that do not apply to the chosen type.
 * </p>
 */
@Schema(description = "A warning raised while building the handover preview. The applicable fields depend on 'type'")
public class Warning {

	@Schema(description = "Type of warning, acts as discriminator for which other fields are populated", requiredMode = REQUIRED)
	private WarningType type;

	@Schema(description = "Key the warning relates to. Populated for PARAMETER_SCHEMA_MISMATCH", examples = "orgUnit")
	private String key;

	@Schema(description = "Human readable detail describing the warning. Populated for PARAMETER_SCHEMA_MISMATCH", examples = "jsonSchema 'orgUnit-v2' not registered in target")
	private String detail;

	@Schema(description = "Value the warning relates to. Populated for ROLE_NOT_IN_TARGET", examples = "EXTERNAL_REPORTER")
	private String value;

	public static Warning create() {
		return new Warning();
	}

	/**
	 * Creates a warning for a parameter whose json schema is not registered in the target namespace.
	 *
	 * @param  key    the parameter key the warning relates to
	 * @param  detail a human readable detail describing the mismatch
	 * @return        a {@code PARAMETER_SCHEMA_MISMATCH} warning
	 */
	public static Warning parameterSchemaMismatch(final String key, final String detail) {
		return new Warning()
			.withType(WarningType.PARAMETER_SCHEMA_MISMATCH)
			.withKey(key)
			.withDetail(detail);
	}

	/**
	 * Creates a warning for a stakeholder role that does not exist in the target namespace.
	 *
	 * @param  value the role that is missing in the target namespace
	 * @return       a {@code ROLE_NOT_IN_TARGET} warning
	 */
	public static Warning roleNotInTarget(final String value) {
		return new Warning()
			.withType(WarningType.ROLE_NOT_IN_TARGET)
			.withValue(value);
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
