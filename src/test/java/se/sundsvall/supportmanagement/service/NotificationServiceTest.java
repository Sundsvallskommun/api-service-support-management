package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.createNotificationEntity;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.TestObjectsBuilder;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepositoryMock;

	@InjectMocks
	private NotificationService notificationService;

	@Captor
	private ArgumentCaptor<NotificationEntity> notificationEntityArgumentCaptor;


	@Test
	void getNotifications() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var ownerId = UUID.randomUUID().toString();

		when(notificationRepositoryMock.findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId)).thenReturn(List.of(createNotificationEntity(n -> {})));

		// Act
		final var result = notificationService.getNotifications(municipalityId, namespace, ownerId);

		// Assert
		assertThat(result).isNotNull().hasSize(1);
		verify(notificationRepositoryMock).findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId);
	}


	@Test
	void getNotifications_noneFound() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var ownerId = UUID.randomUUID().toString();

		// Act
		final var result = notificationService.getNotifications(municipalityId, namespace, ownerId);

		// Assert
		assertThat(result).isNotNull().isEmpty();
		verify(notificationRepositoryMock).findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId);
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
		when(notificationRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(notificationId, namespace, municipalityId)).thenReturn(true);
		when(notificationRepositoryMock.getReferenceById(notificationId)).thenReturn(createNotificationEntity(n -> {}));

		// Act
		notificationService.updateNotification(municipalityId, namespace, notificationId, notification);

		// Assert
		verify(notificationRepositoryMock).save(notificationEntityArgumentCaptor.capture());
		assertThat(notificationEntityArgumentCaptor.getValue().getOwnerFullName()).isEqualTo(notification.getOwnerFullName());
		assertThat(notificationEntityArgumentCaptor.getValue().getOwnerId()).isEqualTo(notification.getOwnerId());
		assertThat(notificationEntityArgumentCaptor.getValue().getCreatedBy()).isEqualTo(notification.getCreatedBy());
		assertThat(notificationEntityArgumentCaptor.getValue().getType()).isEqualTo(notification.getType());
		assertThat(notificationEntityArgumentCaptor.getValue().getDescription()).isEqualTo(notification.getDescription());
		assertThat(notificationEntityArgumentCaptor.getValue().getErrandId()).isEqualTo(notification.getErrandId());
		assertThat(notificationEntityArgumentCaptor.getValue().isAcknowledged()).isEqualTo(notification.isAcknowledged());
		assertThat(notificationEntityArgumentCaptor.getValue().getNamespace()).isEqualTo(namespace);
		assertThat(notificationEntityArgumentCaptor.getValue().getMunicipalityId()).isEqualTo(municipalityId);
	}

	@Test
	void updateNotification_notFound() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notification = TestObjectsBuilder.createNotification(n -> {});
		final var notificationId = UUID.randomUUID().toString();

		// Act
		assertThatThrownBy(() -> notificationService.updateNotification(municipalityId, namespace, notificationId, notification))
			.isInstanceOf(Problem.class)
			.hasMessage(String.format("Not Found: Notification with id %s not found in namespace %s for municipality with id %s", notificationId, namespace, municipalityId));

		// Assert
		verify(notificationRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(notificationId, namespace, municipalityId);
	}

	@Test
	void deleteNotification() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notificationId = UUID.randomUUID().toString();
		when(notificationRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(notificationId, namespace, municipalityId)).thenReturn(true);

		// Act
		notificationService.deleteNotification(municipalityId, namespace, notificationId);

		// Assert
		verify(notificationRepositoryMock).deleteById(notificationId);
	}

	@Test
	void deleteNotification_notFound() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notificationId = UUID.randomUUID().toString();

		// Act
		assertThatThrownBy(() -> notificationService.deleteNotification(municipalityId, namespace, notificationId))
			.isInstanceOf(Problem.class)
			.hasMessage(String.format("Not Found: Notification with id %s not found in namespace %s for municipality with id %s", notificationId, namespace, municipalityId));

		// Assert
		verify(notificationRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(notificationId, namespace, municipalityId);
	}

}
