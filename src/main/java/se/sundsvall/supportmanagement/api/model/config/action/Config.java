package se.sundsvall.supportmanagement.api.model.config.action;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;

@Schema(description = "Errand action config model")
public class Config {

	@Schema(description = "Unique id for action config", example = "a337d0de-5a6d-4952-9f39-74800b254c21", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Name of the type of action. Must match an existing action definition", example = "ADD_LABEL")
	@NotBlank
	private String name;

	@Schema(description = "If set to true, action will be active", defaultValue = "false", example = "true")
	private Boolean active;

	@Schema(description = "Conditions for when the action should be added to errand. Parameters must match action definition.")
	@NotEmpty
	private List<Parameter> conditions;

	@Schema(description = "Parameters for action. Must match action definition.")
	@NotEmpty
	private List<Parameter> parameters;

	@Schema(description = "Display value for this action. Will be mapped to each action on errands", example = "Classification change will occur")
	private String displayValue;

	public static Config create() {
		return new Config();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Config withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Config withName(final String name) {
		this.name = name;
		return this;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Config withActive(final Boolean active) {
		this.active = active;
		return this;
	}

	public List<Parameter> getConditions() {
		return conditions;
	}

	public void setConditions(List<Parameter> conditions) {
		this.conditions = conditions;
	}

	public Config withConditions(final List<Parameter> conditions) {
		this.conditions = conditions;
		return this;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}

	public Config withParameters(final List<Parameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	public String getDisplayValue() {
		return displayValue;
	}

	public void setDisplayValue(String displayValue) {
		this.displayValue = displayValue;
	}

	public Config withDisplayValue(final String displayValue) {
		this.displayValue = displayValue;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		Config config = (Config) o;
		return Objects.equals(id, config.id) && Objects.equals(name, config.name) && Objects.equals(active, config.active) && Objects.equals(conditions, config.conditions) && Objects.equals(parameters, config.parameters) && Objects.equals(displayValue,
			config.displayValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, active, conditions, parameters, displayValue);
	}

	@Override
	public String toString() {
		return "Config{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", active=" + active +
			", conditions=" + conditions +
			", parameters=" + parameters +
			", displayValue='" + displayValue + '\'' +
			'}';
	}
}
