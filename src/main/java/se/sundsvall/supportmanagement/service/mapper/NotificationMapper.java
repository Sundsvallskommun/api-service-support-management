package se.sundsvall.supportmanagement.service.mapper;

import static org.apache.commons.lang3.ObjectUtils.anyNull;

import java.util.Optional;

import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;

public class NotificationMapper {

	private NotificationMapper() {
		// Intentionally empty
	}


	public static NotificationEntity toNotificationEntity(final String namespace, final String municipalityId, final Notification notification) {
		return NotificationEntity.create()
			.withOwnerFullName(notification.getOwnerFullName())
			.withOwnerId(notification.getOwnerId())
			.withCreatedBy(notification.getCreatedBy())
			.withCreatedByFullName(notification.getCreatedByFullName())
			.withType(notification.getType())
			.withDescription(notification.getDescription())
			.withContent(notification.getContent())
			.withExpires(notification.getExpires())
			.withAcknowledged(notification.isAcknowledged())
			.withErrandId(notification.getErrandId())
			.withErrandNumber(notification.getErrandNumber())
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace);
	}

	public static NotificationEntity updateEntity(final NotificationEntity entity, final Notification notification) {
		if (anyNull(entity, notification)) {
			return entity;
		}
		Optional.ofNullable(notification.getOwnerFullName()).ifPresent(entity::setOwnerFullName);
		Optional.ofNullable(notification.getOwnerId()).ifPresent(entity::setOwnerId);
		Optional.ofNullable(notification.getCreatedBy()).ifPresent(entity::setCreatedBy);
		Optional.ofNullable(notification.getType()).ifPresent(entity::setType);
		Optional.ofNullable(notification.getCreatedByFullName()).ifPresent(entity::setCreatedByFullName);
		Optional.ofNullable(notification.getDescription()).ifPresent(entity::setDescription);
		Optional.ofNullable(notification.getContent()).ifPresent(entity::setContent);
		Optional.ofNullable(notification.getExpires()).ifPresent(entity::setExpires);
		Optional.of(notification.isAcknowledged()).ifPresent(entity::setAcknowledged);
		Optional.ofNullable(notification.getErrandId()).ifPresent(entity::setErrandId);
		Optional.ofNullable(notification.getErrandNumber()).ifPresent(entity::setErrandNumber);
		return entity;
	}

	public static Notification toNotification(final NotificationEntity notificationEntity) {

		return Optional.ofNullable(notificationEntity)
			.map(entity -> Notification.create()
				.withId(entity.getId())
				.withOwnerFullName(entity.getOwnerFullName())
				.withOwnerId(entity.getOwnerId())
				.withCreatedBy(entity.getCreatedBy())
				.withCreatedByFullName(entity.getCreatedByFullName())
				.withType(entity.getType())
				.withDescription(entity.getDescription())
				.withContent(entity.getContent())
				.withExpires(entity.getExpires())
				.withAcknowledged(entity.isAcknowledged())
				.withErrandId(entity.getErrandId())
				.withErrandNumber(entity.getErrandNumber())
				.withModified(entity.getModified())
				.withCreated(entity.getCreated()))
			.orElse(null);
	}

}
