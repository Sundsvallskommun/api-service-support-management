package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;
import se.sundsvall.supportmanagement.service.scheduler.emaildispatch.SubscriberEmailService;

@Component
public class NotificationChannelDispatcher {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationChannelDispatcher.class);

	private final SubscriberNotificationService subscriberNotificationService;
	private final SubscriberEmailService subscriberEmailService;

	public NotificationChannelDispatcher(final SubscriberNotificationService subscriberNotificationService, final SubscriberEmailService subscriberEmailService) {
		this.subscriberNotificationService = subscriberNotificationService;
		this.subscriberEmailService = subscriberEmailService;
	}

	/**
	 * Delivers the events a subscriber should be notified about once per type of channel the subscriber has, so a
	 * subscriber holding several channels of one type is not notified several times over.
	 * <p>
	 * Failures are propagated so the caller can roll back and reschedule the whole group, rather than leaving some
	 * subscribers notified and others not.
	 */
	public void send(final String errandId, final String errandNumber, final SubscriberEntity subscriber, final List<NotificationDispatchEntity> events) {
		subscriber.getChannels().stream()
			.map(NotificationChannelEmbeddable::getType)
			.distinct()
			.forEach(type -> {
				switch (type) {
					case INTERNAL -> createInternalNotification(errandId, errandNumber, subscriber, events);
					case EMAIL -> subscriberEmailService.enqueue(errandId, errandNumber, subscriber, events);
					case SMS -> LOG.warn("Channel type: {} is not yet implemented, skipping delivery for errand: {} subscriber: {}", type, errandId, subscriber.getId());
				}
			});
	}

	private void createInternalNotification(final String errandId, final String errandNumber, final SubscriberEntity subscriber, final List<NotificationDispatchEntity> events) {
		final var internalEvents = events.stream()
			.filter(event -> !event.isEmailOnly())
			.toList();
		if (!internalEvents.isEmpty()) {
			subscriberNotificationService.create(errandId, errandNumber, subscriber, internalEvents);
		}
	}
}
