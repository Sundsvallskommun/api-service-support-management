package se.sundsvall.supportmanagement.service.scheduler.notifications;

import static java.time.OffsetDateTime.now;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;

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
