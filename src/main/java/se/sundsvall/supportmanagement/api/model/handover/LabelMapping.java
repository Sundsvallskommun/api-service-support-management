package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Mapping suggestion for a single namespace-bound label")
public class LabelMapping {

	@Schema(description = "Unique id of the label on the source errand", examples = "uuid-a")
	private String sourceId;

	@Schema(description = "Display name of the source label", examples = "Nyckelkort")
	private String sourceDisplayName;

	@Schema(description = "Hierarchical resource path of the source label", examples = "/access/keycard")
	private String sourceResourcePath;

	@Schema(description = "Auto-suggested target label id, or null if no match was found", examples = "uuid-b")
	private String suggestedTargetId;

	@Schema(description = "Reason the target was suggested, or null if there is no suggestion")
	private MatchReason matchReason;

	public static LabelMapping create() {
		return new LabelMapping();
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(final String sourceId) {
		this.sourceId = sourceId;
	}

	public LabelMapping withSourceId(final String sourceId) {
		this.sourceId = sourceId;
		return this;
	}

	public String getSourceDisplayName() {
		return sourceDisplayName;
	}

	public void setSourceDisplayName(final String sourceDisplayName) {
		this.sourceDisplayName = sourceDisplayName;
	}

	public LabelMapping withSourceDisplayName(final String sourceDisplayName) {
		this.sourceDisplayName = sourceDisplayName;
		return this;
	}

	public String getSourceResourcePath() {
		return sourceResourcePath;
	}

	public void setSourceResourcePath(final String sourceResourcePath) {
		this.sourceResourcePath = sourceResourcePath;
	}

	public LabelMapping withSourceResourcePath(final String sourceResourcePath) {
		this.sourceResourcePath = sourceResourcePath;
		return this;
	}

	public String getSuggestedTargetId() {
		return suggestedTargetId;
	}

	public void setSuggestedTargetId(final String suggestedTargetId) {
		this.suggestedTargetId = suggestedTargetId;
	}

	public LabelMapping withSuggestedTargetId(final String suggestedTargetId) {
		this.suggestedTargetId = suggestedTargetId;
		return this;
	}

	public MatchReason getMatchReason() {
		return matchReason;
	}

	public void setMatchReason(final MatchReason matchReason) {
		this.matchReason = matchReason;
	}

	public LabelMapping withMatchReason(final MatchReason matchReason) {
		this.matchReason = matchReason;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sourceId, sourceDisplayName, sourceResourcePath, suggestedTargetId, matchReason);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelMapping other)) {
			return false;
		}
		return Objects.equals(sourceId, other.sourceId) && Objects.equals(sourceDisplayName, other.sourceDisplayName)
			&& Objects.equals(sourceResourcePath, other.sourceResourcePath) && Objects.equals(suggestedTargetId, other.suggestedTargetId)
			&& matchReason == other.matchReason;
	}

	@Override
	public String toString() {
		return "LabelMapping [sourceId=" + sourceId + ", sourceDisplayName=" + sourceDisplayName + ", sourceResourcePath=" + sourceResourcePath
			+ ", suggestedTargetId=" + suggestedTargetId + ", matchReason=" + matchReason + "]";
	}
}
