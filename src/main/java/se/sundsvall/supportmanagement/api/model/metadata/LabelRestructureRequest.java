package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.validation.ValidRestructureStep;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * A label-tree restructure reshuffles the tree and, for MOVE/MERGE steps, the errands under it, and cannot be undone,
 * so dryRun carries no default for the same reason it carries none on {@link LabelMoveRequest}/
 * {@link LabelMergeRequest} - leaving it out is far more likely to be an oversight than a considered request to
 * start that, and a caller that has to write the intent out cannot make that mistake silently.
 */
@Schema(description = "Request for restructuring the label tree as one ordered sequence of add/rename/delete/move/merge steps")
public class LabelRestructureRequest {

	@NotEmpty
	@ArraySchema(arraySchema = @Schema(description = "Steps to apply, in order. A later step may reference a label an earlier step in this same request added or moved", requiredMode = REQUIRED))
	private List<@Valid @ValidRestructureStep LabelRestructureStep> steps;

	@NotNull
	@Schema(description = "When true, return affected counts for every step without making any changes. When false, starts the restructure as an asynchronous job.", examples = "true", requiredMode = REQUIRED)
	private Boolean dryRun;

	public static LabelRestructureRequest create() {
		return new LabelRestructureRequest();
	}

	public List<LabelRestructureStep> getSteps() {
		return steps;
	}

	public void setSteps(final List<LabelRestructureStep> steps) {
		this.steps = steps;
	}

	public LabelRestructureRequest withSteps(final List<LabelRestructureStep> steps) {
		this.steps = steps;
		return this;
	}

	public Boolean getDryRun() {
		return dryRun;
	}

	public void setDryRun(final Boolean dryRun) {
		this.dryRun = dryRun;
	}

	public LabelRestructureRequest withDryRun(final Boolean dryRun) {
		this.dryRun = dryRun;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(steps, dryRun);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelRestructureRequest other)) {
			return false;
		}
		return Objects.equals(dryRun, other.dryRun) && Objects.equals(steps, other.steps);
	}

	@Override
	public String toString() {
		return "LabelRestructureRequest[steps=" + steps + ", dryRun=" + dryRun + "]";
	}
}
