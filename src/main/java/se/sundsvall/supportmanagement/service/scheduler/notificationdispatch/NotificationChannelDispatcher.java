package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;

@Component
public class NotificationChannelDispatcher {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationChannelDispatcher.class);

	private final SubscriberNotificationService subscriberNotificationService;
	private final TransactionTemplate requiresNewTemplate;

	public NotificationChannelDispatcher(final SubscriberNotificationService subscriberNotificationService, final PlatformTransactionManager transactionManager) {
		this.subscriberNotificationService = subscriberNotificationService;
		this.requiresNewTemplate = new TransactionTemplate(transactionManager);
		this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public boolean send(final String errandId, final String errandNumber, final SubscriberEntity subscriber) {
		var allSucceeded = true;
		for (final var channel : subscriber.getChannels()) {
			try {
				requiresNewTemplate.executeWithoutResult(status -> {
					switch (channel.getType()) {
						case INTERNAL -> subscriberNotificationService.upsert(errandId, errandNumber, subscriber);
						case SMS, EMAIL -> throw new UnsupportedOperationException("Channel type %s is not yet implemented".formatted(channel.getType()));
					}
				});
			} catch (final Exception e) {
				LOG.warn("Failed to send notification for errand: {} subscriber: {} channel: {}", errandId, subscriber.getId(), channel.getType(), e);
				allSucceeded = false;
			}
		}
		return allSucceeded;
	}
}
