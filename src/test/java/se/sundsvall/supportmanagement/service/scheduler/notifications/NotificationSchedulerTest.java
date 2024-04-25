package se.sundsvall.supportmanagement.service.scheduler.notifications;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

	@Mock
	private NotificationWorker notificationWorkerMock;

	@InjectMocks
	private NotificationScheduler notificationScheduler;

	@Test
	void cleanUpNotifications() {

		// Act
		notificationScheduler.cleanUpNotifications();

		// Verify
		verify(notificationWorkerMock).cleanUpNotifications();
		verifyNoMoreInteractions(notificationWorkerMock);
	}

}
