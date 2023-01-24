package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "CustomerType model", enumAsRef = true)
public enum CustomerType {
	PRIVATE,
	ENTERPRISE,
	EMPLOYEE
}
