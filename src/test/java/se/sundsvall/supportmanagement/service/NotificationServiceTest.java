package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.TestObjectsBuilder;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	// @Mock
	// private NotificationRepository notificationRepositoryMock;

	@InjectMocks
	private NotificationService notificationService;


	@Test
	void getNotifications() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var ownerId = UUID.randomUUID().toString();

		// Act
		final var result = notificationService.getNotifications(municipalityId, namespace, ownerId);

		// Assert
		// Todo fix when entities are in place
		assertThat(result).isNotNull();

	}

	@Test
	void createNotification() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notification = TestObjectsBuilder.createNotification(n -> {});

		// Act
		final var result = notificationService.createNotification(municipalityId, namespace, notification);

		// Assert
		// Todo fix when entities are in place
		assertThat(result).isNotNull();
	}

	@Test
	void updateNotification() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notification = TestObjectsBuilder.createNotification(n -> {});
		final var notificationId = UUID.randomUUID().toString();

		// Act
		notificationService.updateNotification(municipalityId, namespace, notificationId, notification);

		// Assert
		// Todo fix when entities are in place
		// verify(notificationRepositoryMock).save(notification);
	}

	@Test
	void deleteNotification() {
		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notificationId = UUID.randomUUID().toString();

		// Act
		notificationService.deleteNotification(municipalityId, namespace, notificationId);

		// Assert
		// Todo fix when entities are in place
		// verify(notificationRepositoryMock).deleteById(notificationId);
	}

}
