package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Field of an errand that access may be configured for")
public class AccessFieldDefinition {

	@Schema(description = "Value to configure the field with", examples = "PARAMETERS")
	private String field;

	@Schema(description = "Property the field names on the errand, which is how the access of an errand reports it", examples = "parameters")
	private String property;

	@Schema(description = "If the field holds a keyed collection, in which case keys and a level may be configured for it", examples = "true")
	private boolean keyed;

	public static AccessFieldDefinition create() {
		return new AccessFieldDefinition();
	}

	public String getField() {
		return field;
	}

	public void setField(final String field) {
		this.field = field;
	}

	public AccessFieldDefinition withField(final String field) {
		this.field = field;
		return this;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(final String property) {
		this.property = property;
	}

	public AccessFieldDefinition withProperty(final String property) {
		this.property = property;
		return this;
	}

	public boolean isKeyed() {
		return keyed;
	}

	public void setKeyed(final boolean keyed) {
		this.keyed = keyed;
	}

	public AccessFieldDefinition withKeyed(final boolean keyed) {
		this.keyed = keyed;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(field, keyed, property);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final AccessFieldDefinition other)) { return false; }
		return Objects.equals(field, other.field) && keyed == other.keyed && Objects.equals(property, other.property);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("AccessFieldDefinition [field=").append(field).append(", property=").append(property).append(", keyed=").append(keyed).append("]");
		return builder.toString();
	}
}
