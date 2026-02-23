package se.sundsvall.supportmanagement.api.model.config.action;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Action definition model describing an available action and its conditions/parameters")
public class ActionDefinition {

	@Schema(description = "Name of the action", examples = "ADD_LABEL")
	private String name;

	@Schema(description = "Description of the action", examples = "Adds a label to the errand when conditions are met")
	private String description;

	@Schema(description = "Definitions of conditions for this action")
	private List<Definition> conditionDefinitions;

	@Schema(description = "Definitions of parameters for this action")
	private List<Definition> parameterDefinitions;

	public static ActionDefinition create() {
		return new ActionDefinition();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ActionDefinition withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ActionDefinition withDescription(final String description) {
		this.description = description;
		return this;
	}

	public List<Definition> getConditionDefinitions() {
		return conditionDefinitions;
	}

	public void setConditionDefinitions(List<Definition> conditionDefinitions) {
		this.conditionDefinitions = conditionDefinitions;
	}

	public ActionDefinition withConditionDefinitions(final List<Definition> conditionDefinitions) {
		this.conditionDefinitions = conditionDefinitions;
		return this;
	}

	public List<Definition> getParameterDefinitions() {
		return parameterDefinitions;
	}

	public void setParameterDefinitions(List<Definition> parameterDefinitions) {
		this.parameterDefinitions = parameterDefinitions;
	}

	public ActionDefinition withParameterDefinitions(final List<Definition> parameterDefinitions) {
		this.parameterDefinitions = parameterDefinitions;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		ActionDefinition that = (ActionDefinition) o;
		return Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(conditionDefinitions, that.conditionDefinitions) && Objects.equals(parameterDefinitions, that.parameterDefinitions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, conditionDefinitions, parameterDefinitions);
	}

	@Override
	public String toString() {
		return "ActionDefinition{" +
			"name='" + name + '\'' +
			", description='" + description + '\'' +
			", conditionDefinitions=" + conditionDefinitions +
			", parameterDefinitions=" + parameterDefinitions +
			'}';
	}
}
