package se.sundsvall.supportmanagement.api.model.config;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.validation.ValidStatus;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Message exchange worker config model")
public class MessageExchangeIntegration {

	@Schema(description = "Status on errand that will trigger a status change when a new incoming message refers to an existing errand", examples = "SOLVED", types = {
		"string", "null"
	})
	@ValidStatus
	private String triggerStatusChangeOn;

	@Schema(description = "Status that will be set on errand if status change is triggered. Can only be null if 'triggerStatusChangeOn' is null.", examples = "OPEN", types = {
		"string", "null"
	})
	@ValidStatus
	private String statusChangeTo;

	@Schema(description = "Timestamp when the configuration was created", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "Timestamp when the configuration was last modified", examples = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	public static MessageExchangeIntegration create() {
		return new MessageExchangeIntegration();
	}

	public String getTriggerStatusChangeOn() {
		return triggerStatusChangeOn;
	}

	public void setTriggerStatusChangeOn(final String triggerStatusChangeOn) {
		this.triggerStatusChangeOn = triggerStatusChangeOn;
	}

	public MessageExchangeIntegration withTriggerStatusChangeOn(final String triggerStatusChangeOn) {
		this.triggerStatusChangeOn = triggerStatusChangeOn;
		return this;
	}

	public String getStatusChangeTo() {
		return statusChangeTo;
	}

	public void setStatusChangeTo(final String statusChangeTo) {
		this.statusChangeTo = statusChangeTo;
	}

	public MessageExchangeIntegration withStatusChangeTo(final String statusChangeTo) {
		this.statusChangeTo = statusChangeTo;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public MessageExchangeIntegration withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public MessageExchangeIntegration withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final MessageExchangeIntegration that = (MessageExchangeIntegration) o;
		return Objects.equals(triggerStatusChangeOn, that.triggerStatusChangeOn) && Objects.equals(statusChangeTo, that.statusChangeTo)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(triggerStatusChangeOn, statusChangeTo, created, modified);
	}

	@Override
	public String toString() {
		return "MessageExchangeIntegration{" +
			"triggerStatusChangeOn='" + triggerStatusChangeOn + '\'' +
			", statusChangeTo='" + statusChangeTo + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
