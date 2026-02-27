package se.sundsvall.supportmanagement.api.model.errand;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

@Schema(description = "Errand action model")
public class ErrandAction {

	@Schema(description = "Unique id for the action", examples = "f0882f1d-06bc-47fd-b017-1d8307f5ce95", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Name of the action", examples = "ADD_LABEL", accessMode = READ_ONLY)
	private String actionName;

	@Schema(description = "Timestamp after which the action should be executed", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime executeAfter;

	@Schema(description = "Id of the action config that created this action", examples = "f0882f1d-06bc-47fd-b017-1d8307f5ce95", accessMode = READ_ONLY)
	private String actionConfigId;

	@Schema(description = "Display value for the action", examples = "Label will be added", accessMode = READ_ONLY)
	private String displayValue;

	public static ErrandAction create() {
		return new ErrandAction();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandAction withId(final String id) {
		this.id = id;
		return this;
	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(final String actionName) {
		this.actionName = actionName;
	}

	public ErrandAction withActionName(final String actionName) {
		this.actionName = actionName;
		return this;
	}

	public OffsetDateTime getExecuteAfter() {
		return executeAfter;
	}

	public void setExecuteAfter(final OffsetDateTime executeAfter) {
		this.executeAfter = executeAfter;
	}

	public ErrandAction withExecuteAfter(final OffsetDateTime executeAfter) {
		this.executeAfter = executeAfter;
		return this;
	}

	public String getActionConfigId() {
		return actionConfigId;
	}

	public void setActionConfigId(final String actionConfigId) {
		this.actionConfigId = actionConfigId;
	}

	public ErrandAction withActionConfigId(final String actionConfigId) {
		this.actionConfigId = actionConfigId;
		return this;
	}

	public String getDisplayValue() {
		return displayValue;
	}

	public void setDisplayValue(final String displayValue) {
		this.displayValue = displayValue;
	}

	public ErrandAction withDisplayValue(final String displayValue) {
		this.displayValue = displayValue;
		return this;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ErrandAction other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(actionName, other.actionName) && Objects.equals(executeAfter, other.executeAfter) && Objects.equals(actionConfigId, other.actionConfigId) && Objects.equals(displayValue, other.displayValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, actionName, executeAfter, actionConfigId, displayValue);
	}

	@Override
	public String toString() {
		return "ErrandAction{" +
			"id='" + id + '\'' +
			", actionName='" + actionName + '\'' +
			", executeAfter=" + executeAfter +
			", actionConfigId='" + actionConfigId + '\'' +
			", displayValue='" + displayValue + '\'' +
			'}';
	}
}
