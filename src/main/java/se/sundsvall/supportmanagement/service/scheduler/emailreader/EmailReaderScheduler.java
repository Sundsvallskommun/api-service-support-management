package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import se.sundsvall.dept44.requestid.RequestId;

@Service
public class EmailReaderScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(EmailReaderScheduler.class);

	private final EmailReaderWorker emailReaderWorker;
	private final EmailProcessingHealthIndicator healthIndicator;

	public EmailReaderScheduler(final EmailReaderWorker emailReaderWorker, final EmailProcessingHealthIndicator healthIndicator) {
		this.emailReaderWorker = emailReaderWorker;
		this.healthIndicator = healthIndicator;
	}

	@Scheduled(cron = "${scheduler.emailreader.cron}")
	@SchedulerLock(name = "fetch_emails", lockAtMostFor = "${scheduler.emailreader.shedlock-lock-at-most-for}")
	public void getAndProcessEmails() {

		try {
			RequestId.init();

			LOG.debug("Fetching messages from Emailreader");
			healthIndicator.resetErrors();
			emailReaderWorker.getEnabledEmailConfigs()
					.forEach(config -> emailReaderWorker.getEmailsFromConfig(config)
							.forEach(email -> {
								try {
									emailReaderWorker.processEmail(email, config);
								} catch (final Exception e) {
									LOG.error("Error processing email with id: {}", email.getId(), e);
									healthIndicator.setUnhealthy();
								}
							}));
			if (!healthIndicator.hasErrors()) {
				healthIndicator.setHealthy();
			}
			LOG.debug("Finished fetching from Emailreader");
		} finally {
			RequestId.reset();
		}
	}
}
