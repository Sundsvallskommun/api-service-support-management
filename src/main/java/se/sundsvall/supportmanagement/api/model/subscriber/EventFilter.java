package se.sundsvall.supportmanagement.api.model.subscriber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.validation.ValidEventFilter;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;

@Schema(description = "Filter on event type/subtype, used to limit which eventlog events trigger a notification")
@ValidEventFilter(groups = {
	Default.class, OnCreate.class, OnUpdate.class
})
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
	@Schema(description = "Event subtype. If null, all subtypes of the given type match. MESSAGE matches every message, while MESSAGE_INTERNAL " +
		"and MESSAGE_EXTERNAL match only messages in internal conversations and messages from outside respectively. Likewise ERRAND matches every change " +
		"to an errand, while STATUS and PHASE match only changes of its status and of its phase respectively. LABEL matches a label being added to an " +
		"errand, logged once for every label added - as CREATE/LABEL for the labels an errand is created with and as UPDATE/LABEL for those added later.",
		examples = "ATTACHMENT")
	private String subtype;

	@ValidUuid(nullable = true)
	@Schema(description = "Optional id of a metadata label, only allowed together with subtype LABEL. When set, only the event of this label " +
		"being added to the errand matches. If null, which label was added does not matter.",
		examples = "f2b7c5d1-7e3a-4b8e-9f0a-1c2d3e4f5a6b")
	private String labelId;

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

	public String getLabelId() {
		return labelId;
	}

	public void setLabelId(final String labelId) {
		this.labelId = labelId;
	}

	public EventFilter withLabelId(final String labelId) {
		this.labelId = labelId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, subtype, labelId);
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
		return Objects.equals(type, other.type) && Objects.equals(subtype, other.subtype) && Objects.equals(labelId, other.labelId);
	}

	@Override
	public String toString() {
		return "EventFilter{type='" + type + "', subtype='" + subtype + "', labelId='" + labelId + "'}";
	}
}
