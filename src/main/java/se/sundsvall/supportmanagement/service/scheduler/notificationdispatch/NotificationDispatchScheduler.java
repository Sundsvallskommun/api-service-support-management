package se.sundsvall.supportmanagement.service.scheduler.notificationdispatch;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

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
		healthUtility.setHealthIndicatorHealthy(jobName);

		try {
			final var groups = worker.fetchProcessable().stream()
				.collect(Collectors.groupingBy(e -> e.getErrandId()));

			groups.forEach((errandId, group) -> {
				try {
					worker.processGroup(group);
				} catch (final Exception e) {
					LOG.error("Error processing notification dispatch for errand: {}", errandId, e);
					healthUtility.setHealthIndicatorUnhealthy(jobName, "Error processing notification dispatch: " + e.getMessage());
				}
			});

			worker.cleanUpDeadLetters();
		} catch (final Exception e) {
			LOG.error("Error fetching processable notification dispatches", e);
			healthUtility.setHealthIndicatorUnhealthy(jobName, "Error fetching processable notification dispatches: " + e.getMessage());
		}
	}
}
