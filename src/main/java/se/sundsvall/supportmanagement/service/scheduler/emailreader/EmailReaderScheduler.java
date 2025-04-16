package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import java.util.function.Consumer;
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
	private final Consumer<String> emailSetUnHealthyConsumer;

	@Value("${scheduler.emailreader.name}")
	private String jobName;

	public EmailReaderScheduler(final EmailReaderWorker emailReaderWorker, final Dept44HealthUtility dept44HealthUtility) {
		this.emailReaderWorker = emailReaderWorker;
		this.emailSetUnHealthyConsumer = msg -> dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, String.format("Email error: %s", msg));

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
						emailReaderWorker.processEmail(email, config, emailSetUnHealthyConsumer);
					} catch (final Exception e) {
						LOG.error("Error processing email with id: {}", email.getId(), e);
						emailSetUnHealthyConsumer.accept("Error processing email");
					}
				}));
	}
}
