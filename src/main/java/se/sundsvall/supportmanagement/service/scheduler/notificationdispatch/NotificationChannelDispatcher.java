package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;
import se.sundsvall.supportmanagement.service.scheduler.emaildispatch.SubscriberEmailService;
import se.sundsvall.supportmanagement.service.scheduler.smsdispatch.SubscriberSmsService;

@Component
public class NotificationChannelDispatcher {

	private final SubscriberNotificationService subscriberNotificationService;
	private final SubscriberEmailService subscriberEmailService;
	private final SubscriberSmsService subscriberSmsService;

	public NotificationChannelDispatcher(final SubscriberNotificationService subscriberNotificationService, final SubscriberEmailService subscriberEmailService, final SubscriberSmsService subscriberSmsService) {
		this.subscriberNotificationService = subscriberNotificationService;
		this.subscriberEmailService = subscriberEmailService;
		this.subscriberSmsService = subscriberSmsService;
	}

	/**
	 * Delivers the events routed to each type of channel once, with the events meant for that channel. The routing is
	 * decided by the caller, which is what lets one subscriber get some events by email and others only internally.
	 * <p>
	 * Failures are propagated so the caller can roll back and reschedule the whole group.
	 */
	public void send(final String errandId, final String errandNumber, final SubscriberEntity subscriber, final Map<NotificationChannelType, List<NotificationDispatchEntity>> deliveries) {
		deliveries.forEach((type, events) -> {
			switch (type) {
				case INTERNAL -> subscriberNotificationService.create(errandId, errandNumber, subscriber, events);
				case EMAIL -> subscriberEmailService.enqueue(errandId, errandNumber, subscriber, events);
				case SMS -> subscriberSmsService.enqueue(errandId, errandNumber, subscriber, events);
			}
		});
	}
}
