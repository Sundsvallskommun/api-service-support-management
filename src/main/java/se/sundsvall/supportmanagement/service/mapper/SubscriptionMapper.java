package se.sundsvall.supportmanagement.service.mapper;

import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.subscription.Subscription;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTarget;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;

public final class SubscriptionMapper {

	private SubscriptionMapper() {
		// Intentionally empty
	}

	public static SubscriptionEntity toSubscriptionEntity(final SubscriberEntity subscriber, final ErrandEntity errand, final Subscription subscription) {
		return Optional.ofNullable(subscription)
			.map(dto -> SubscriptionEntity.create()
				.withSubscriber(subscriber)
				.withTargetType(toDbTargetType(targetTypeOf(dto)))
				.withErrand(errand)
				.withEventFilters(SubscriberMapper.toEventFilterEmbeddables(dto.getEventFilters()))
				.withExpiresAt(dto.getExpiresAt()))
			.orElse(null);
	}

	public static Subscription toSubscription(final SubscriptionEntity entity) {
		return Optional.ofNullable(entity)
			.map(e -> Subscription.create()
				.withId(e.getId())
				.withTarget(toTarget(e))
				.withEventFilters(SubscriberMapper.toEventFilters(e.getEventFilters()))
				.withExpiresAt(e.getExpiresAt())
				.withCreated(e.getCreated())
				.withCreatedBy(SubscriberMapper.toIdentifier(e.getCreatedBy())))
			.orElse(null);
	}

	public static se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionTargetType toDbTargetType(final SubscriptionTargetType apiType) {
		return Optional.ofNullable(apiType)
			.map(t -> se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionTargetType.valueOf(t.name()))
			.orElse(null);
	}

	static SubscriptionTargetType toApiTargetType(final se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionTargetType dbType) {
		return Optional.ofNullable(dbType)
			.map(t -> SubscriptionTargetType.valueOf(t.name()))
			.orElse(null);
	}

	static SubscriptionTarget toTarget(final SubscriptionEntity entity) {
		return Optional.ofNullable(entity.getTargetType())
			.map(type -> SubscriptionTarget.create()
				.withType(toApiTargetType(type))
				.withId(Optional.ofNullable(entity.getErrand()).map(ErrandEntity::getId).orElse(null)))
			.orElse(null);
	}

	private static SubscriptionTargetType targetTypeOf(final Subscription subscription) {
		return Optional.ofNullable(subscription.getTarget()).map(SubscriptionTarget::getType).orElse(null);
	}
}
