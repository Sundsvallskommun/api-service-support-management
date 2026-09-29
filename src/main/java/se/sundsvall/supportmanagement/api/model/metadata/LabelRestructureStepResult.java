package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Dry-run result for one step of a label-tree restructure")
public class LabelRestructureStepResult {

	@Schema(description = "Index of the step in the request's steps list", examples = "0")
	private int index;

	@Schema(description = "The step's type")
	private LabelRestructureStepType type;

	@Schema(description = "The step's path, as submitted")
	private List<String> path;

	@Schema(description = "Number of errands that reference the label or, for MOVE/MERGE, any of its descendants/sources. Always 0 for ADD/RENAME, and for a step whose path was only created by an earlier step in the same request")
	private long affectedErrandCount;

	@Schema(description = "Actions that have hasLabel conditions referencing a label this step affects")
	private List<AffectedAction> affectedActions;

	public static LabelRestructureStepResult create() {
		return new LabelRestructureStepResult();
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(final int index) {
		this.index = index;
	}

	public LabelRestructureStepResult withIndex(final int index) {
		this.index = index;
		return this;
	}

	public LabelRestructureStepType getType() {
		return type;
	}

	public void setType(final LabelRestructureStepType type) {
		this.type = type;
	}

	public LabelRestructureStepResult withType(final LabelRestructureStepType type) {
		this.type = type;
		return this;
	}

	public List<String> getPath() {
		return path;
	}

	public void setPath(final List<String> path) {
		this.path = path;
	}

	public LabelRestructureStepResult withPath(final List<String> path) {
		this.path = path;
		return this;
	}

	public long getAffectedErrandCount() {
		return affectedErrandCount;
	}

	public void setAffectedErrandCount(final long affectedErrandCount) {
		this.affectedErrandCount = affectedErrandCount;
	}

	public LabelRestructureStepResult withAffectedErrandCount(final long affectedErrandCount) {
		this.affectedErrandCount = affectedErrandCount;
		return this;
	}

	public List<AffectedAction> getAffectedActions() {
		return affectedActions;
	}

	public void setAffectedActions(final List<AffectedAction> affectedActions) {
		this.affectedActions = affectedActions;
	}

	public LabelRestructureStepResult withAffectedActions(final List<AffectedAction> affectedActions) {
		this.affectedActions = affectedActions;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(index, type, path, affectedErrandCount, affectedActions);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelRestructureStepResult other)) {
			return false;
		}
		return index == other.index && affectedErrandCount == other.affectedErrandCount && type == other.type &&
			Objects.equals(path, other.path) && Objects.equals(affectedActions, other.affectedActions);
	}

	@Override
	public String toString() {
		return "LabelRestructureStepResult[index=" + index + ", type=" + type + ", path=" + path + ", affectedErrandCount=" + affectedErrandCount + ", affectedActions=" + affectedActions + "]";
	}
}
