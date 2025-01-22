package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

@Service
public class EmailReaderScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(EmailReaderScheduler.class);

	private final EmailReaderWorker emailReaderWorker;
	private final Dept44HealthUtility healthUtility;

	@Value("${scheduler.emailreader.name}")
	private String jobName;

	public EmailReaderScheduler(final EmailReaderWorker emailReaderWorker, final Dept44HealthUtility healthUtility) {
		this.emailReaderWorker = emailReaderWorker;
		this.healthUtility = healthUtility;
	}

	@Dept44Scheduled(cron = "${scheduler.emailreader.cron}",
		name = "${scheduler.emailreader.name}",
		lockAtMostFor = "${scheduler.emailreader.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.emailreader.maximum-execution-time}")
	public void getAndProcessEmails() {

		emailReaderWorker.getEnabledEmailConfigs()
			.forEach(config -> emailReaderWorker.getEmailsFromConfig(config)
				.forEach(email -> {
					try {
						emailReaderWorker.processEmail(email, config);
					} catch (final Exception e) {
						LOG.error("Error processing email with id: {}", email.getId(), e);
						healthUtility.setHealthIndicatorUnhealthy(jobName, "Error processing email");
					}
				}));
	}
}
