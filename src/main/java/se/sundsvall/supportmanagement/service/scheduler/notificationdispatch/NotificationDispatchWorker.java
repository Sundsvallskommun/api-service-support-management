package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.time.Duration;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;

import static java.time.OffsetDateTime.now;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

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

	public NotificationDispatchWorker(
		final NotificationDispatchRepository dispatchRepository,
		final SubscriptionRepository subscriptionRepository,
		final ErrandsRepository errandsRepository,
		final NotificationChannelDispatcher channelDispatcher) {
		this.dispatchRepository = dispatchRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.errandsRepository = errandsRepository;
		this.channelDispatcher = channelDispatcher;
	}

	@Transactional(readOnly = true)
	public List<NotificationDispatchEntity> fetchProcessable() {
		return dispatchRepository.findProcessable(now(ZoneId.systemDefault()).minus(transactionBuffer));
	}

	/**
	 * Dispatches one group of entries, all belonging to the same errand.
	 * <p>
	 * Every subscriber matched by an active subscription receives at most one delivery per group, carrying the subset of
	 * the group's events that the subscriber's filters accept.
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

		// TODO Subscriber notifications, remaining work. Honouring the subscription is now done, by the query below.
		// Two decisions are still outstanding:
		//
		// 1. Filter on access when the notification is created, not when it is read. A subscriber who no longer reaches
		// the errand, because its labels changed, gets no notification created for it. The subscription itself is left
		// untouched, so notifications resume by themselves if the errand later becomes reachable again - nothing has to
		// be repaired or re-subscribed. Creating the row and hiding it on read was rejected: the row carries the errand
		// id and number, so the association would already be stored and every future read path would have to remember to
		// filter it.
		//
		// The check has to be evaluated as the subscriber, not as the caller. This job runs without an Identifier, but
		// AccessControlService.withAccessControl takes the user explicitly, so one can be built per subscriber from
		// SubscriberEntity.getIdentifier(). Label lookups are cached per user and namespace, so the cost stays bounded.
		//
		// Note this makes the worker authorise, which is not what its entry in AccessControlChokePointTest.EXEMPT says.
		// The exemption still holds - it authorises on behalf of subscribers rather than a caller - but fix the wording.
		//
		// 2. Notifications already created are NOT retracted. One created before the errand became unreachable stays in
		// the subscriber's list and stays readable, because the subscriber did have access at the time it was created -
		// it tells them nothing they were not already entitled to know. Losing access stops new notifications, it does
		// not rewrite history.
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

		final var events = group.stream()
			.filter(this::isWithinMaxAge)
			.filter(entry -> !isExecutingUser(subscriber, entry))
			.filter(entry -> subscriptions.stream().anyMatch(subscription -> wantsEvent(subscription, entry)))
			.toList();

		if (!events.isEmpty()) {
			channelDispatcher.send(errandId, errandNumber, subscriber, events);
		}
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
		return filters == null || filters.isEmpty() || filters.stream().anyMatch(filter -> matches(filter, entry));
	}

	private boolean matches(final EventFilterEmbeddable filter, final NotificationDispatchEntity entry) {
		return Objects.equals(filter.getType(), entry.getEventType())
			&& (filter.getSubtype() == null || Objects.equals(filter.getSubtype(), entry.getSubType()));
	}
}
