package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class EmailDispatchOutboxEventEmbeddable {

	@Column(name = "event_id", length = 36)
	private String eventId;

	@Column(name = "description")
	private String description;

	public static EmailDispatchOutboxEventEmbeddable create() {
		return new EmailDispatchOutboxEventEmbeddable();
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(final String eventId) {
		this.eventId = eventId;
	}

	public EmailDispatchOutboxEventEmbeddable withEventId(final String eventId) {
		this.eventId = eventId;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public EmailDispatchOutboxEventEmbeddable withDescription(final String description) {
		this.description = description;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(eventId, description);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final EmailDispatchOutboxEventEmbeddable other = (EmailDispatchOutboxEventEmbeddable) obj;
		return Objects.equals(eventId, other.eventId) && Objects.equals(description, other.description);
	}

	@Override
	public String toString() {
		return "EmailDispatchOutboxEventEmbeddable{" +
			"eventId='" + eventId + '\'' +
			", description='" + description + '\'' +
			'}';
	}
}
