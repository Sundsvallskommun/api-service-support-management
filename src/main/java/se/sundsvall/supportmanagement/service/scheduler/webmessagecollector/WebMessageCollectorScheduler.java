package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import se.sundsvall.dept44.requestid.RequestId;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import se.sundsvall.supportmanagement.integration.db.WebMessageCollectRepository;

@Service
public class WebMessageCollectorScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(WebMessageCollectorScheduler.class);

	private final WebMessageCollectorWorker worker;
	private final WebMessageCollectRepository repository;
	private final WebMessageCollectorProcessingHealthIndicator healthIndicator;

	public WebMessageCollectorScheduler(final WebMessageCollectorWorker worker,
		final WebMessageCollectRepository repository,
		final WebMessageCollectorProcessingHealthIndicator healthIndicator) {

		this.worker = worker;
		this.repository = repository;
		this.healthIndicator = healthIndicator;
	}

	@Scheduled(cron = "${scheduler.web-message-collector.cron}")
	@SchedulerLock(name = "fetch_webMessages", lockAtMostFor = "${scheduler.web-message-collector.shedlock-lock-at-most-for}")
	public void fetchWebMessages() {
		try {
			RequestId.init();

			LOG.debug("Fetching messages from WebMessageCollector");

			healthIndicator.resetErrors();
			repository.findAll().forEach(entity -> {
				entity.getFamilyIds().forEach(familyId -> {
					try {
						worker.getWebMessages(entity.getInstance(), familyId, entity.getMunicipalityId()).forEach(message -> {
							try {
								worker.processMessage(message, entity.getMunicipalityId());
							} catch (Exception e) {
								LOG.error("Error processing web message with id '{}'", message.getMessageId(), e);
								healthIndicator.setUnhealthy();
							}
						});
					} catch (Exception e) {
						LOG.error("Error fetching web messages for familyId '{}'", familyId, e);
						healthIndicator.setUnhealthy();
					}
				});
			});
			if (!healthIndicator.hasErrors()) {
				healthIndicator.setHealthy();
			}

			LOG.debug("Finished fetching from WebMessageCollector");
		} finally {
			RequestId.reset();
		}
	}
}
