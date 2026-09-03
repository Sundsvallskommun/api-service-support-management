package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Result of a label move dry-run — no changes are made")
public class LabelMoveDryRunResponse {

	@Schema(description = "Number of errands that reference the label or any of its descendants")
	private long affectedErrandCount;

	@Schema(description = "Actions that have hasLabel conditions referencing the moved label or its descendants")
	private List<AffectedAction> affectedActions;

	public static LabelMoveDryRunResponse create() {
		return new LabelMoveDryRunResponse();
	}

	public long getAffectedErrandCount() {
		return affectedErrandCount;
	}

	public void setAffectedErrandCount(final long affectedErrandCount) {
		this.affectedErrandCount = affectedErrandCount;
	}

	public LabelMoveDryRunResponse withAffectedErrandCount(final long affectedErrandCount) {
		this.affectedErrandCount = affectedErrandCount;
		return this;
	}

	public List<AffectedAction> getAffectedActions() {
		return affectedActions;
	}

	public void setAffectedActions(final List<AffectedAction> affectedActions) {
		this.affectedActions = affectedActions;
	}

	public LabelMoveDryRunResponse withAffectedActions(final List<AffectedAction> affectedActions) {
		this.affectedActions = affectedActions;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(affectedErrandCount, affectedActions);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelMoveDryRunResponse other)) {
			return false;
		}
		return affectedErrandCount == other.affectedErrandCount && Objects.equals(affectedActions, other.affectedActions);
	}

	@Override
	public String toString() {
		return "LabelMoveDryRunResponse[affectedErrandCount=" + affectedErrandCount + ", affectedActions=" + affectedActions + "]";
	}
}
