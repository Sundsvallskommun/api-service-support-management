package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
@Transactional
public class EmailReaderScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(EmailReaderScheduler.class);

	private final EmailReaderWorker emailReaderWorker;

	public EmailReaderScheduler(final EmailReaderWorker emailReaderWorker) {
		this.emailReaderWorker = emailReaderWorker;
	}

	@Scheduled(initialDelayString = "${scheduler.emailreader.initialDelay}", fixedRateString = "${scheduler.emailreader.fixedRate}", timeUnit = TimeUnit.SECONDS)
	@SchedulerLock(name = "fetch_emails", lockAtMostFor = "${scheduler.emailreader.shedlock-lock-at-most-for}")
	void getAndProcessEmails() {
		LOG.debug("Fetching messages from Emailreader");
		emailReaderWorker.getAndProcessEmails();
		LOG.debug("Finished fetching from Emailreader");
	}

}
