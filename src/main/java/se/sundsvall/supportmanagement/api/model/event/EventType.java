package se.sundsvall.supportmanagement.api.model.event;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of event", enumAsRef = true)
public enum EventType {
	CREATE,
	UPDATE,
	DELETE,
	UNKNOWN
}
