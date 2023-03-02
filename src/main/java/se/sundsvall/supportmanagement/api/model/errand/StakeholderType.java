package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "StakeholderType model", enumAsRef = true)
public enum StakeholderType {
	PRIVATE,
	ENTERPRISE,
	EMPLOYEE
}
