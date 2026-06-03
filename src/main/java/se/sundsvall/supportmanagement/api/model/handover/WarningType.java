package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of warning raised while building a handover preview")
public enum WarningType {

	@Schema(description = "A parameter references a json schema that is not registered in the target namespace")
	PARAMETER_SCHEMA_MISMATCH,

	@Schema(description = "A stakeholder role on the source errand does not exist in the target namespace")
	ROLE_NOT_IN_TARGET
}
