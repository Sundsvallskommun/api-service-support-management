package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
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
	 * Delivers the events a subscriber should be notified about on each of the subscriber's channels.
	 * <p>
	 * Failures are propagated so the caller can roll back and reschedule the whole group, rather than leaving some
	 * subscribers notified and others not.
	 */
	public void send(final String errandId, final String errandNumber, final SubscriberEntity subscriber, final List<NotificationDispatchEntity> events) {
		for (final var channel : subscriber.getChannels()) {
			switch (channel.getType()) {
				case INTERNAL -> subscriberNotificationService.create(errandId, errandNumber, subscriber, events);
				case EMAIL -> subscriberEmailService.enqueue(errandId, errandNumber, channel, subscriber, events);
				case SMS -> LOG.warn("Channel type: {} is not yet implemented, skipping delivery for errand: {} subscriber: {}", channel.getType(), errandId, subscriber.getId());
			}
		}
	}
}
