package se.sundsvall.supportmanagement.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of operation", enumAsRef = true)
public enum OperationType {
	CREATE,
	UPDATE,
	DELETE,
	READ
}
