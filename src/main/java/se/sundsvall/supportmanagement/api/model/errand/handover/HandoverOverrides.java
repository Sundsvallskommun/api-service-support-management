package se.sundsvall.supportmanagement.api.model.errand.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.model.errand.Priority;

@Schema(description = "Field values that override what is copied from the source errand")
public class HandoverOverrides {

	@Schema(description = "Title override")
	private String title;

	@Schema(description = "Description override")
	private String description;

	@Schema(implementation = Priority.class)
	private Priority priority;

	@Schema(description = "Assigned user id override")
	private String assignedUserId;

	@Schema(description = "Assigned group id override")
	private String assignedGroupId;

	public static HandoverOverrides create() {
		return new HandoverOverrides();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public HandoverOverrides withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public HandoverOverrides withDescription(final String description) {
		this.description = description;
		return this;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(final Priority priority) {
		this.priority = priority;
	}

	public HandoverOverrides withPriority(final Priority priority) {
		this.priority = priority;
		return this;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public HandoverOverrides withAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public String getAssignedGroupId() {
		return assignedGroupId;
	}

	public void setAssignedGroupId(final String assignedGroupId) {
		this.assignedGroupId = assignedGroupId;
	}

	public HandoverOverrides withAssignedGroupId(final String assignedGroupId) {
		this.assignedGroupId = assignedGroupId;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final HandoverOverrides that = (HandoverOverrides) o;
		return Objects.equals(title, that.title) && Objects.equals(description, that.description)
			&& priority == that.priority && Objects.equals(assignedUserId, that.assignedUserId)
			&& Objects.equals(assignedGroupId, that.assignedGroupId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, description, priority, assignedUserId, assignedGroupId);
	}

	@Override
	public String toString() {
		return "HandoverOverrides{title='" + title + "', description='" + description + "', priority=" + priority
			+ ", assignedUserId='" + assignedUserId + "', assignedGroupId='" + assignedGroupId + "'}";
	}
}
