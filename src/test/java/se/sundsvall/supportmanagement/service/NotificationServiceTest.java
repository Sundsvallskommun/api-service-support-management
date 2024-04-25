package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.createNotificationEntity;

import java.util.List;
import java.util.Optional;
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
import se.sundsvall.supportmanagement.api.filter.ExecutingUserSupplier;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private ExecutingUserSupplier executingUserSupplierMock;

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
		final var id = "SomeId";

		when(executingUserSupplierMock.getAdUser()).thenReturn("otherAD");
		when(notificationRepositoryMock.findByNamespaceAndMunicipalityIdAndOwnerIdAndAcknowledgedAndErrandIdAndType(namespace, municipalityId, notification.getOwnerId(), notification.isAcknowledged(), notification.getErrandId(), notification.getType()))
			.thenReturn(Optional.empty());
		when(notificationRepositoryMock.save(any())).thenReturn(createNotificationEntity(n -> n.setId(id)));
		// Act
		final var result = notificationService.createNotification(municipalityId, namespace, notification);

		// Assert
		assertThat(result).isNotNull().isEqualTo(id);
		verify(notificationRepositoryMock).save(notificationEntityArgumentCaptor.capture());
		assertThat(notificationEntityArgumentCaptor.getValue().getOwnerFullName()).isEqualTo(notification.getOwnerFullName());
		verify(notificationRepositoryMock).findByNamespaceAndMunicipalityIdAndOwnerIdAndAcknowledgedAndErrandIdAndType(namespace, municipalityId, notification.getOwnerId(), notification.isAcknowledged(), notification.getErrandId(), notification.getType());
	}

	@Test
	void updateNotifications() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notificationId = UUID.randomUUID().toString();
		final var notification = TestObjectsBuilder.createNotification(n -> n.setId(notificationId));
		when(notificationRepositoryMock.findByIdAndNamespaceAndMunicipalityId(notificationId, namespace, municipalityId))
			.thenReturn(Optional.ofNullable(createNotificationEntity(n -> n.setId(notificationId))));

		// Act
		notificationService.updateNotifications(municipalityId, namespace, List.of(notification));

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
		final var notificationId = UUID.randomUUID().toString();

		final var notification = TestObjectsBuilder.createNotification(n -> n.setId(notificationId));
		// Act
		assertThatThrownBy(() -> notificationService.updateNotifications(municipalityId, namespace, List.of(notification)))
			.isInstanceOf(Problem.class)
			.hasMessage(String.format("Not Found: Notification with id %s not found in namespace %s for municipality with id %s", notificationId, namespace, municipalityId));

		// Assert
		verify(notificationRepositoryMock).findByIdAndNamespaceAndMunicipalityId(notificationId, namespace, municipalityId);
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
