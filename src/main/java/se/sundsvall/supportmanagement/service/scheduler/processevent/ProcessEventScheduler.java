package se.sundsvall.supportmanagement.service.scheduler.processevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

@Service
public class ProcessEventScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessEventScheduler.class);

	private final ProcessEventRelay processEventRelay;
	private final ProcessEventCleanup processEventCleanup;
	private final Dept44HealthUtility healthUtility;

	@Value("${scheduler.process-event.name}")
	private String relayJobName;

	public ProcessEventScheduler(final ProcessEventRelay processEventRelay, final ProcessEventCleanup processEventCleanup, final Dept44HealthUtility healthUtility) {
		this.processEventRelay = processEventRelay;
		this.processEventCleanup = processEventCleanup;
		this.healthUtility = healthUtility;
	}

	/**
	 * The scheduled run of the relay, which delivers whatever the direct runs did not.
	 * <p>
	 * Health is decided afterwards, on what is still waiting, rather than on whether anything failed in the run. A pw-alkt
	 * restarting for a minute is no fault as long as the rows go through once it is back.
	 */
	@Dept44Scheduled(
		cron = "${scheduler.process-event.cron}",
		name = "${scheduler.process-event.name}",
		lockAtMostFor = "${scheduler.process-event.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.process-event.maximum-execution-time}")
	public void relay() {
		processEventRelay.relay();

		processEventRelay.findHealthFault().ifPresent(fault -> {
			LOG.warn("The process event relay is unhealthy: {}", fault);
			healthUtility.setHealthIndicatorUnhealthy(relayJobName, fault);
		});
	}

	/**
	 * The nightly cleanup: delivered rows of the outbox, and entries of the activity log past their retention.
	 */
	@Dept44Scheduled(
		cron = "${scheduler.process-cleanup.cron}",
		name = "${scheduler.process-cleanup.name}",
		lockAtMostFor = "${scheduler.process-cleanup.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.process-cleanup.maximum-execution-time}")
	public void cleanUp() {
		processEventCleanup.removeDelivered();
		processEventCleanup.removeExpiredActivities();
	}
}
