package se.sundsvall.supportmanagement.service.scheduler.notifications;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationWorkerTest {

	@Mock
	private NotificationRepository notificationRepositoryMock;

	@InjectMocks
	private NotificationWorker notificationWorker;

	@Test
	void cleanUpNotifications() {

		// Act
		notificationWorker.cleanUpNotifications();

		// Assert
		verify(notificationRepositoryMock).deleteByExpiresBefore(any(OffsetDateTime.class));
	}
}
