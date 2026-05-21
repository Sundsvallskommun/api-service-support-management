package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;

@Embeddable
public class NotificationChannelEmbeddable {

	@Column(name = "type", nullable = false, length = 32)
	private NotificationChannelType type;

	@Column(name = "destination")
	private String destination;

	public static NotificationChannelEmbeddable create() {
		return new NotificationChannelEmbeddable();
	}

	public NotificationChannelType getType() {
		return type;
	}

	public void setType(final NotificationChannelType type) {
		this.type = type;
	}

	public NotificationChannelEmbeddable withType(final NotificationChannelType type) {
		this.type = type;
		return this;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(final String destination) {
		this.destination = destination;
	}

	public NotificationChannelEmbeddable withDestination(final String destination) {
		this.destination = destination;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, destination);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final NotificationChannelEmbeddable other = (NotificationChannelEmbeddable) obj;
		return Objects.equals(type, other.type) && Objects.equals(destination, other.destination);
	}

	@Override
	public String toString() {
		return "NotificationChannelEmbeddable{type='" + type + "', destination='" + destination + "'}";
	}
}
