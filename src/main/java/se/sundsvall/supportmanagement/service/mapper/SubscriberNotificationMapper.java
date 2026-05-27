package se.sundsvall.supportmanagement.service.mapper;

import se.sundsvall.supportmanagement.api.model.notification.SubscriberNotification;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

public final class SubscriberNotificationMapper {

	private SubscriberNotificationMapper() {}

	public static SubscriberNotificationEntity toEntity(final String errandId, final String errandNumber, final SubscriberEntity subscriber) {
		return SubscriberNotificationEntity.create()
			.withMunicipalityId(subscriber.getMunicipalityId())
			.withNamespace(subscriber.getNamespace())
			.withIdentifierType(subscriber.getIdentifier().getType())
			.withIdentifierValue(subscriber.getIdentifier().getValue())
			.withErrandId(errandId)
			.withErrandNumber(errandNumber);
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
			.withAcknowledged(entity.getAcknowledged());
	}
}
