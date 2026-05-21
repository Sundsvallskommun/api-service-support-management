package se.sundsvall.supportmanagement.service.scheduler.supensions;

import generated.se.sundsvall.eventlog.EventType;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SUSPENSION;

@ExtendWith(MockitoExtension.class)
class SuspensionWorkerTest {

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private EventService eventServiceMock;

	@Mock
	private NotificationService notificationServiceMock;

	@InjectMocks
	private SuspensionWorker suspensionWorker;

	@Test
	void processExpiredSuspensions() {
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

		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));
		when(notificationServiceMock.doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(
			municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört", errandEntity.getSuspendedFrom()))
			.thenReturn(false);

		suspensionWorker.processExpiredSuspensions();

		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verify(notificationServiceMock).doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(
			municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört", errandEntity.getSuspendedFrom());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Parkering av ärendet har upphört"), eq(errandEntity), isNull(), isNull(), eq(SUSPENSION));

		verifyNoMoreInteractions(errandsRepositoryMock, notificationServiceMock, eventServiceMock);
	}

	@Test
	void processExpiredSuspensionsNotificationExists() {
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

		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));
		when(notificationServiceMock.doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(
			municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört", errandEntity.getSuspendedFrom()))
			.thenReturn(true);

		suspensionWorker.processExpiredSuspensions();

		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verify(notificationServiceMock).doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(
			municipalityId, namespace, errandEntity.getAssignedUserId(), errandEntity, "Parkering av ärendet har upphört", errandEntity.getSuspendedFrom());
		verifyNoInteractions(eventServiceMock);
		verifyNoMoreInteractions(errandsRepositoryMock, notificationServiceMock);
	}

	@Test
	void processExpiredSuspensionsNoSuspensions() {
		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of());

		suspensionWorker.processExpiredSuspensions();

		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verifyNoInteractions(eventServiceMock, notificationServiceMock);
		verifyNoMoreInteractions(errandsRepositoryMock);
	}

	@Test
	void processExpiredSuspensionsNotSuspendedStatus() {
		final var errandEntity = ErrandEntity.create()
			.withId("id")
			.withMunicipalityId("municipalityId")
			.withSuspendedTo(OffsetDateTime.now().minusHours(1))
			.withStatus("OPEN");

		when(errandsRepositoryMock.findAllBySuspendedToBefore(any(OffsetDateTime.class))).thenReturn(List.of(errandEntity));

		suspensionWorker.processExpiredSuspensions();

		verify(errandsRepositoryMock).findAllBySuspendedToBefore(any(OffsetDateTime.class));
		verifyNoInteractions(eventServiceMock, notificationServiceMock);
		verifyNoMoreInteractions(errandsRepositoryMock);
	}
}
