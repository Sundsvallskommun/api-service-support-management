package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.WebMessageCollectRepository;

@Service
public class WebMessageCollectorScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(WebMessageCollectorScheduler.class);

	private final WebMessageCollectorWorker worker;
	private final WebMessageCollectRepository repository;
	private final Dept44HealthUtility healthUtility;

	@Value("${scheduler.web-message-collector.name}")
	private String jobName;

	public WebMessageCollectorScheduler(final WebMessageCollectorWorker worker,
		final WebMessageCollectRepository repository,
		final Dept44HealthUtility healthUtility) {

		this.worker = worker;
		this.repository = repository;
		this.healthUtility = healthUtility;
	}

	@Dept44Scheduled(
		cron = "${scheduler.web-message-collector.cron}",
		name = "${scheduler.web-message-collector.name}",
		lockAtMostFor = "${scheduler.web-message-collector.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.web-message-collector.maximum-execution-time}")
	public void fetchWebMessages() {

		repository.findAll().forEach(entity -> entity.getFamilyIds().forEach(familyId -> {
			try {
				worker.getWebMessages(entity.getInstance(), familyId, entity.getMunicipalityId()).forEach(message -> {
					try {
						worker.processMessage(message, entity.getMunicipalityId());
					} catch (final Exception e) {
						LOG.error("Error processing web message with id '{}'", message.getMessageId(), e);
						healthUtility.setHealthIndicatorUnhealthy(jobName, String.format("Error processing web message with id: %s", message.getMessageId()));
					}
				});
			} catch (final Exception e) {
				LOG.error("Error fetching web messages for familyId '{}'", familyId, e);
				healthUtility.setHealthIndicatorUnhealthy(jobName, String.format("Error fetching web messages for familyId: %s", familyId));
			}
		}));
	}
}
