package se.sundsvall.supportmanagement.service.scheduler.supensions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.employee.PortalPersonData;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.EmployeeService;
import se.sundsvall.supportmanagement.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class SuspensionWorkerTest {

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private NotificationService notificationServiceMock;

	@Mock
	private EmployeeService employeeServiceMock;

	@InjectMocks
	private SuspensionWorker suspensionWorker;

	@Captor
	private ArgumentCaptor<Notification> notificationCaptor;

	@Test
	void processExpiredSuspensions() {

		// Arrange
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		// When the previous time measurement is stopped and the current time measurement is started
		final var previousStatus = "previousStatus";
		final var errandEntity = ErrandEntity.create()
			.withNamespace(namespace)
			.withAssignedUserId("assignedUserId")
			.withId("id")
			.withMunicipalityId(municipalityId)
			.withSuspendedFrom(OffsetDateTime.now().minusDays(1))
			.withSuspendedTo(OffsetDateTime.now().minusHours(1))
			.withPreviousStatus(previousStatus)
			.withStatus("SUSPENDED")
			.withMunicipalityId(municipalityId);

		final var owner = new PortalPersonData().fullname("ownerFullName");

		when(notificationServiceMock.doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört",
			errandEntity.getSuspendedFrom())).thenReturn(false);
		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));
		when(employeeServiceMock.getEmployeeByLoginName(municipalityId, errandEntity.getAssignedUserId())).thenReturn(owner);

		// Act
		suspensionWorker.processExpiredSuspensions();

		// Assert
		verify(employeeServiceMock).getEmployeeByLoginName(municipalityId, errandEntity.getAssignedUserId());
		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verify(notificationServiceMock).doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört",
			errandEntity.getSuspendedFrom());
		verify(notificationServiceMock).createNotification(eq(municipalityId), eq(namespace), eq(errandEntity.getId()), notificationCaptor.capture());

		final var notification = notificationCaptor.getValue();
		assertThat(notification).isNotNull();
		assertThat(notification.getErrandId()).isEqualTo(errandEntity.getId());
		assertThat(notification.getErrandNumber()).isEqualTo(errandEntity.getErrandNumber());
		assertThat(notification.getOwnerFullName()).isEqualTo(owner.getFullname());
		assertThat(notification.getOwnerId()).isEqualTo(errandEntity.getAssignedUserId());

		verifyNoMoreInteractions(errandsRepositoryMock, notificationServiceMock);
	}

	@Test
	void processExpiredSuspensionsWhneAssignbedUserIsNull() {

		// Arrange
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		// When the previous time measurement is stopped and the current time measurement is started
		final var previousStatus = "previousStatus";
		final var errandEntity = ErrandEntity.create()
			.withNamespace(namespace)
			.withId("id")
			.withMunicipalityId(municipalityId)
			.withSuspendedFrom(OffsetDateTime.now().minusDays(1))
			.withSuspendedTo(OffsetDateTime.now().minusHours(1))
			.withPreviousStatus(previousStatus)
			.withStatus("SUSPENDED")
			.withMunicipalityId(municipalityId);

		when(notificationServiceMock.doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört",
			errandEntity.getSuspendedFrom())).thenReturn(false);
		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));

		// Act
		suspensionWorker.processExpiredSuspensions();

		// Assert
		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verify(notificationServiceMock).doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört",
			errandEntity.getSuspendedFrom());
		verify(notificationServiceMock).createNotification(eq(municipalityId), eq(namespace), eq(errandEntity.getId()), notificationCaptor.capture());

		final var notification = notificationCaptor.getValue();
		assertThat(notification).isNotNull();
		assertThat(notification.getErrandId()).isEqualTo(errandEntity.getId());
		assertThat(notification.getErrandNumber()).isEqualTo(errandEntity.getErrandNumber());
		assertThat(notification.getOwnerFullName()).isEqualTo("UNKNOWN");
		assertThat(notification.getOwnerId()).isEqualTo(errandEntity.getAssignedUserId());

		verifyNoInteractions(employeeServiceMock);
		verifyNoMoreInteractions(errandsRepositoryMock, notificationServiceMock);
	}

	@Test
	void processExpiredSuspensionsNoSuspensions() {

		// Arrange
		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of());
		// Act
		suspensionWorker.processExpiredSuspensions();

		// Assert
		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verifyNoMoreInteractions(errandsRepositoryMock, notificationServiceMock, employeeServiceMock);
	}

	@Test
	void processExpiredSuspensionsNotificationExists() {

		// Arrange
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(namespace)
			.withId("id")
			.withMunicipalityId(municipalityId)
			.withSuspendedFrom(OffsetDateTime.now().minusDays(1))
			.withSuspendedTo(OffsetDateTime.now().minusHours(1))
			.withStatus("SUSPENDED")
			.withMunicipalityId(municipalityId);

		when(notificationServiceMock.doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört",
			errandEntity.getSuspendedFrom())).thenReturn(true);
		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));
		// Act
		suspensionWorker.processExpiredSuspensions();

		// Assert
		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verify(notificationServiceMock).doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört",
			errandEntity.getSuspendedFrom());
		verifyNoMoreInteractions(errandsRepositoryMock, notificationServiceMock, employeeServiceMock);
	}

	@Test
	void processExpiredSuspensionsNoTimeMeasurements() {

		// Arrange
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var errandEntity = ErrandEntity.create()
			.withNamespace(namespace)
			.withAssignedUserId("assignedUserId")
			.withId("id")
			.withMunicipalityId(municipalityId)
			.withSuspendedFrom(OffsetDateTime.now().minusDays(1))
			.withSuspendedTo(OffsetDateTime.now().minusHours(1))
			.withStatus("SUSPENDED")
			.withMunicipalityId(municipalityId);

		final var owner = new PortalPersonData().fullname("ownerFullName");
		when(notificationServiceMock.doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört",
			errandEntity.getSuspendedFrom())).thenReturn(false);
		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));
		when(employeeServiceMock.getEmployeeByLoginName(municipalityId, errandEntity.getAssignedUserId())).thenReturn(owner);

		// Act
		suspensionWorker.processExpiredSuspensions();

		// Assert
		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verify(notificationServiceMock).doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört",
			errandEntity.getSuspendedFrom());
		verify(notificationServiceMock).createNotification(eq(municipalityId), eq(namespace), eq(errandEntity.getId()), notificationCaptor.capture());
		verify(employeeServiceMock).getEmployeeByLoginName(municipalityId, errandEntity.getAssignedUserId());

		final var notification = notificationCaptor.getValue();
		assertThat(notification).isNotNull();
		assertThat(notification.getErrandId()).isEqualTo(errandEntity.getId());
		assertThat(notification.getErrandNumber()).isEqualTo(errandEntity.getErrandNumber());
		assertThat(notification.getOwnerFullName()).isEqualTo(owner.getFullname());
		assertThat(notification.getOwnerId()).isEqualTo(errandEntity.getAssignedUserId());

		verifyNoMoreInteractions(errandsRepositoryMock, notificationServiceMock, employeeServiceMock);
	}
}
