package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WebMessageCollectorScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(WebMessageCollectorScheduler.class);

	private final WebMessageCollectorWorker webMessageCollectorWorker;

	public WebMessageCollectorScheduler(final WebMessageCollectorWorker webMessageCollectorWorker) {this.webMessageCollectorWorker = webMessageCollectorWorker;}

	@Scheduled(initialDelayString = "${scheduler.web-message-collector.initialDelay}", fixedRateString = "${scheduler.web-message-collector.fixedRate}", timeUnit = TimeUnit.SECONDS)
	void fetchWebMessages() {
		LOG.info("Fetching messages from WebMessageCollector");
		webMessageCollectorWorker.fetchWebMessages();
		LOG.info("Finished fetching from WebMessageCollector");
	}

}
