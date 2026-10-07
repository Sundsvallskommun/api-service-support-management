package se.sundsvall.supportmanagement.api.model.subscriber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "Filter on event type/subtype, used to limit which eventlog events trigger a notification")
public class EventFilter {

	@NotBlank
	@Pattern(regexp = "^(CREATE|READ|UPDATE|DELETE|ACCESS|EXECUTE|CANCEL|DROP)$",
		message = "type must be one of CREATE, READ, UPDATE, DELETE, ACCESS, EXECUTE, CANCEL, DROP")
	@Schema(description = "Event type. Matches the eventlog EventType enum.",
		allowableValues = {
			"CREATE", "READ", "UPDATE", "DELETE", "ACCESS", "EXECUTE", "CANCEL", "DROP"
		},
		examples = "UPDATE")
	private String type;

	@Size(max = 64)
	@Schema(description = "Event subtype. If null, all subtypes of the given type match.", examples = "ATTACHMENT")
	private String subtype;

	@ValidUuid(nullable = true)
	@Schema(description = "Optional id of a metadata label. When set, only events that added this label to the errand match - " +
		"every label counts as added when the errand is created. If null, the labels of the errand do not matter.",
		examples = "f2b7c5d1-7e3a-4b8e-9f0a-1c2d3e4f5a6b")
	private String labelAdded;

	public static EventFilter create() {
		return new EventFilter();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public EventFilter withType(final String type) {
		this.type = type;
		return this;
	}

	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(final String subtype) {
		this.subtype = subtype;
	}

	public EventFilter withSubtype(final String subtype) {
		this.subtype = subtype;
		return this;
	}

	public String getLabelAdded() {
		return labelAdded;
	}

	public void setLabelAdded(final String labelAdded) {
		this.labelAdded = labelAdded;
	}

	public EventFilter withLabelAdded(final String labelAdded) {
		this.labelAdded = labelAdded;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, subtype, labelAdded);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final EventFilter other = (EventFilter) obj;
		return Objects.equals(type, other.type) && Objects.equals(subtype, other.subtype) && Objects.equals(labelAdded, other.labelAdded);
	}

	@Override
	public String toString() {
		return "EventFilter{type='" + type + "', subtype='" + subtype + "', labelAdded='" + labelAdded + "'}";
	}
}
