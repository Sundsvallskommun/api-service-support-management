package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
	Type of warning raised while building a handover preview: \
	PARAMETER_SCHEMA_MISMATCH (a parameter references a json schema not registered in the target namespace, see 'key'/'detail'), \
	ROLE_NOT_IN_TARGET (a stakeholder role on the source errand does not exist in the target namespace, see 'value')""", enumAsRef = true)
public enum WarningType {
	PARAMETER_SCHEMA_MISMATCH,
	ROLE_NOT_IN_TARGET
}
