package se.sundsvall.supportmanagement.api.model.subscriber;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Channel through which a notification is delivered")
public enum NotificationChannelType {

	/** Internal GUI notification (default). */
	INTERNAL,

	/** Notification delivered via e-mail. */
	EMAIL,

	/** Notification delivered via SMS. */
	SMS
}
