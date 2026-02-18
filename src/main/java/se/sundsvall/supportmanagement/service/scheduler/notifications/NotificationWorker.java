package se.sundsvall.supportmanagement.service.scheduler.notifications;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;

import static java.time.OffsetDateTime.now;

@Component
public class NotificationWorker {

	private final NotificationRepository notificationRepository;

	public NotificationWorker(final NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Transactional
	public void cleanUpNotifications() {
		notificationRepository.deleteByExpiresBefore(now());
	}
}
