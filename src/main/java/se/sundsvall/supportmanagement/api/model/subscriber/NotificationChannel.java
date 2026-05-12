package se.sundsvall.supportmanagement.api.model.subscriber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@Schema(description = "Channel a subscriber wants to receive notifications on")
public class NotificationChannel {

	@NotNull
	@Schema(description = "Channel type", examples = "EMAIL")
	private NotificationChannelType type;

	@Schema(description = "Optional destination override (e.g. an alternative e-mail address or phone number). " +
		"If omitted, the default destination derived from the subscriber's identifier is used.", examples = "user@example.com")
	private String destination;

	public static NotificationChannel create() {
		return new NotificationChannel();
	}

	public NotificationChannelType getType() {
		return type;
	}

	public void setType(final NotificationChannelType type) {
		this.type = type;
	}

	public NotificationChannel withType(final NotificationChannelType type) {
		this.type = type;
		return this;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(final String destination) {
		this.destination = destination;
	}

	public NotificationChannel withDestination(final String destination) {
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
		final NotificationChannel other = (NotificationChannel) obj;
		return type == other.type && Objects.equals(destination, other.destination);
	}

	@Override
	public String toString() {
		return "NotificationChannel{type=" + type + ", destination='" + destination + "'}";
	}
}
