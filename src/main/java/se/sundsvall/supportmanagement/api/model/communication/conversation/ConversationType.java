package se.sundsvall.supportmanagement.api.model.communication.conversation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "ConversationType model", enumAsRef = true)
public enum ConversationType {

	INTERNAL,
	EXTERNAL

}
