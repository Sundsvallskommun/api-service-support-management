package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriberRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static java.time.OffsetDateTime.now;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Component
public class NotificationDispatchWorker {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationDispatchWorker.class);
	private static final long TRANSACTION_BUFFER_SECONDS = 10;

	@Value("${scheduler.notification-dispatch.max-retries:3}")
	private int maxRetries = 3;

	@Value("${scheduler.notification-dispatch.dead-letter-retention-days:7}")
	private int deadLetterRetentionDays = 7;

	private final NotificationDispatchRepository dispatchRepository;
	private final SubscriberRepository subscriberRepository;
	private final ErrandsRepository errandsRepository;
	private final NotificationChannelDispatcher channelDispatcher;

	public NotificationDispatchWorker(
		final NotificationDispatchRepository dispatchRepository,
		final SubscriberRepository subscriberRepository,
		final ErrandsRepository errandsRepository,
		final NotificationChannelDispatcher channelDispatcher) {
		this.dispatchRepository = dispatchRepository;
		this.subscriberRepository = subscriberRepository;
		this.errandsRepository = errandsRepository;
		this.channelDispatcher = channelDispatcher;
	}

	@Transactional
	public void cleanUpDeadLetters() {
		dispatchRepository.deleteByDeadLetterTrueAndCreatedBefore(now().minusDays(deadLetterRetentionDays));
	}

	@Transactional(readOnly = true)
	public List<NotificationDispatchEntity> fetchProcessable() {
		final var cutoff = now().minusSeconds(TRANSACTION_BUFFER_SECONDS);
		return dispatchRepository.findProcessable(cutoff, now());
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void processGroup(final List<NotificationDispatchEntity> group) {
		final var first = group.getFirst();
		final var errandId = first.getErrandId();
		final var municipalityId = first.getMunicipalityId();
		final var namespace = first.getNamespace();

		final var errandNumber = errandsRepository.findById(errandId)
			.map(ErrandEntity::getErrandNumber)
			.orElse(null);

		// TODO Subscriber notifications are reworked in a separate PR. Three things are decided and belong here:
		//
		// 1. Honour the subscription. This loads every subscriber of the namespace and filters only on event type and
		// executing user, so a subscriber is notified about errands they never subscribed to. SubscriptionRepository is
		// not consulted anywhere in this package. The target of the subscription (a single errand, or the namespace as a
		// whole) has to decide who is in scope before anything else is considered.
		//
		// 2. Filter on access when the notification is created, not when it is read. A subscriber who no longer reaches
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
		// 3. Notifications already created are NOT retracted. One created before the errand became unreachable stays in
		// the subscriber's list and stays readable, because the subscriber did have access at the time it was created -
		// it tells them nothing they were not already entitled to know. Losing access stops new notifications, it does
		// not rewrite history.
		final var subscribers = subscriberRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		var allSucceeded = true;

		for (final var subscriber : subscribers) {
			final var relevantEntries = group.stream()
				.filter(e -> !isExecutingUser(subscriber, e))
				.filter(e -> subscriberWantsEventType(subscriber, e))
				.toList();

			if (relevantEntries.isEmpty()) {
				continue;
			}

			if (!channelDispatcher.send(errandId, errandNumber, subscriber)) {
				allSucceeded = false;
			}
		}

		if (allSucceeded) {
			dispatchRepository.deleteAll(group);
		} else {
			group.forEach(this::handleFailure);
		}
	}

	private void handleFailure(final NotificationDispatchEntity entry) {
		entry.setRetryCount(entry.getRetryCount() + 1);
		if (entry.getRetryCount() >= maxRetries) {
			LOG.error("Notification dispatch id: {} has reached max retries, marking as dead-letter", entry.getId());
			entry.setDeadLetter(true);
		} else {
			final long delayMinutes = (long) Math.pow(2, entry.getRetryCount() - 1.0);
			entry.setNextRetryAt(now().plusMinutes(delayMinutes));
			LOG.info("Notification dispatch id: {} scheduled for retry in {} minute(s)", entry.getId(), delayMinutes);
		}
		dispatchRepository.save(entry);
	}

	private boolean isExecutingUser(final SubscriberEntity subscriber, final NotificationDispatchEntity entry) {
		return entry.getExecutingUserId() != null
			&& subscriber.getIdentifier() != null
			&& entry.getExecutingUserId().equals(subscriber.getIdentifier().getValue());
	}

	private boolean subscriberWantsEventType(final SubscriberEntity subscriber, final NotificationDispatchEntity entry) {
		final var filters = subscriber.getEventFilters();
		if (filters == null || filters.isEmpty()) {
			return true;
		}
		return filters.stream().anyMatch(filter -> entry.getEventType().equals(filter.getType()));
	}
}
