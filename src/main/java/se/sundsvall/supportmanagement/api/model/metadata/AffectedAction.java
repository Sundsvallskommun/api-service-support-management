package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "An action that references labels affected by a label move")
public class AffectedAction {

	@Schema(description = "Action ID", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7")
	private String id;

	@Schema(description = "Action name", example = "SEND_EMAIL")
	private String name;

	@Schema(description = "Human-readable display value for the action", example = "Send email to assignee")
	private String displayValue;

	public static AffectedAction create() {
		return new AffectedAction();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public AffectedAction withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public AffectedAction withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayValue() {
		return displayValue;
	}

	public void setDisplayValue(final String displayValue) {
		this.displayValue = displayValue;
	}

	public AffectedAction withDisplayValue(final String displayValue) {
		this.displayValue = displayValue;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, displayValue);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final AffectedAction other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(displayValue, other.displayValue);
	}

	@Override
	public String toString() {
		return "AffectedAction[id=" + id + ", name=" + name + ", displayValue=" + displayValue + "]";
	}
}
