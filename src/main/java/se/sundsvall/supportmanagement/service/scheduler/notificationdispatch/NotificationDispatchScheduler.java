package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;

@Service
public class NotificationDispatchScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationDispatchScheduler.class);

	private final NotificationDispatchWorker worker;
	private final Dept44HealthUtility healthUtility;

	@Value("${scheduler.notification-dispatch.name}")
	private String jobName;

	public NotificationDispatchScheduler(final NotificationDispatchWorker worker, final Dept44HealthUtility healthUtility) {
		this.worker = worker;
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
	}
}
