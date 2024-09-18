package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import se.sundsvall.dept44.requestid.RequestId;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
public class WebMessageCollectorScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(WebMessageCollectorScheduler.class);

	private final WebMessageCollectorWorker webMessageCollectorWorker;

	public WebMessageCollectorScheduler(final WebMessageCollectorWorker webMessageCollectorWorker) {
		this.webMessageCollectorWorker = webMessageCollectorWorker;
	}

	@Scheduled(cron = "${scheduler.web-message-collector.cron}")
	@SchedulerLock(name = "fetch_webMessages", lockAtMostFor = "${scheduler.web-message-collector.shedlock-lock-at-most-for}")
	public void fetchWebMessages() {
		try {
			RequestId.init();

			LOG.debug("Fetching messages from WebMessageCollector");

			webMessageCollectorWorker.fetchWebMessages()
					.forEach((municipalityId, attachments) ->
							attachments.forEach(attachment ->
									webMessageCollectorWorker.processAttachments(attachment, municipalityId)));


			LOG.debug("Finished fetching from WebMessageCollector");
		} finally {
			RequestId.reset();
		}
	}

}
