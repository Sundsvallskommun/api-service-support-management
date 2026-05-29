package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannel;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;
import se.sundsvall.supportmanagement.api.model.subscriber.Subscriber;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static java.util.stream.Collectors.toCollection;

/**
 * Maps between {@link Subscriber} DTOs and {@link SubscriberEntity}.
 *
 * <p>
 * Collection-returning methods that produce entity embeddables ({@link #toChannelEmbeddables},
 * {@link #toEventFilterEmbeddables}) return <strong>mutable</strong> ArrayLists. This is required
 * so Hibernate can manage {@code @ElementCollection @OrderColumn} lists during update flush —
 * {@code Stream.toList()} returns an immutable list which would cause
 * {@code UnsupportedOperationException}, translated by dept44 to {@code 501 NOT_IMPLEMENTED}.
 * See {@code SubscribersIT.test07_updateSubscriber} for regression coverage.
 */
public final class SubscriberMapper {

	private SubscriberMapper() {
		// Intentionally empty
	}

	public static SubscriberEntity toSubscriberEntity(final String municipalityId, final String namespace, final Subscriber subscriber) {
		return Optional.ofNullable(subscriber)
			.map(dto -> SubscriberEntity.create()
				.withMunicipalityId(municipalityId)
				.withNamespace(namespace)
				.withName(dto.getName())
				.withIdentifier(IdentifierEmbeddableMapper.toIdentifierEmbeddable(dto.getIdentifier()))
				.withChannels(toChannelEmbeddables(dto.getChannels()))
				.withEventFilters(toEventFilterEmbeddables(dto.getEventFilters()))
				.withPausedFrom(dto.getPausedFrom())
				.withPausedUntil(dto.getPausedUntil()))
			.orElse(null);
	}

	public static void applyPatch(final SubscriberEntity entity, final Subscriber patch) {
		if (entity == null || patch == null) {
			return;
		}
		Optional.ofNullable(patch.getName()).ifPresent(entity::setName);
		Optional.ofNullable(patch.getChannels()).map(SubscriberMapper::toChannelEmbeddables).ifPresent(entity::setChannels);
		Optional.ofNullable(patch.getEventFilters()).map(SubscriberMapper::toEventFilterEmbeddables).ifPresent(entity::setEventFilters);
		Optional.ofNullable(patch.getPausedFrom()).ifPresent(entity::setPausedFrom);
		Optional.ofNullable(patch.getPausedUntil()).ifPresent(entity::setPausedUntil);
	}

	public static Subscriber toSubscriber(final SubscriberEntity entity, final Long subscriptionCount) {
		return Optional.ofNullable(entity)
			.map(e -> Subscriber.create()
				.withId(e.getId())
				.withName(e.getName())
				.withIdentifier(IdentifierEmbeddableMapper.toIdentifier(e.getIdentifier()))
				.withChannels(toChannels(e.getChannels()))
				.withEventFilters(toEventFilters(e.getEventFilters()))
				.withPausedFrom(e.getPausedFrom())
				.withPausedUntil(e.getPausedUntil())
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withCreatedBy(IdentifierEmbeddableMapper.toIdentifier(e.getCreatedBy()))
				.withSubscriptionCount(subscriptionCount == null ? null : subscriptionCount.intValue()))
			.orElse(null);
	}

	static List<NotificationChannel> toChannels(final List<NotificationChannelEmbeddable> embeddables) {
		return Optional.ofNullable(embeddables)
			.map(list -> list.stream()
				.map(SubscriberMapper::toChannel)
				.toList())
			.orElse(null);
	}

	static NotificationChannel toChannel(final NotificationChannelEmbeddable embeddable) {
		return Optional.ofNullable(embeddable)
			.map(e -> NotificationChannel.create()
				.withType(toApiChannelType(e.getType()))
				.withDestination(e.getDestination()))
			.orElse(null);
	}

	static List<NotificationChannelEmbeddable> toChannelEmbeddables(final List<NotificationChannel> dtos) {
		return Optional.ofNullable(dtos)
			.map(list -> list.stream()
				.map(SubscriberMapper::toChannelEmbeddable)
				.collect(toCollection(ArrayList::new)))
			.orElse(null);
	}

	static NotificationChannelEmbeddable toChannelEmbeddable(final NotificationChannel dto) {
		return Optional.ofNullable(dto)
			.map(d -> NotificationChannelEmbeddable.create()
				.withType(toDbChannelType(d.getType()))
				.withDestination(d.getDestination()))
			.orElse(null);
	}

	static NotificationChannelType toApiChannelType(final se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType dbType) {
		return Optional.ofNullable(dbType)
			.map(t -> NotificationChannelType.valueOf(t.name()))
			.orElse(null);
	}

	static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType toDbChannelType(final NotificationChannelType apiType) {
		return Optional.ofNullable(apiType)
			.map(t -> se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.valueOf(t.name()))
			.orElse(null);
	}

	static List<EventFilter> toEventFilters(final List<EventFilterEmbeddable> embeddables) {
		return Optional.ofNullable(embeddables)
			.map(list -> list.stream()
				.map(SubscriberMapper::toEventFilter)
				.toList())
			.orElse(null);
	}

	static EventFilter toEventFilter(final EventFilterEmbeddable embeddable) {
		return Optional.ofNullable(embeddable)
			.map(e -> EventFilter.create()
				.withType(e.getType())
				.withSubtype(e.getSubtype()))
			.orElse(null);
	}

	static List<EventFilterEmbeddable> toEventFilterEmbeddables(final List<EventFilter> dtos) {
		return Optional.ofNullable(dtos)
			.map(list -> list.stream()
				.map(SubscriberMapper::toEventFilterEmbeddable)
				.collect(toCollection(ArrayList::new)))
			.orElse(null);
	}

	static EventFilterEmbeddable toEventFilterEmbeddable(final EventFilter dto) {
		return Optional.ofNullable(dto)
			.map(d -> EventFilterEmbeddable.create()
				.withType(d.getType())
				.withSubtype(d.getSubtype()))
			.orElse(null);
	}
}
