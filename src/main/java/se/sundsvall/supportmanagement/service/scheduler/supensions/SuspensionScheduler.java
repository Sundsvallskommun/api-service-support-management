package se.sundsvall.supportmanagement.service.scheduler.supensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

@Service
public class SuspensionScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(SuspensionScheduler.class);

	private final SuspensionWorker suspensionWorker;
	private final Dept44HealthUtility healthUtility;

	private final String jobName;

	public SuspensionScheduler(final SuspensionWorker suspensionWorker, final Dept44HealthUtility healthUtility, @Value("${scheduler.suspension.name}") final String jobName) {
		this.suspensionWorker = suspensionWorker;
		this.healthUtility = healthUtility;
		this.jobName = jobName;
	}

	@Dept44Scheduled(
		cron = "${scheduler.suspension.cron}",
		name = "${scheduler.suspension.name}",
		lockAtMostFor = "${scheduler.suspension.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.suspension.maximum-execution-time}")
	void processExpiredSuspensions() {
		suspensionWorker.getErrandIdsWithEndedSuspension().forEach(errandId -> {
			try {
				suspensionWorker.announceSuspensionEnd(errandId);
			} catch (final Exception e) {
				LOG.error("Error announcing the end of the suspension of errand with id: {}", errandId, e);
				healthUtility.setHealthIndicatorUnhealthy(jobName, "Error announcing the end of a suspension: " + e.getMessage());
			}
		});
	}
}
