package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.notification.SubscriberNotification;
import se.sundsvall.supportmanagement.api.model.notification.SubscriberNotificationEvent;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEventEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static java.time.OffsetDateTime.now;
import static java.util.Optional.ofNullable;

public final class SubscriberNotificationMapper {

	private SubscriberNotificationMapper() {}

	public static SubscriberNotificationEntity toEntity(final String errandId, final String errandNumber, final SubscriberEntity subscriber, final int notificationTTLInDays, final List<SubscriberNotificationEventEntity> events) {
		return SubscriberNotificationEntity.create()
			.withMunicipalityId(subscriber.getMunicipalityId())
			.withNamespace(subscriber.getNamespace())
			.withIdentifierType(subscriber.getIdentifier().getType())
			.withIdentifierValue(subscriber.getIdentifier().getValue())
			.withErrandId(errandId)
			.withErrandNumber(errandNumber)
			.withExpires(now().plusDays(notificationTTLInDays))
			.withEvents(events);
	}

	public static SubscriberNotificationEventEntity toEventEntity(final String eventType, final String description, final String subType) {
		return SubscriberNotificationEventEntity.create()
			.withEventType(eventType)
			.withDescription(description)
			.withSubType(subType);
	}

	public static SubscriberNotification toModel(final SubscriberNotificationEntity entity) {
		return SubscriberNotification.create()
			.withId(entity.getId())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified())
			.withIdentifierType(entity.getIdentifierType())
			.withIdentifierValue(entity.getIdentifierValue())
			.withErrandId(entity.getErrandId())
			.withErrandNumber(entity.getErrandNumber())
			.withExpires(entity.getExpires())
			.withAcknowledged(entity.getAcknowledged())
			.withEvents(ofNullable(entity.getEvents())
				.map(events -> events.stream().map(SubscriberNotificationMapper::toEventModel).toList())
				.orElse(null));
	}

	private static SubscriberNotificationEvent toEventModel(final SubscriberNotificationEventEntity entity) {
		return SubscriberNotificationEvent.create()
			.withCreated(entity.getCreated())
			.withEventType(entity.getEventType())
			.withDescription(entity.getDescription())
			.withSubType(entity.getSubType());
	}
}
