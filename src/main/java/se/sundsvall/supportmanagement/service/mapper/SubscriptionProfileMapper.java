package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionProfile;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;

import static java.util.stream.Collectors.toCollection;

/**
 * Maps between {@link SubscriptionProfile} DTOs and {@link SubscriptionProfileEntity}.
 * <p>
 * As in {@link SubscriberMapper}, collections headed for the entity are mutable ArrayLists, since Hibernate manages
 * {@code @ElementCollection @OrderColumn} lists in place when flushing an update.
 */
public final class SubscriptionProfileMapper {

	private SubscriptionProfileMapper() {
		// Intentionally empty
	}

	public static SubscriptionProfileEntity toSubscriptionProfileEntity(final String municipalityId, final String namespace, final SubscriptionProfile profile) {
		return Optional.ofNullable(profile)
			.map(dto -> SubscriptionProfileEntity.create()
				.withMunicipalityId(municipalityId)
				.withNamespace(namespace)
				.withName(dto.getName())
				.withDescription(dto.getDescription())
				.withEventFilters(SubscriberMapper.toEventFilterEmbeddables(dto.getEventFilters()))
				.withChannels(toChannelTypeEntities(dto.getChannels())))
			.orElse(null);
	}

	public static void updateEntity(final SubscriptionProfileEntity entity, final SubscriptionProfile patch) {
		if (entity == null || patch == null) {
			return;
		}
		Optional.ofNullable(patch.getName()).ifPresent(entity::setName);
		Optional.ofNullable(patch.getDescription()).ifPresent(entity::setDescription);
		Optional.ofNullable(patch.getEventFilters()).map(SubscriberMapper::toEventFilterEmbeddables).ifPresent(entity::setEventFilters);
		Optional.ofNullable(patch.getChannels()).map(SubscriptionProfileMapper::toChannelTypeEntities).ifPresent(entity::setChannels);
	}

	public static SubscriptionProfile toSubscriptionProfile(final SubscriptionProfileEntity entity) {
		return Optional.ofNullable(entity)
			.map(e -> SubscriptionProfile.create()
				.withId(e.getId())
				.withName(e.getName())
				.withDescription(e.getDescription())
				.withEventFilters(SubscriberMapper.toEventFilters(e.getEventFilters()))
				.withChannels(toChannelTypes(e.getChannels()))
				.withCreated(e.getCreated())
				.withModified(e.getModified()))
			.orElse(null);
	}

	public static List<SubscriptionProfile> toSubscriptionProfileList(final List<SubscriptionProfileEntity> entities) {
		return Optional.ofNullable(entities)
			.map(list -> list.stream()
				.map(SubscriptionProfileMapper::toSubscriptionProfile)
				.toList())
			.orElse(List.of());
	}

	static List<se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType> toChannelTypeEntities(final List<NotificationChannelType> types) {
		return Optional.ofNullable(types)
			.map(list -> list.stream()
				.map(SubscriberMapper::toDbChannelType)
				.collect(toCollection(ArrayList::new)))
			.orElse(null);
	}

	static List<NotificationChannelType> toChannelTypes(final List<se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType> types) {
		return Optional.ofNullable(types)
			.map(list -> list.stream()
				.map(SubscriberMapper::toApiChannelType)
				.toList())
			.orElse(null);
	}
}
