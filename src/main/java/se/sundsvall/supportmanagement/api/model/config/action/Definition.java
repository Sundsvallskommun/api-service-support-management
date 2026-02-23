package se.sundsvall.supportmanagement.api.model.config.action;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Definition of a condition or parameter for an action")
public class Definition {

	@Schema(description = "Key for the definition", examples = "status")
	private String key;

	@Schema(description = "Whether this definition is mandatory", defaultValue = "false", examples = "true")
	private boolean mandatory;

	@Schema(description = "Description of the definition", examples = "The status to match against")
	private String description;

	@Schema(description = "List of possible values for this definition")
	private List<PossibleValue> possibleValues;

	public static Definition create() {
		return new Definition();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Definition withKey(final String key) {
		this.key = key;
		return this;
	}

	public boolean getMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public Definition withMandatory(final boolean mandatory) {
		this.mandatory = mandatory;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Definition withDescription(final String description) {
		this.description = description;
		return this;
	}

	public List<PossibleValue> getPossibleValues() {
		return possibleValues;
	}

	public void setPossibleValues(List<PossibleValue> possibleValues) {
		this.possibleValues = possibleValues;
	}

	public Definition withPossibleValues(final List<PossibleValue> possibleValues) {
		this.possibleValues = possibleValues;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		Definition that = (Definition) o;
		return Objects.equals(mandatory, that.mandatory) && Objects.equals(key, that.key) && Objects.equals(description, that.description) && Objects.equals(possibleValues, that.possibleValues);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, mandatory, description, possibleValues);
	}

	@Override
	public String toString() {
		return "Definition{" +
			"key='" + key + '\'' +
			", mandatory=" + mandatory +
			", description='" + description + '\'' +
			", possibleValues=" + possibleValues +
			'}';
	}
}
