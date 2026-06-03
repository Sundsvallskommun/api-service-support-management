package se.sundsvall.supportmanagement.api.model.errand.handover;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Action to take on the source errand after handover", enumAsRef = true)
public enum HandoverSourceAction {
	CLOSE
}
