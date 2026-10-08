package se.sundsvall.supportmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Subscribes the people tied to an errand once a change to it is committed.
 * <p>
 * Kept apart from {@link SubscriptionService} so that the assignee and the reporter are subscribed through its proxy,
 * each in a transaction of its own: a failure subscribing one then neither rolls back nor holds up the other. A failure
 * is logged and the errand goes without the subscription, as the change it followed has already been committed.
 */
@Component
class AutoSubscribeListener {

	private static final Logger LOG = LoggerFactory.getLogger(AutoSubscribeListener.class);

	private final SubscriptionService subscriptionService;

	AutoSubscribeListener(final SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void handleAutoSubscribeEvent(final AutoSubscribeEvent event) {
		final var errand = event.errandEntity();
		try {
			subscriptionService.autoSubscribeErrandAssignee(errand);
		} catch (final Exception e) {
			LOG.warn("Auto-subscribe failed for errand '{}' – continuing without subscription", errand.getId(), e);
		}
		if (event.errandCreated()) {
			try {
				subscriptionService.autoSubscribeReporter(errand);
			} catch (final Exception e) {
				LOG.warn("Auto-subscribe of reporter failed for errand '{}' – continuing without subscription", errand.getId(), e);
			}
		}
	}
}
