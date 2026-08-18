package se.sundsvall.supportmanagement.api.model.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

public class SubscriberNotificationEvent {

	@DateTimeFormat(iso = DATE_TIME)
	@Schema(description = "Timestamp when the event occurred", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "Event type", example = "UPDATE", accessMode = READ_ONLY)
	private String eventType;

	@Schema(description = "Description of the event", example = "Bilaga har skapats", accessMode = READ_ONLY)
	private String description;

	@Schema(description = "Subtype describing what kind of entity the event refers to", example = "ATTACHMENT", accessMode = READ_ONLY)
	private String subType;

	public static SubscriberNotificationEvent create() {
		return new SubscriberNotificationEvent();
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public SubscriberNotificationEvent withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(final String eventType) {
		this.eventType = eventType;
	}

	public SubscriberNotificationEvent withEventType(final String eventType) {
		this.eventType = eventType;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public SubscriberNotificationEvent withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(final String subType) {
		this.subType = subType;
	}

	public SubscriberNotificationEvent withSubType(final String subType) {
		this.subType = subType;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, eventType, description, subType);
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final SubscriberNotificationEvent that = (SubscriberNotificationEvent) o;
		return Objects.equals(created, that.created)
			&& Objects.equals(eventType, that.eventType)
			&& Objects.equals(description, that.description)
			&& Objects.equals(subType, that.subType);
	}

	@Override
	public String toString() {
		return "SubscriberNotificationEvent{" +
			"created=" + created +
			", eventType='" + eventType + '\'' +
			", description='" + description + '\'' +
			", subType='" + subType + '\'' +
			'}';
	}
}
