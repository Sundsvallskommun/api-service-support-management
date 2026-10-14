package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.service.scheduler.emaildispatch.SubscriberEmailService;
import se.sundsvall.supportmanagement.service.scheduler.smsdispatch.SubscriberSmsService;

@Service
public class NotificationDispatchScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationDispatchScheduler.class);

	private final NotificationDispatchWorker worker;
	private final SubscriberEmailService subscriberEmailService;
	private final SubscriberSmsService subscriberSmsService;
	private final Dept44HealthUtility healthUtility;

	@Value("${scheduler.notification-dispatch.name}")
	private String jobName;

	public NotificationDispatchScheduler(final NotificationDispatchWorker worker, final SubscriberEmailService subscriberEmailService, final SubscriberSmsService subscriberSmsService, final Dept44HealthUtility healthUtility) {
		this.worker = worker;
		this.subscriberEmailService = subscriberEmailService;
		this.subscriberSmsService = subscriberSmsService;
		this.healthUtility = healthUtility;
	}

	@Dept44Scheduled(
		cron = "${scheduler.notification-dispatch.cron}",
		name = "${scheduler.notification-dispatch.name}",
		lockAtMostFor = "${scheduler.notification-dispatch.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.notification-dispatch.maximum-execution-time}")
	public void processDispatch() {
		// Grouped per errand, so a subscriber gets a single notification covering everything that happened to it.
		// A fetch failure is deliberately left to bubble up to the Dept44Scheduled aspect.
		final var groups = worker.fetchProcessable().stream()
			.collect(Collectors.groupingBy(NotificationDispatchEntity::getErrandId));

		// A failing errand is rolled back in its entirety and left in place for the next run, without holding up the rest
		groups.forEach((errandId, group) -> {
			try {
				worker.processGroup(group);
			} catch (final Exception e) {
				LOG.error("Error processing notification dispatch for errand: {}", errandId, e);
				healthUtility.setHealthIndicatorUnhealthy(jobName, "Error processing notification dispatch: " + e.getMessage());
			}
		});

		// Emptied last, so what this run enqueued goes out now along with anything left over from failed runs
		subscriberEmailService.findPendingSubscriberIds().forEach(subscriberId -> {
			try {
				subscriberEmailService.sendPending(subscriberId);
			} catch (final Exception e) {
				LOG.error("Error sending email dispatch to subscriber: {}", subscriberId, e);
				healthUtility.setHealthIndicatorUnhealthy(jobName, "Error sending email dispatch: " + e.getMessage());
			}
		});

		subscriberSmsService.findPendingSubscriberIds().forEach(subscriberId -> {
			try {
				subscriberSmsService.sendPending(subscriberId);
			} catch (final Exception e) {
				LOG.error("Error sending sms dispatch to subscriber: {}", subscriberId, e);
				healthUtility.setHealthIndicatorUnhealthy(jobName, "Error sending sms dispatch: " + e.getMessage());
			}
		});
	}
}
