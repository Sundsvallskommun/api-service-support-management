package se.sundsvall.supportmanagement.service.scheduler.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

@Service
public class ActionScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(ActionScheduler.class);

	private final ActionWorker actionWorker;
	private final Dept44HealthUtility healthUtility;

	@Value("${scheduler.action.name}")
	private String jobName;

	public ActionScheduler(final ActionWorker actionWorker, final Dept44HealthUtility healthUtility) {
		this.actionWorker = actionWorker;
		this.healthUtility = healthUtility;
	}

	@Dept44Scheduled(
		cron = "${scheduler.action.cron}",
		name = "${scheduler.action.name}",
		lockAtMostFor = "${scheduler.action.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.action.maximum-execution-time}")
	public void processActions() {
		actionWorker.getExpiredActions().forEach(action -> {
			try {
				actionWorker.processAction(action);
			} catch (final Exception e) {
				LOG.error("Error processing action with id: {}", action.getId(), e);
				healthUtility.setHealthIndicatorUnhealthy(jobName, "Error processing action: " + e.getMessage());
			}
		});
	}
}
