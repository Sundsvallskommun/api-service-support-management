package se.sundsvall.supportmanagement.service;

import java.util.List;

import org.springframework.stereotype.Service;

import se.sundsvall.supportmanagement.api.model.notification.Notification;

@Service
public class NotificationService {


	public List<Notification> getNotifications(final String municipalityId, final String namespace, final String ownerId) {
		return List.of();
	}

	public String createNotification(final String municipalityId, final String namespace, final Notification notification) {
		return "";
	}

	public void updateNotification(final String municipalityId, final String namespace, final String notificationId, final Notification notification) {
		// TODO  Update notification
	}

	public void deleteNotification(final String municipalityId, final String namespace, final String notificationId) {
		// TODO  Delete notification
	}

}
