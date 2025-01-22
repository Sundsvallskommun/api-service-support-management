package se.sundsvall.supportmanagement.service.scheduler.notifications;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

@Service
@Transactional
public class NotificationScheduler {

	private final NotificationWorker notificationWorker;

	public NotificationScheduler(final NotificationWorker notificationWorker) {
		this.notificationWorker = notificationWorker;
	}

	@Dept44Scheduled(
		cron = "${scheduler.notification.cron}",
		name = "${scheduler.notification.name}",
		lockAtMostFor = "${scheduler.notification.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.notification.maximum-execution-time}")
	void cleanUpNotifications() {
		notificationWorker.cleanUpNotifications();
	}
}
