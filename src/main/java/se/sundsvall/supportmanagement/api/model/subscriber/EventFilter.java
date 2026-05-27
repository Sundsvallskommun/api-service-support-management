package se.sundsvall.supportmanagement.api.model.subscriber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

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

	@Override
	public int hashCode() {
		return Objects.hash(type, subtype);
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
		return Objects.equals(type, other.type) && Objects.equals(subtype, other.subtype);
	}

	@Override
	public String toString() {
		return "EventFilter{type='" + type + "', subtype='" + subtype + "'}";
	}
}
