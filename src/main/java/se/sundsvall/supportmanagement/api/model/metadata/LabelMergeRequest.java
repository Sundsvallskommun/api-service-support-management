package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Merging labels reassigns every errand under one or more source labels to a destination label and then removes the
 * sources, and cannot be undone, so dryRun carries no default for the same reason it carries none on
 * {@link LabelMoveRequest} - leaving it out is far more likely to be an oversight than a considered request to start
 * that, and a caller that has to write the intent out cannot make that mistake silently.
 */
@Schema(description = "Request for merging one or more source labels into a destination label")
public class LabelMergeRequest {

	@NotEmpty
	@ArraySchema(arraySchema = @Schema(description = "IDs of the source labels to merge into the destination label", requiredMode = REQUIRED))
	private List<@ValidUuid String> sourceLabelIds;

	@NotNull
	@Schema(description = "When true, return affected counts without making any changes. When false, starts the merge as an asynchronous job.", examples = "true", requiredMode = REQUIRED)
	private Boolean dryRun;

	public static LabelMergeRequest create() {
		return new LabelMergeRequest();
	}

	public List<String> getSourceLabelIds() {
		return sourceLabelIds;
	}

	public void setSourceLabelIds(final List<String> sourceLabelIds) {
		this.sourceLabelIds = sourceLabelIds;
	}

	public LabelMergeRequest withSourceLabelIds(final List<String> sourceLabelIds) {
		this.sourceLabelIds = sourceLabelIds;
		return this;
	}

	public Boolean getDryRun() {
		return dryRun;
	}

	public void setDryRun(final Boolean dryRun) {
		this.dryRun = dryRun;
	}

	public LabelMergeRequest withDryRun(final Boolean dryRun) {
		this.dryRun = dryRun;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sourceLabelIds, dryRun);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelMergeRequest other)) {
			return false;
		}
		return Objects.equals(dryRun, other.dryRun) && Objects.equals(sourceLabelIds, other.sourceLabelIds);
	}

	@Override
	public String toString() {
		return "LabelMergeRequest[sourceLabelIds=" + sourceLabelIds + ", dryRun=" + dryRun + "]";
	}
}
