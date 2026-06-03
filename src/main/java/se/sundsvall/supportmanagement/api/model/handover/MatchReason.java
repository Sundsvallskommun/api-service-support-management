package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reason a target was auto-suggested for a namespace-bound field")
public enum MatchReason {

	@Schema(description = "Exact match on the technical name")
	NAME_EXACT,

	@Schema(description = "Case-insensitive exact match on the display name")
	DISPLAY_NAME_EXACT,

	@Schema(description = "Match on the hierarchical resource path (labels only)")
	RESOURCE_PATH_MATCH
}
