package se.sundsvall.supportmanagement.service.mapper;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import se.sundsvall.supportmanagement.api.model.notification.SubscriberNotification;
import se.sundsvall.supportmanagement.api.model.notification.SubscriberNotificationEvent;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEventEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static java.time.OffsetDateTime.now;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;

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
			.withExpires(now(ZoneId.systemDefault()).plusDays(notificationTTLInDays))
			.withEvents(events);
	}

	/**
	 * Returns a mutable list, since it is handed to a {@code @OneToMany} collection that Hibernate manages.
	 */
	public static List<SubscriberNotificationEventEntity> toEventEntities(final List<NotificationDispatchEntity> events) {
		return ofNullable(events).orElseGet(List::of).stream()
			.map(SubscriberNotificationMapper::toEventEntity)
			.collect(toCollection(ArrayList::new));
	}

	public static SubscriberNotificationEventEntity toEventEntity(final NotificationDispatchEntity event) {
		return SubscriberNotificationEventEntity.create()
			.withEventType(event.getEventType())
			.withDescription(event.getDescription())
			.withSubType(event.getSubType());
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
