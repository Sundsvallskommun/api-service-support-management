package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Request for moving a label to a new parent. Moving a label reshuffles every errand under it and cannot be undone,
 * and dryRun carries no default, so the caller always has to state whether the move is started.
 */
@Schema(description = "Request for moving a label to a new parent")
public class LabelMoveRequest {

	@ValidUuid(nullable = true)
	@Schema(description = "ID of the new parent label. Null means move to root.", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7", nullable = true)
	private String newParentId;

	@NotNull
	@Schema(description = "When true, return affected counts without making any changes. When false, starts the move as an asynchronous job.", examples = "true", requiredMode = REQUIRED)
	private Boolean dryRun;

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

	public Boolean getDryRun() {
		return dryRun;
	}

	public void setDryRun(final Boolean dryRun) {
		this.dryRun = dryRun;
	}

	public LabelMoveRequest withDryRun(final Boolean dryRun) {
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
		return Objects.equals(dryRun, other.dryRun) && Objects.equals(newParentId, other.newParentId);
	}

	@Override
	public String toString() {
		return "LabelMoveRequest[newParentId=" + newParentId + ", dryRun=" + dryRun + "]";
	}
}
