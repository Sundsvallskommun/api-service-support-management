package se.sundsvall.supportmanagement.service.scheduler.emaildispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

@Service
public class EmailDispatchScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(EmailDispatchScheduler.class);

	private final SubscriberEmailService subscriberEmailService;

	public EmailDispatchScheduler(final SubscriberEmailService subscriberEmailService) {
		this.subscriberEmailService = subscriberEmailService;
	}

	@Dept44Scheduled(
		cron = "${scheduler.email-dispatch.cron}",
		name = "${scheduler.email-dispatch.name}",
		lockAtMostFor = "${scheduler.email-dispatch.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.email-dispatch.maximum-execution-time}")
	public void processEmailDispatch() {
		LOG.info("Processing email dispatch outbox");
		final var entries = subscriberEmailService.fetchPending();
		for (final var entry : entries) {
			subscriberEmailService.processEntry(entry);
		}
	}
}
