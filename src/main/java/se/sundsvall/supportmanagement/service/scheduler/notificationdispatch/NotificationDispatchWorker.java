package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;
import se.sundsvall.supportmanagement.service.AccessControlService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static java.time.OffsetDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withId;

@Component
public class NotificationDispatchWorker {

	/**
	 * How long a request group must be quiet before it is considered complete and eligible for dispatch.
	 */
	@Value("${scheduler.notification-dispatch.transaction-buffer:PT10S}")
	private Duration transactionBuffer = Duration.ofSeconds(10);

	/**
	 * How old an entry may get before it is considered too stale to notify about. Since a failed dispatch is retried
	 * indefinitely, this is what stops an entry that can never succeed from being sent long after the fact.
	 */
	@Value("${scheduler.notification-dispatch.max-age:P30D}")
	private Duration maxAge = Duration.ofDays(30);

	private final NotificationDispatchRepository dispatchRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final ErrandsRepository errandsRepository;
	private final NotificationChannelDispatcher channelDispatcher;
	private final AccessControlService accessControlService;

	public NotificationDispatchWorker(
		final NotificationDispatchRepository dispatchRepository,
		final SubscriptionRepository subscriptionRepository,
		final ErrandsRepository errandsRepository,
		final NotificationChannelDispatcher channelDispatcher,
		final AccessControlService accessControlService) {
		this.dispatchRepository = dispatchRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.errandsRepository = errandsRepository;
		this.channelDispatcher = channelDispatcher;
		this.accessControlService = accessControlService;
	}

	@Transactional(readOnly = true)
	public List<NotificationDispatchEntity> fetchProcessable() {
		return dispatchRepository.findProcessable(now(ZoneId.systemDefault()).minus(transactionBuffer));
	}

	/**
	 * Dispatches one group of entries, all belonging to the same errand.
	 * <p>
	 * Every subscriber matched by an active subscription receives at most one delivery per channel and group, carrying
	 * the subset of the group's events that the subscriber's subscriptions route to that channel.
	 * <p>
	 * Deleting the group here is what marks it as done: delivery and deletion share one transaction, so a failure
	 * anywhere rolls back every delivery and leaves the whole group in place, which is what makes the next scheduler run
	 * pick it up again. An entry therefore survives until it has been dispatched successfully, or until it ages past
	 * {@code maxAge} and is dropped undelivered.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	public void processGroup(final List<NotificationDispatchEntity> group) {
		final var first = group.getFirst();
		final var errandId = first.getErrandId();

		final var errandNumber = errandsRepository.findById(errandId)
			.map(ErrandEntity::getErrandNumber)
			.orElse(null);

		subscriptionRepository.findAllActiveForErrand(first.getMunicipalityId(), first.getNamespace(), errandId, now(ZoneId.systemDefault()))
			.stream()
			// A subscriber may cover the same errand through both a NAMESPACE and an ERRAND subscription
			.collect(groupingBy(subscription -> subscription.getSubscriber().getId(), LinkedHashMap::new, toList()))
			.values()
			.forEach(subscriptions -> dispatch(errandId, errandNumber, group, subscriptions));

		dispatchRepository.deleteAll(group);
	}

	private void dispatch(final String errandId, final String errandNumber, final List<NotificationDispatchEntity> group, final List<SubscriptionEntity> subscriptions) {
		final var subscriber = subscriptions.getFirst().getSubscriber();

		// A subscription outlives access to the errand. If the subscriber can no longer reach it, because its labels
		// changed, no notification is created - the subscription is left alone, so notifications resume by themselves
		// once the errand is reachable again. Already delivered notifications are deliberately not retracted: the
		// subscriber had access when they were created, so they tell them nothing they were not entitled to know.
		if (!mayReachErrand(errandId, subscriber)) {
			return;
		}

		// Each event goes to the union of the channels of every subscription that wants it, so an event reached through
		// several subscriptions or profiles is still delivered only once per channel
		final var deliveries = new EnumMap<NotificationChannelType, List<NotificationDispatchEntity>>(NotificationChannelType.class);
		group.stream()
			.filter(this::isWithinMaxAge)
			.filter(entry -> !isExecutingUser(subscriber, entry))
			.forEach(entry -> subscriptions.stream()
				.flatMap(subscription -> channelsFor(subscription, entry).stream())
				.distinct()
				.forEach(channel -> deliveries.computeIfAbsent(channel, _ -> new ArrayList<>()).add(entry)));

		if (!deliveries.isEmpty()) {
			channelDispatcher.send(errandId, errandNumber, subscriber, deliveries);
		}
	}

	/**
	 * The channels a subscription delivers the event on. A subscription to a profile is governed by the profile - its
	 * filters select the events and its channels carry them, unless it has none and leaves that to the subscriber's own
	 * channels. Any other subscription delivers what its filters accept on the subscriber's own channels.
	 */
	private List<NotificationChannelType> channelsFor(final SubscriptionEntity subscription, final NotificationDispatchEntity entry) {
		final var profile = subscription.getProfile();
		if (profile != null) {
			return matchesAny(profile.getEventFilters(), entry) ? channelsOf(profile, subscription.getSubscriber()) : emptyList();
		}
		return wantsEvent(subscription, entry) ? channelsOf(subscription.getSubscriber()) : emptyList();
	}

