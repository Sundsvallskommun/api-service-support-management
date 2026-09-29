package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Result of a label-tree restructure dry-run — no changes are made")
public class LabelRestructureDryRunResponse {

	@Schema(description = "Sum of affectedErrandCount across all steps. An errand touched by more than one step is counted once per step that touches it, not deduplicated across steps")
	private long totalAffectedErrandCount;

	@Schema(description = "Per-step results, in the same order as the request's steps")
	private List<LabelRestructureStepResult> steps;

	public static LabelRestructureDryRunResponse create() {
		return new LabelRestructureDryRunResponse();
	}

	public long getTotalAffectedErrandCount() {
		return totalAffectedErrandCount;
	}

	public void setTotalAffectedErrandCount(final long totalAffectedErrandCount) {
		this.totalAffectedErrandCount = totalAffectedErrandCount;
	}

	public LabelRestructureDryRunResponse withTotalAffectedErrandCount(final long totalAffectedErrandCount) {
		this.totalAffectedErrandCount = totalAffectedErrandCount;
		return this;
	}

	public List<LabelRestructureStepResult> getSteps() {
		return steps;
	}

	public void setSteps(final List<LabelRestructureStepResult> steps) {
		this.steps = steps;
	}

	public LabelRestructureDryRunResponse withSteps(final List<LabelRestructureStepResult> steps) {
		this.steps = steps;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(totalAffectedErrandCount, steps);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelRestructureDryRunResponse other)) {
			return false;
		}
		return totalAffectedErrandCount == other.totalAffectedErrandCount && Objects.equals(steps, other.steps);
	}

	@Override
	public String toString() {
		return "LabelRestructureDryRunResponse[totalAffectedErrandCount=" + totalAffectedErrandCount + ", steps=" + steps + "]";
	}
}
