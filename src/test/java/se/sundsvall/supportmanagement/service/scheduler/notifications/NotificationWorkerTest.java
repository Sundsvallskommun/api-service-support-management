package se.sundsvall.supportmanagement.service.scheduler.notifications;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.api.filter.ExecutingUserSupplier.UNKNOWN;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class NotificationWorkerTest {

	@Mock
	private NotificationRepository notificationRepositoryMock;

	@InjectMocks
	private NotificationWorker notificationWorker;

	@Test
	void cleanUpNotifications() {

		// Arrange
		final var entity1 = NotificationEntity.create()
			.withOwnerId(null)
			.withAcknowledged(true)
			.withGlobalAcknowledged(true); // Will be deleted: YES

		final var entity2 = NotificationEntity.create()
			.withOwnerId(null)
			.withAcknowledged(false)
			.withGlobalAcknowledged(true); // Will be deleted: YES

		final var entity3 = NotificationEntity.create()
			.withOwnerId(UNKNOWN)
			.withAcknowledged(false)
			.withGlobalAcknowledged(true); // Will be deleted: YES

		final var entity4 = NotificationEntity.create()
			.withOwnerId("user123")
			.withAcknowledged(false)
			.withGlobalAcknowledged(true); // Will be deleted: NO

		final var entity5 = NotificationEntity.create()
			.withOwnerId("user123")
			.withAcknowledged(false)
			.withGlobalAcknowledged(true); // Will be deleted: NO

		final var list = List.of(entity1, entity2, entity3, entity4, entity5);

		when(notificationRepositoryMock.findByExpiresBefore(any())).thenReturn(list);

		// Act
		notificationWorker.cleanUpNotifications();

		// Assert
		verify(notificationRepositoryMock).findByExpiresBefore(any());
		verify(notificationRepositoryMock).deleteAllInBatch(List.of(entity1, entity2, entity3));
	}

	@Test
	void cleanUpNotificationsNoNotifications() {

		// Arrange
		when(notificationRepositoryMock.findByExpiresBefore(any())).thenReturn(emptyList());

		// Act
		notificationWorker.cleanUpNotifications();

		// Assert
		verify(notificationRepositoryMock).findByExpiresBefore(any());
		verify(notificationRepositoryMock).deleteAllInBatch(emptyList());
	}
}
