package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;

@Component
public class NotificationChannelDispatcher {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationChannelDispatcher.class);

	private final SubscriberNotificationService subscriberNotificationService;

	public NotificationChannelDispatcher(final SubscriberNotificationService subscriberNotificationService) {
		this.subscriberNotificationService = subscriberNotificationService;
	}

	public void send(final String errandId, final String errandNumber, final SubscriberEntity subscriber, final NotificationChannelEmbeddable channel) {
		switch (channel.getType()) {
			case INTERNAL -> subscriberNotificationService.upsert(errandId, errandNumber, subscriber);
			case SMS, EMAIL -> LOG.warn("Channel type {} is not yet implemented", channel.getType());
		}
	}
}
