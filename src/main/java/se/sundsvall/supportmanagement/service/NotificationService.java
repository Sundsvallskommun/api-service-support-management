package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.mapper.NotificationMapper;

@Service
public class NotificationService {

	private static final String NOTIFICATION_ENTITY_NOT_FOUND = "Notification with id:'%s' not found in namespace:'%s' for municipality with id:'%s' and errand with id:'%s'";
	private static final String ERRAND_ENTITY_NOT_FOUND = "Errand with id:'%s' not found in namespace:'%s' for municipality with id:'%s'";

	private final NotificationRepository notificationRepository;
	private final ErrandsRepository errandsRepository;

	public NotificationService(final NotificationRepository notificationRepository, final ErrandsRepository errandsRepository) {
		this.notificationRepository = notificationRepository;
		this.errandsRepository = errandsRepository;
	}

	public Notification getNotification(final String municipalityId, final String namespace, final String errandId, final String notificationId) {
		return notificationRepository.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, errandId)
			.map(NotificationMapper::toNotification)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(NOTIFICATION_ENTITY_NOT_FOUND, notificationId, namespace, municipalityId, errandId)));
	}

	public List<Notification> getNotificationsByOwnerId(final String municipalityId, final String namespace, final String ownerId) {
		return notificationRepository.findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId)
			.stream()
			.map(NotificationMapper::toNotification)
			.toList();
	}

	public List<Notification> getNotificationsByErrandId(final String municipalityId, final String namespace, final String errandId, final Sort sort) {
		return notificationRepository.findAllByNamespaceAndMunicipalityIdAndErrandEntityId(namespace, municipalityId, errandId, sort)
			.stream()
			.map(NotificationMapper::toNotification)
			.toList();
	}

	public List<Notification> getNotifications(final String municipalityId, final String namespace, final String ownerId) {
		return notificationRepository.findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId).stream().map(NotificationMapper::toNotification).toList();
	}

	public String createNotification(final String municipalityId, final String namespace, final String errandId, final Notification notification) {
		if (notificationExist(municipalityId, namespace, errandId, notification)) {
			return null;
		}

		final var errandEntity = errandsRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, notification.getErrandId(), namespace, municipalityId)));

		final var entity = NotificationMapper.toNotificationEntity(namespace, municipalityId, notification, errandEntity);

		return notificationRepository.save(entity).getId();
	}

	private boolean notificationExist(final String municipalityId, final String namespace, final String errandId, final Notification notification) {
		return notificationRepository
			.findByNamespaceAndMunicipalityIdAndOwnerIdAndAcknowledgedAndErrandEntityIdAndType(
				namespace,
				municipalityId,
				notification.getOwnerId(),
				notification.isAcknowledged(),
				errandId,
				notification.getType())
			.isPresent();
	}

	public boolean doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(final String municipalityId, final String namespace, final String ownerId, final ErrandEntity errandEntity, final String description,
		final OffsetDateTime created) {
		return notificationRepository.existsByNamespaceAndMunicipalityIdAndOwnerIdAndErrandEntityAndDescriptionAndCreatedIsAfter(namespace,
			municipalityId,
			ownerId,
			errandEntity,
			description,
			created);
	}

	@Transactional
	public void updateNotifications(final String municipalityId, final String namespace, final List<Notification> notifications) {
		notifications.forEach(notification -> updateNotification(municipalityId, namespace, notification.getId(), notification));
	}

	private void updateNotification(final String municipalityId, final String namespace, final String notificationId, final Notification notification) {
		final var entity = notificationRepository.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, notification.getErrandId())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(NOTIFICATION_ENTITY_NOT_FOUND, notificationId, namespace, municipalityId, notification.getErrandId())));
		notificationRepository.save(NotificationMapper.updateEntity(entity, notification));
	}

	public void deleteNotification(final String municipalityId, final String namespace, final String errandId, final String notificationId) {
		if (!notificationRepository.existsByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, errandId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(NOTIFICATION_ENTITY_NOT_FOUND, notificationId, namespace, municipalityId, errandId));
		}
		notificationRepository.deleteById(notificationId);
	}
}
