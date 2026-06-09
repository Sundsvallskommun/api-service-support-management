package se.sundsvall.supportmanagement.api.model.errand.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@Schema(description = "Defines what happens to the source errand after handover")
public class HandoverSourceHandling {

	@NotNull
	@Schema(implementation = HandoverSourceAction.class)
	private HandoverSourceAction action;

	@Schema(description = "Status to set on the source errand", example = "SOLVED")
	private String status;

	@Schema(description = "Resolution to set on the source errand", example = "HANDED_OVER")
	private String resolution;

	@Schema(description = "Closing comment to add to the source errand", example = "Överlämnad till annan drake")
	private String closingComment;

	public static HandoverSourceHandling create() {
		return new HandoverSourceHandling();
	}

	public HandoverSourceAction getAction() {
		return action;
	}

	public void setAction(final HandoverSourceAction action) {
		this.action = action;
	}

	public HandoverSourceHandling withAction(final HandoverSourceAction action) {
		this.action = action;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public HandoverSourceHandling withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(final String resolution) {
		this.resolution = resolution;
	}

	public HandoverSourceHandling withResolution(final String resolution) {
		this.resolution = resolution;
		return this;
	}

	public String getClosingComment() {
		return closingComment;
	}

	public void setClosingComment(final String closingComment) {
		this.closingComment = closingComment;
	}

	public HandoverSourceHandling withClosingComment(final String closingComment) {
		this.closingComment = closingComment;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final HandoverSourceHandling that = (HandoverSourceHandling) o;
		return action == that.action && Objects.equals(status, that.status) && Objects.equals(resolution, that.resolution) && Objects.equals(closingComment, that.closingComment);
	}

	@Override
	public int hashCode() {
		return Objects.hash(action, status, resolution, closingComment);
	}

	@Override
	public String toString() {
		return "HandoverSourceHandling{action=" + action + ", status='" + status + "', resolution='" + resolution + "', closingComment='" + closingComment + "'}";
	}
}
