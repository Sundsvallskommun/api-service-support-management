package se.sundsvall.supportmanagement.service.scheduler.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import se.sundsvall.dept44.requestid.RequestId;

@Service
@Transactional
public class NotificationScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationScheduler.class);

	private final NotificationWorker notificationWorker;

	public NotificationScheduler(final NotificationWorker notificationWorker) {
		this.notificationWorker = notificationWorker;
	}

	@Scheduled(cron = "${scheduler.notification.cron}")
	@SchedulerLock(name = "clean_notifications", lockAtMostFor = "${scheduler.notification.shedlock-lock-at-most-for}")
	void cleanUpNotifications() {
		try {
			RequestId.init();

			LOG.debug("Cleaning up notifications");
			notificationWorker.cleanUpNotifications();
			LOG.debug("Finished cleaning up notifications");
		} finally {
			RequestId.reset();
		}
	}

}
