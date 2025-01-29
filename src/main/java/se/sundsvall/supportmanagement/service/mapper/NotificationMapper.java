package se.sundsvall.supportmanagement.service.mapper;

import static java.time.OffsetDateTime.now;
import static org.apache.commons.lang3.ObjectUtils.anyNull;

import generated.se.sundsvall.eventlog.Event;
import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;

public final class NotificationMapper {

	private static final int DEFAULT_EXPIRE_TIME_IN_DAYS = 30;

	private NotificationMapper() {
		// Intentionally empty
	}

	public static NotificationEntity toNotificationEntity(final String namespace, final String municipalityId, final Notification notification, final ErrandEntity errandEntity) {
		return NotificationEntity.create()
			.withOwnerId(notification.getOwnerId())
			.withCreatedBy(notification.getCreatedBy())
			.withType(notification.getType())
			.withDescription(notification.getDescription())
			.withContent(notification.getContent())
			.withExpires(Optional.ofNullable(notification.getExpires()).orElse(now().plusDays(DEFAULT_EXPIRE_TIME_IN_DAYS)))
			.withAcknowledged(notification.isAcknowledged())
			.withGlobalAcknowledged(notification.isGlobalAcknowledged())
			.withErrandEntity(errandEntity)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace);
	}

	public static NotificationEntity updateEntity(final NotificationEntity entity, final Notification notification) {
		if (anyNull(entity, notification)) {
			return entity;
		}

		Optional.ofNullable(notification.getOwnerId()).ifPresent(entity::setOwnerId);
		Optional.ofNullable(notification.getType()).ifPresent(entity::setType);
		Optional.ofNullable(notification.getDescription()).ifPresent(entity::setDescription);
		Optional.ofNullable(notification.getContent()).ifPresent(entity::setContent);
		Optional.ofNullable(notification.getExpires()).ifPresent(entity::setExpires);
		Optional.of(notification.isAcknowledged()).ifPresent(entity::setAcknowledged);
		Optional.of(notification.isGlobalAcknowledged()).ifPresent(entity::setGlobalAcknowledged);

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
				.withGlobalAcknowledged(entity.isGlobalAcknowledged())
				.withErrandId(entity.getErrandEntity().getId())
				.withErrandNumber(entity.getErrandEntity().getErrandNumber())
				.withModified(entity.getModified())
				.withCreated(entity.getCreated()))
			.orElse(null);
	}

	public static Notification toNotification(final Event event, final ErrandEntity errandEntity, final String executingUser) {
		return Notification.create()
			.withDescription(event.getMessage())
			.withErrandId(errandEntity.getId())
			.withErrandNumber(errandEntity.getErrandNumber())
			.withType(event.getType().getValue())
			.withExpires(event.getExpires())
			.withModified(event.getCreated())
			.withCreated(event.getCreated())
			.withCreatedBy(executingUser)
			.withOwnerId(errandEntity.getAssignedUserId());
	}
}
