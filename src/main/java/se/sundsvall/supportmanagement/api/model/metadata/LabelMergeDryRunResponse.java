package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Result of a label merge dry-run — no changes are made")
public class LabelMergeDryRunResponse {

	@Schema(description = "Number of errands that reference one or more of the source labels")
	private long affectedErrandCount;

	@Schema(description = "Actions that have hasLabel conditions referencing one or more of the source labels")
	private List<AffectedAction> affectedActions;

	public static LabelMergeDryRunResponse create() {
		return new LabelMergeDryRunResponse();
	}

	public long getAffectedErrandCount() {
		return affectedErrandCount;
	}

	public void setAffectedErrandCount(final long affectedErrandCount) {
		this.affectedErrandCount = affectedErrandCount;
	}

	public LabelMergeDryRunResponse withAffectedErrandCount(final long affectedErrandCount) {
		this.affectedErrandCount = affectedErrandCount;
		return this;
	}

	public List<AffectedAction> getAffectedActions() {
		return affectedActions;
	}

	public void setAffectedActions(final List<AffectedAction> affectedActions) {
		this.affectedActions = affectedActions;
	}

	public LabelMergeDryRunResponse withAffectedActions(final List<AffectedAction> affectedActions) {
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
		if (!(obj instanceof final LabelMergeDryRunResponse other)) {
			return false;
		}
		return affectedErrandCount == other.affectedErrandCount && Objects.equals(affectedActions, other.affectedActions);
	}

	@Override
	public String toString() {
		return "LabelMergeDryRunResponse[affectedErrandCount=" + affectedErrandCount + ", affectedActions=" + affectedActions + "]";
	}
}
