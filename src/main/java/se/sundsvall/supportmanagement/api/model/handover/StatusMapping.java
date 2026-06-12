package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Mapping suggestion for the namespace-bound status field")
public class StatusMapping {

	@Schema(description = "Status on the source errand")
	private MetadataOption source;

	@Schema(description = "Auto-suggested target status name, or null if no match was found", examples = "IN_PROGRESS")
	private String suggestedTarget;

	@Schema(description = "Reason the target was suggested, or null if there is no suggestion")
	private MatchReason matchReason;

	@ArraySchema(arraySchema = @Schema(description = "All selectable statuses in the target namespace. Always present, may be empty", requiredMode = REQUIRED),
		schema = @Schema(implementation = MetadataOption.class))
	private List<MetadataOption> candidates;

	public static StatusMapping create() {
		return new StatusMapping();
	}

	public MetadataOption getSource() {
		return source;
	}

	public void setSource(final MetadataOption source) {
		this.source = source;
	}

	public StatusMapping withSource(final MetadataOption source) {
		this.source = source;
		return this;
	}

	public String getSuggestedTarget() {
		return suggestedTarget;
	}

	public void setSuggestedTarget(final String suggestedTarget) {
		this.suggestedTarget = suggestedTarget;
	}

	public StatusMapping withSuggestedTarget(final String suggestedTarget) {
		this.suggestedTarget = suggestedTarget;
		return this;
	}

	public MatchReason getMatchReason() {
		return matchReason;
	}

	public void setMatchReason(final MatchReason matchReason) {
		this.matchReason = matchReason;
	}

	public StatusMapping withMatchReason(final MatchReason matchReason) {
		this.matchReason = matchReason;
		return this;
	}

	public List<MetadataOption> getCandidates() {
		return candidates;
	}

	public void setCandidates(final List<MetadataOption> candidates) {
		this.candidates = candidates;
	}

	public StatusMapping withCandidates(final List<MetadataOption> candidates) {
		this.candidates = candidates;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, suggestedTarget, matchReason, candidates);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final StatusMapping other)) {
			return false;
		}
		return Objects.equals(source, other.source) && Objects.equals(suggestedTarget, other.suggestedTarget)
			&& matchReason == other.matchReason && Objects.equals(candidates, other.candidates);
	}

	@Override
	public String toString() {
		return "StatusMapping [source=" + source + ", suggestedTarget=" + suggestedTarget + ", matchReason=" + matchReason + ", candidates=" + candidates + "]";
	}
}
