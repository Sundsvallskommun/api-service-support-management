package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;

import java.util.List;

import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;
import se.sundsvall.supportmanagement.service.mapper.NotificationMapper;


@Service
public class NotificationService {

	private static final String NOTIFICATION_ENTITY_NOT_FOUND = "Notification with id %s not found in namespace %s for municipality with id %s";

	private final NotificationRepository notificationRepository;

	public NotificationService(final NotificationRepository notificationRepository) {this.notificationRepository = notificationRepository;}


	public List<Notification> getNotifications(final String municipalityId, final String namespace, final String ownerId) {

		return notificationRepository.findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId).stream().map(NotificationMapper::toNotification).toList();
	}

	public String createNotification(final String municipalityId, final String namespace, final Notification notification) {
		return "";
	}
	
	public void updateNotifications(final String municipalityId, final String namespace, final List<Notification> notifications) {
		notifications.forEach(notification -> updateNotification(municipalityId, namespace, notification.getId(), notification));
	}


	public void updateNotification(final String municipalityId, final String namespace, final String notificationId, final Notification notification) {
		if (!notificationRepository.existsByIdAndNamespaceAndMunicipalityId(notificationId, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(NOTIFICATION_ENTITY_NOT_FOUND, notificationId, namespace, municipalityId));
		}
		final var entity = notificationRepository.getReferenceById(notificationId);
		notificationRepository.save(NotificationMapper.updateEntity(entity, notification));

	}

	public void deleteNotification(final String municipalityId, final String namespace, final String notificationId) {
		if (!notificationRepository.existsByIdAndNamespaceAndMunicipalityId(notificationId, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(NOTIFICATION_ENTITY_NOT_FOUND, notificationId, namespace, municipalityId));
		}
		notificationRepository.deleteById(notificationId);
	}

}
