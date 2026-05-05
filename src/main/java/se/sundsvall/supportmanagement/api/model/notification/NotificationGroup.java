package se.sundsvall.supportmanagement.api.model.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

public class NotificationGroup {

	@Schema(description = "The request group ID that groups related notifications", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
	private String requestGroupId;

	@Schema(description = "Notifications belonging to this group")
	private List<Notification> notifications;

	public static NotificationGroup create() {
		return new NotificationGroup();
	}

	public String getRequestGroupId() {
		return requestGroupId;
	}

	public void setRequestGroupId(final String requestGroupId) {
		this.requestGroupId = requestGroupId;
	}

	public NotificationGroup withRequestGroupId(final String requestGroupId) {
		this.requestGroupId = requestGroupId;
		return this;
	}

	public List<Notification> getNotifications() {
		return notifications;
	}

	public void setNotifications(final List<Notification> notifications) {
		this.notifications = notifications;
	}

	public NotificationGroup withNotifications(final List<Notification> notifications) {
		this.notifications = notifications;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NotificationGroup that = (NotificationGroup) o;
		return Objects.equals(requestGroupId, that.requestGroupId) && Objects.equals(notifications, that.notifications);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestGroupId, notifications);
	}

	@Override
	public String toString() {
		return "NotificationGroup{" +
			"requestGroupId='" + requestGroupId + '\'' +
			", notifications=" + notifications +
			'}';
	}
}
