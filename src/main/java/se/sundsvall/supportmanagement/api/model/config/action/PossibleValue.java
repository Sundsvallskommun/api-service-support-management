package se.sundsvall.supportmanagement.api.model.config.action;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Possible value for an action definition parameter or condition")
public class PossibleValue {

	@Schema(description = "The value", examples = "CATEGORY_1")
	private String value;

	@Schema(description = "Display name for the value", examples = "Category 1")
	private String displayName;

	public static PossibleValue create() {
		return new PossibleValue();
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public PossibleValue withValue(final String value) {
		this.value = value;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public PossibleValue withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		PossibleValue that = (PossibleValue) o;
		return Objects.equals(value, that.value) && Objects.equals(displayName, that.displayName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, displayName);
	}

	@Override
	public String toString() {
		return "PossibleValue{" +
			"value='" + value + '\'' +
			", displayName='" + displayName + '\'' +
			'}';
	}
}
