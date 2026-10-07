package se.sundsvall.supportmanagement.service.scheduler.teliaace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

@Service
public class TeliaAceWorkItemScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(TeliaAceWorkItemScheduler.class);

	private final TeliaAceWorkItemWorker worker;
	private final Dept44HealthUtility healthUtility;

	@Value("${scheduler.telia-ace-work-item.name}")
	private String jobName;

	public TeliaAceWorkItemScheduler(final TeliaAceWorkItemWorker worker, final Dept44HealthUtility healthUtility) {
		this.worker = worker;
		this.healthUtility = healthUtility;
	}

	@Dept44Scheduled(
		cron = "${scheduler.telia-ace-work-item.cron}",
		name = "${scheduler.telia-ace-work-item.name}",
		lockAtMostFor = "${scheduler.telia-ace-work-item.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.telia-ace-work-item.maximum-execution-time}")
	public void processWorkItems() {
		// A failing work item is rolled back and left in place for the next run, without holding up the rest of the queue.
		worker.fetchProcessable().forEach(workItem -> {
			try {
				worker.process(workItem);
			} catch (final Exception e) {
				LOG.error("Error delivering Telia ACE work item for errand: {}", workItem.getErrandId(), e);
				healthUtility.setHealthIndicatorUnhealthy(jobName, "Error delivering Telia ACE work item: " + e.getMessage());
			}
		});
	}
}
