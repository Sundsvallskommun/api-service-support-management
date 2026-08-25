package se.sundsvall.supportmanagement.service.scheduler.messagingoutbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

@Service
public class MessagingOutboxScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(MessagingOutboxScheduler.class);

	private final MessagingOutboxWorker worker;
	private final Dept44HealthUtility healthUtility;

	@Value("${scheduler.messaging-outbox.name}")
	private String jobName;

	public MessagingOutboxScheduler(final MessagingOutboxWorker worker, final Dept44HealthUtility healthUtility) {
		this.worker = worker;
		this.healthUtility = healthUtility;
	}

	@Dept44Scheduled(
		cron = "${scheduler.messaging-outbox.cron}",
		name = "${scheduler.messaging-outbox.name}",
		lockAtMostFor = "${scheduler.messaging-outbox.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.messaging-outbox.maximum-execution-time}")
	public void processOutbox() {
		worker.fetchProcessable().forEach(entry -> {
			try {
				worker.process(entry);
			} catch (final Exception e) {
				LOG.error("Error processing messaging outbox entry: {}", entry.getId(), e);
				healthUtility.setHealthIndicatorUnhealthy(jobName, "Error processing messaging outbox: " + e.getMessage());
			}
		});
	}
}
