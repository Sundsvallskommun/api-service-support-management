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

			for (final var channel : subscriber.getChannels()) {
				try {
					channelDispatcher.send(errandId, errandNumber, subscriber, channel);
				} catch (final Exception e) {
					LOG.warn("Failed to send notification for errand: {} subscriber: {} channel: {}", errandId, subscriber.getId(), channel.getType(), e);
					allSucceeded = false;
				}
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
