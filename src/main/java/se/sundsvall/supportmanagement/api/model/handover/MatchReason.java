package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
	Reason a target was auto-suggested for a namespace-bound field: \
	NAME_EXACT (exact match on technical name), \
	DISPLAY_NAME_EXACT (case-insensitive exact match on display name), \
	RESOURCE_PATH_MATCH (match on hierarchical resource path, labels only)""", enumAsRef = true)
public enum MatchReason {
	NAME_EXACT,
	DISPLAY_NAME_EXACT,
	RESOURCE_PATH_MATCH
}