	private static List<NotificationChannelType> channelsOf(final SubscriptionProfileEntity profile, final SubscriberEntity subscriber) {
		return ofNullable(profile.getChannels())
			.filter(channels -> !channels.isEmpty())
			.orElseGet(() -> channelsOf(subscriber));
	}

	private static List<NotificationChannelType> channelsOf(final SubscriberEntity subscriber) {
		return ofNullable(subscriber.getChannels()).orElse(emptyList()).stream()
			.map(NotificationChannelEmbeddable::getType)
			.filter(Objects::nonNull)
			.toList();
	}

	/**
	 * Signals whether the subscriber may still reach the errand. Evaluated as the subscriber rather than as a caller,
	 * since this job runs without an Identifier of its own - which is why the access control specification takes the
	 * user explicitly. A subscriber whose identifier cannot be resolved reaches nothing.
	 */
	private boolean mayReachErrand(final String errandId, final SubscriberEntity subscriber) {
		return errandsRepository.findOne(withId(errandId)
			.and(accessControlService.withAccessControl(subscriber.getNamespace(), subscriber.getMunicipalityId(), toIdentifier(subscriber), ProtectedResource.NOTIFICATION, LR)))
			.isPresent();
	}

	private static Identifier toIdentifier(final SubscriberEntity subscriber) {
		return ofNullable(subscriber.getIdentifier())
			.map(owner -> {
				final var type = Identifier.Type.fromString(owner.getType());
				return isNull(type) ? null : Identifier.create().withType(type).withValue(owner.getValue());
			})
			.orElse(null);
	}

	/**
	 * Stale entries are left out of the delivery but still deleted along with the rest of the group.
	 */
	private boolean isWithinMaxAge(final NotificationDispatchEntity entry) {
		return entry.getCreated() == null || entry.getCreated().isAfter(now(ZoneId.systemDefault()).minus(maxAge));
	}

	private boolean isExecutingUser(final SubscriberEntity subscriber, final NotificationDispatchEntity entry) {
		return entry.getExecutingUserId() != null
			&& subscriber.getIdentifier() != null
			&& entry.getExecutingUserId().equals(subscriber.getIdentifier().getValue());
	}

	/**
	 * Subscription level filters override the subscriber's global ones, as documented on the subscription API model. No
	 * filters at either level means everything is wanted.
	 */
	private boolean wantsEvent(final SubscriptionEntity subscription, final NotificationDispatchEntity entry) {
		var filters = subscription.getEventFilters();
		if (filters == null || filters.isEmpty()) {
			filters = subscription.getSubscriber().getEventFilters();
		}
		return filters == null || filters.isEmpty() || matchesAny(filters, entry);
	}

	private boolean matchesAny(final List<EventFilterEmbeddable> filters, final NotificationDispatchEntity entry) {
		return ofNullable(filters).orElse(emptyList()).stream().anyMatch(filter -> matches(filter, entry));
	}

	private boolean matches(final EventFilterEmbeddable filter, final NotificationDispatchEntity entry) {
		return Objects.equals(filter.getType(), entry.getEventType())
			&& (filter.getSubtype() == null || Objects.equals(filter.getSubtype(), entry.getSubType()))
			&& (filter.getLabelId() == null || ofNullable(entry.getAddedLabelIds()).orElse(emptySet()).contains(filter.getLabelId()));
	}
}
