package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
	Reason a target was auto-suggested for a namespace-bound field: \
	NAME_EXACT (exact match on technical name), \
	DISPLAY_NAME_EXACT (case-insensitive exact match on display name), \
	RESOURCE_PATH_MATCH (labels only: exact match on the classification path, the CATEGORY and TYPE segments of the resource path), \
	BEST_EFFORT (labels only: no exact match, the best ranked candidates on the classification path, see matchCandidates)""", enumAsRef = true)
public enum MatchReason {
	NAME_EXACT,
	DISPLAY_NAME_EXACT,
	RESOURCE_PATH_MATCH,
	BEST_EFFORT
}
