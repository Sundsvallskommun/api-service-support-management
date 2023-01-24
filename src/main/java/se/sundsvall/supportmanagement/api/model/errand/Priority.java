package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Priority model", enumAsRef = true)
public enum Priority {
	LOW,
	MEDIUM,
	HIGH
}
