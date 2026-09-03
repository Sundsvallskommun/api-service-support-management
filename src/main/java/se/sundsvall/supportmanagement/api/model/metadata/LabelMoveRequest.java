package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "Request for moving a label to a new parent")
public class LabelMoveRequest {

	@ValidUuid(nullable = true)
	@Schema(description = "ID of the new parent label. Null means move to root.", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7", nullable = true)
	private String newParentId;

	@Schema(description = "If true, return affected counts without making any changes.", defaultValue = "false")
	private boolean dryRun;

	public static LabelMoveRequest create() {
		return new LabelMoveRequest();
	}

	public String getNewParentId() {
		return newParentId;
	}

	public void setNewParentId(final String newParentId) {
		this.newParentId = newParentId;
	}

	public LabelMoveRequest withNewParentId(final String newParentId) {
		this.newParentId = newParentId;
		return this;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(final boolean dryRun) {
		this.dryRun = dryRun;
	}

	public LabelMoveRequest withDryRun(final boolean dryRun) {
		this.dryRun = dryRun;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(newParentId, dryRun);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelMoveRequest other)) {
			return false;
		}
		return dryRun == other.dryRun && Objects.equals(newParentId, other.newParentId);
	}

	@Override
	public String toString() {
		return "LabelMoveRequest[newParentId=" + newParentId + ", dryRun=" + dryRun + "]";
	}
}
