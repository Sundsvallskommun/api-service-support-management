package se.sundsvall.supportmanagement.service.scheduler.notifications;


import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
@Transactional
public class NotificationScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationScheduler.class);

	private final NotificationWorker notificationWorker;

	public NotificationScheduler(final NotificationWorker notificationWorker) {this.notificationWorker = notificationWorker;}

	@Scheduled(initialDelayString = "${scheduler.notification.initialDelay}", fixedRateString = "${scheduler.notification.fixedRate}", timeUnit = TimeUnit.SECONDS)
	@SchedulerLock(name = "clean_notifications", lockAtMostFor = "${scheduler.notification.shedlock-lock-at-most-for}")
	void cleanUpNotifications() {

		LOG.debug("Cleaning up notifications");
		notificationWorker.cleanUpNotifications();
		LOG.debug("Finished cleaning up notifications");
	}

}
