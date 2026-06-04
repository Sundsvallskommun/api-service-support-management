package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;

@Component
public class NotificationChannelDispatcher {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationChannelDispatcher.class);

	private final SubscriberNotificationService subscriberNotificationService;

	public NotificationChannelDispatcher(final SubscriberNotificationService subscriberNotificationService) {
		this.subscriberNotificationService = subscriberNotificationService;
	}

	public boolean send(final String errandId, final String errandNumber, final SubscriberEntity subscriber) {
		var allSucceeded = true;
		for (final var channel : subscriber.getChannels()) {
			try {
				switch (channel.getType()) {
					case INTERNAL -> subscriberNotificationService.upsert(errandId, errandNumber, subscriber);
					case SMS, EMAIL -> throw new UnsupportedOperationException("Channel type %s is not yet implemented".formatted(channel.getType()));
				}
			} catch (final Exception e) {
				LOG.warn("Failed to send notification for errand: {} subscriber: {} channel: {}", errandId, subscriber.getId(), channel.getType(), e);
				allSucceeded = false;
			}
		}
		return allSucceeded;
	}
}
