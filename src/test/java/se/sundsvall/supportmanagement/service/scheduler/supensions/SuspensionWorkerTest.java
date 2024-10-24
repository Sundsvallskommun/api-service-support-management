package se.sundsvall.supportmanagement.service.scheduler.supensions;

import generated.se.sundsvall.employee.PortalPersonData;
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

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
			.withId("id")
			.withMunicipalityId(municipalityId)
			.withSuspendedFrom(OffsetDateTime.now().minusDays(1))
			.withSuspendedTo(OffsetDateTime.now().minusHours(1))
			.withPreviousStatus(previousStatus)
			.withMunicipalityId(municipalityId);

		final var owner = new PortalPersonData().fullname("ownerFullName");

		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));
		when(employeeServiceMock.getEmployeeByLoginName(errandEntity.getAssignedUserId())).thenReturn(owner);
		// Act
		suspensionWorker.processExpiredSuspensions();

		// Assert
		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verify(notificationServiceMock).createNotification(eq(municipalityId), eq(namespace), notificationCaptor.capture());
		final var notification = notificationCaptor.getValue();
		assertThat(notification).isNotNull();
		assertThat(notification.getErrandId()).isEqualTo(errandEntity.getId());
		assertThat(notification.getErrandNumber()).isEqualTo(errandEntity.getErrandNumber());
		assertThat(notification.getOwnerFullName()).isEqualTo(owner.getFullname());
		assertThat(notification.getOwnerId()).isEqualTo(errandEntity.getAssignedUserId());

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
	void processExpiredSuspensionsNoTimeMeasurements() {

		// Arrange
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		final var errandEntity = ErrandEntity.create()
			.withNamespace(namespace)
			.withId("id")
			.withMunicipalityId(municipalityId)
			.withSuspendedFrom(OffsetDateTime.now().minusDays(1))
			.withSuspendedTo(OffsetDateTime.now().minusHours(1))
			.withMunicipalityId(municipalityId);

		final var owner = new PortalPersonData().fullname("ownerFullName");

		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));
		when(employeeServiceMock.getEmployeeByLoginName(errandEntity.getAssignedUserId())).thenReturn(owner);

		// Act
		suspensionWorker.processExpiredSuspensions();

		// Assert
		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verify(notificationServiceMock).createNotification(eq(municipalityId), eq(namespace), notificationCaptor.capture());
		final var notification = notificationCaptor.getValue();
		assertThat(notification).isNotNull();
		assertThat(notification.getErrandId()).isEqualTo(errandEntity.getId());
		assertThat(notification.getErrandNumber()).isEqualTo(errandEntity.getErrandNumber());
		assertThat(notification.getOwnerFullName()).isEqualTo(owner.getFullname());
		assertThat(notification.getOwnerId()).isEqualTo(errandEntity.getAssignedUserId());

		verifyNoMoreInteractions(errandsRepositoryMock, notificationServiceMock, employeeServiceMock);
	}

}
