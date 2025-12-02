package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.unsorted;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.buildErrandEntity;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.createNotificationEntity;

import generated.se.sundsvall.employee.PortalPersonData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.TestObjectsBuilder;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private NotificationRepository notificationRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Mock
	private EmployeeService employeeServiceMock;

	@InjectMocks
	private NotificationService notificationService;

	@Captor
	private ArgumentCaptor<NotificationEntity> notificationEntityArgumentCaptor;

	@Captor
	private ArgumentCaptor<List<NotificationEntity>> notificationEntityListArgumentCaptor;

	@BeforeEach
	void beforeEach() {
		Identifier.set(Identifier.create().withType(AD_ACCOUNT).withValue("executingUserId"));
	}

	@Test
	void getNotification() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notificationId = randomUUID().toString();
		final var errandId = randomUUID().toString();

		when(notificationRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, errandId)).thenReturn(Optional.ofNullable(createNotificationEntity(n -> {})));

		// Act
		final var result = notificationService.getNotification(municipalityId, namespace, errandId, notificationId);

		// Assert
		assertThat(result).isNotNull();
		verify(notificationRepositoryMock).findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, errandId);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, R, RW);
	}

	@Test
	void getNotificationNotFound() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notificationId = randomUUID().toString();
		final var errandId = randomUUID().toString();

		// Act
		assertThatThrownBy(() -> notificationService.getNotification(municipalityId, namespace, errandId, notificationId))
			.isInstanceOf(Problem.class)
			.hasMessage(String.format("Not Found: Notification with id:'%s' not found in namespace:'%s' for municipality with id:'%s' and errand with id:'%s'", notificationId, namespace, municipalityId, errandId));

		// Assert
		verify(notificationRepositoryMock).findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, errandId);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, R, RW);
	}

	@Test
	void getNotificationsByOwnerId() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var ownerId = randomUUID().toString();

		when(notificationRepositoryMock.findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId)).thenReturn(List.of(createNotificationEntity(n -> {})));

		// Act
		final var result = notificationService.getNotificationsByOwnerId(municipalityId, namespace, ownerId);

		// Assert
		assertThat(result).isNotNull().hasSize(1);
		verify(notificationRepositoryMock).findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId);
	}

	@Test
	void getNotificationsByOwnerIdNoneFound() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var ownerId = randomUUID().toString();

		// Act
		final var result = notificationService.getNotificationsByOwnerId(municipalityId, namespace, ownerId);

		// Assert
		assertThat(result).isNotNull().isEmpty();
		verify(notificationRepositoryMock).findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId);
	}

	@Test
	void getNotificationsByErrandId() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var errandId = randomUUID().toString();
		final var sort = Sort.by("modified");

		when(notificationRepositoryMock.findAllByNamespaceAndMunicipalityIdAndErrandEntityId(namespace, municipalityId, errandId, sort)).thenReturn(List.of(createNotificationEntity(n -> {})));

		// Act
		final var result = notificationService.getNotificationsByErrandId(municipalityId, namespace, errandId, sort);

		// Assert
		assertThat(result).isNotNull().hasSize(1);
		verify(notificationRepositoryMock).findAllByNamespaceAndMunicipalityIdAndErrandEntityId(namespace, municipalityId, errandId, sort);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, R, RW);
	}

	@Test
	void getNotificationsByErrandIdNoneFound() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var errandId = randomUUID().toString();

		// Act
		final var result = notificationService.getNotificationsByErrandId(municipalityId, namespace, errandId, null);

		// Assert
		assertThat(result).isNotNull().isEmpty();
		verify(notificationRepositoryMock).findAllByNamespaceAndMunicipalityIdAndErrandEntityId(namespace, municipalityId, errandId, null);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, R, RW);
	}

	@Test
	void createNotification() {

		// Arrange
		final var notificationTTLInDays = 10;
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notification = TestObjectsBuilder.createNotification(n -> n.withExpires(null));
		final var errandEntity = TestObjectsBuilder.createNotificationEntity(n -> {}).getErrandEntity();
		errandEntity.setNamespace(namespace);
		errandEntity.setMunicipalityId(municipalityId);
		final var id = "SomeId";
		final var executingUserId = "executingUserId";
		final var createdByFullName = "createdByFullName";
		final var ownerFullName = "ownerFullName";

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(Optional.of(NamespaceConfigEntity.create().withNotificationTTLInDays(notificationTTLInDays)));
		when(accessControlServiceMock.getErrand(namespace, municipalityId, errandEntity.getId(), false, RW)).thenReturn(errandEntity);
		when(notificationRepositoryMock.save(any())).thenReturn(createNotificationEntity(n -> n.setId(id)));
		when(employeeServiceMock.getEmployeeByLoginName(municipalityId, errandEntity.getAssignedUserId())).thenReturn(new PortalPersonData().loginName(errandEntity.getAssignedUserId()).fullname(ownerFullName));
		when(employeeServiceMock.getEmployeeByLoginName(municipalityId, executingUserId)).thenReturn(new PortalPersonData().loginName(executingUserId).fullname(createdByFullName));

		// Act
		final var result = notificationService.createNotification(municipalityId, namespace, errandEntity.getId(), notification);

		// Assert
		assertThat(result).isNotNull().isEqualTo(id);
		verify(employeeServiceMock).getEmployeeByLoginName(municipalityId, executingUserId);
		verify(employeeServiceMock).getEmployeeByLoginName(municipalityId, errandEntity.getAssignedUserId());
		verify(accessControlServiceMock).getErrand(namespace, municipalityId, errandEntity.getId(), false, RW);
		verify(notificationRepositoryMock).save(notificationEntityArgumentCaptor.capture());
		assertThat(notificationEntityArgumentCaptor.getValue().getOwnerFullName()).isEqualTo(ownerFullName);
		assertThat(notificationEntityArgumentCaptor.getValue().getExpires()).isCloseTo(now().plusDays(notificationTTLInDays), within(2, SECONDS));
		assertThat(notificationEntityArgumentCaptor.getValue().getCreatedByFullName()).isEqualTo(createdByFullName);
		assertThat(notificationEntityArgumentCaptor.getValue().isGlobalAcknowledged()).isFalse();
		assertThat(notificationEntityArgumentCaptor.getValue().isAcknowledged()).isFalse();
	}

	@Test
	void createNotificationWhenExecutingUserIsTheSameAsOwnerId() {

		// Arrange,
		final var notificationTTLInDays = 10;
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notification = TestObjectsBuilder.createNotification(n -> n.withExpires(null));
		final var errandEntity = TestObjectsBuilder.createNotificationEntity(n -> {}).getErrandEntity();
		final var id = "SomeId";
		final var executingUserId = notification.getOwnerId();
		final var fullName = "fullName";

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(Optional.of(NamespaceConfigEntity.create().withNotificationTTLInDays(notificationTTLInDays)));
		when(accessControlServiceMock.getErrand(namespace, municipalityId, errandEntity.getId(), false, RW)).thenReturn(errandEntity);
		when(notificationRepositoryMock.save(any())).thenReturn(createNotificationEntity(n -> n.setId(id)));
		when(employeeServiceMock.getEmployeeByLoginName(municipalityId, executingUserId)).thenReturn(new PortalPersonData().loginName(executingUserId).fullname(fullName));

		Identifier.set(Identifier.create().withType(AD_ACCOUNT).withValue(executingUserId));

		// Act
		final var result = notificationService.createNotification(municipalityId, namespace, errandEntity.getId(), notification);

		// Assert
		assertThat(result).isNotNull().isEqualTo(id);
		verify(employeeServiceMock, times(2)).getEmployeeByLoginName(municipalityId, executingUserId);
		verify(notificationRepositoryMock).save(notificationEntityArgumentCaptor.capture());
		verify(accessControlServiceMock).getErrand(namespace, municipalityId, errandEntity.getId(), false, RW);
		assertThat(notificationEntityArgumentCaptor.getValue().getOwnerFullName()).isEqualTo(fullName);
		assertThat(notificationEntityArgumentCaptor.getValue().getExpires()).isCloseTo(now().plusDays(notificationTTLInDays), within(2, SECONDS));
		assertThat(notificationEntityArgumentCaptor.getValue().getCreatedByFullName()).isEqualTo(fullName);
		assertThat(notificationEntityArgumentCaptor.getValue().isGlobalAcknowledged()).isFalse();
		assertThat(notificationEntityArgumentCaptor.getValue().isAcknowledged()).isTrue(); // Set to true when ownerId == executingUserId
	}

	@Test
	void updateNotifications() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notificationId = randomUUID().toString();
		final var notification = TestObjectsBuilder.createNotification(n -> n.setId(notificationId));
		final var executingUserId = notification.getOwnerId();
		final var ownerFullName = "fullName";

		when(notificationRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, notification.getErrandId()))
			.thenReturn(Optional.ofNullable(createNotificationEntity(n -> n.setId(notificationId))));
		when(employeeServiceMock.getEmployeeByLoginName(municipalityId, executingUserId)).thenReturn(new PortalPersonData().loginName(executingUserId).fullname(ownerFullName));

		// Act
		notificationService.updateNotifications(municipalityId, namespace, List.of(notification));

		// Assert
		verify(notificationRepositoryMock).save(notificationEntityArgumentCaptor.capture());
		verify(employeeServiceMock).getEmployeeByLoginName(municipalityId, executingUserId);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(namespace, municipalityId, notification.getErrandId(), RW);
		assertThat(notificationEntityArgumentCaptor.getValue().getOwnerFullName()).isEqualTo(ownerFullName);
		assertThat(notificationEntityArgumentCaptor.getValue().getOwnerId()).isEqualTo(notification.getOwnerId());
		assertThat(notificationEntityArgumentCaptor.getValue().getCreatedBy()).isEqualTo(notification.getCreatedBy());
		assertThat(notificationEntityArgumentCaptor.getValue().getType()).isEqualTo(notification.getType());
		assertThat(notificationEntityArgumentCaptor.getValue().getDescription()).isEqualTo(notification.getDescription());
		assertThat(notificationEntityArgumentCaptor.getValue().getErrandEntity().getId()).isEqualTo(notification.getErrandId());
		assertThat(notificationEntityArgumentCaptor.getValue().isAcknowledged()).isEqualTo(notification.isAcknowledged());
		assertThat(notificationEntityArgumentCaptor.getValue().getNamespace()).isEqualTo(namespace);
		assertThat(notificationEntityArgumentCaptor.getValue().getMunicipalityId()).isEqualTo(municipalityId);
	}

	@Test
	void updateNotificationNotFound() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notificationId = randomUUID().toString();

		final var notification = TestObjectsBuilder.createNotification(n -> n.setId(notificationId));
		// Act
		assertThatThrownBy(() -> notificationService.updateNotifications(municipalityId, namespace, List.of(notification)))
			.isInstanceOf(Problem.class)
			.hasMessage(String.format("Not Found: Notification with id:'%s' not found in namespace:'%s' for municipality with id:'%s' and errand with id:'%s'", notificationId, namespace, municipalityId, notification.getErrandId()));

		// Assert
		verify(employeeServiceMock, never()).getEmployeeByLoginName(eq(municipalityId), any());
		verify(notificationRepositoryMock).findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, notification.getErrandId());
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(namespace, municipalityId, notification.getErrandId(), RW);
	}

	@Test
	void globalAcknowledgeNotificationsByErrandId() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var errandId = randomUUID().toString();
		final var notificationEntity1 = NotificationEntity.create();
		final var notificationEntity2 = NotificationEntity.create();
		final var notificationEntity3 = NotificationEntity.create();

		when(notificationRepositoryMock.findAllByNamespaceAndMunicipalityIdAndErrandEntityId(eq(namespace), eq(municipalityId), eq(errandId), any()))
			.thenReturn(List.of(notificationEntity1, notificationEntity2, notificationEntity3));

		// Act
		notificationService.globalAcknowledgeNotificationsByErrandId(municipalityId, namespace, errandId);

		// Assert
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, RW);
		verify(notificationRepositoryMock).findAllByNamespaceAndMunicipalityIdAndErrandEntityId(namespace, municipalityId, errandId, unsorted());
		verify(notificationRepositoryMock).saveAll(notificationEntityListArgumentCaptor.capture());

		final var capturedNotificationEntitySaveList = notificationEntityListArgumentCaptor.getValue();
		assertThat(capturedNotificationEntitySaveList).hasSize(3);
		capturedNotificationEntitySaveList.forEach(elem -> assertThat(elem.isGlobalAcknowledged()).isTrue());
	}

	@Test
	void globalAcknowledgeNotificationsByErrandIdWhenNothingFound() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var errandId = randomUUID().toString();

		when(notificationRepositoryMock.findAllByNamespaceAndMunicipalityIdAndErrandEntityId(eq(namespace), eq(municipalityId), eq(errandId), any()))
			.thenReturn(emptyList());

		// Act
		notificationService.globalAcknowledgeNotificationsByErrandId(municipalityId, namespace, errandId);

		// Assert
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, RW);
		verify(notificationRepositoryMock).findAllByNamespaceAndMunicipalityIdAndErrandEntityId(namespace, municipalityId, errandId, unsorted());
		verify(notificationRepositoryMock).saveAll(notificationEntityListArgumentCaptor.capture());

		final var capturedNotificationEntitySaveList = notificationEntityListArgumentCaptor.getValue();
		assertThat(capturedNotificationEntitySaveList).isEmpty();
	}

	@Test
	void deleteNotification() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var notificationId = randomUUID().toString();
		final var errandId = randomUUID().toString();

		when(notificationRepositoryMock.existsByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, errandId)).thenReturn(true);

		// Act
		notificationService.deleteNotification(municipalityId, namespace, errandId, notificationId);

		// Assert
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, RW);
		verify(notificationRepositoryMock).deleteById(notificationId);
	}

	@Test
	void doesNotificationWithSpecificDescriptionExistForOwnerAndErrand() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var ownerId = randomUUID().toString();
		final var errandEntity = buildErrandEntity();
		final var description = "description";
		final var created = now();

		when(notificationRepositoryMock.existsByNamespaceAndMunicipalityIdAndOwnerIdAndErrandEntityAndDescriptionAndCreatedIsAfter(namespace, municipalityId, ownerId, errandEntity, description, created)).thenReturn(true);

		// Act
		final var result = notificationService.doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(municipalityId, namespace, ownerId, errandEntity, description, created);

		// Assert
		assertThat(result).isTrue();

		verify(notificationRepositoryMock).existsByNamespaceAndMunicipalityIdAndOwnerIdAndErrandEntityAndDescriptionAndCreatedIsAfter(namespace, municipalityId, ownerId, errandEntity, description, created);
	}

	@Test
	void doesNotificationWithSpecificDescriptionExistForOwnerAndErrandNotFound() {
		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var ownerId = randomUUID().toString();
		final var errandEntity = buildErrandEntity();
		final var description = "description";
		final var created = now();

		when(notificationRepositoryMock.existsByNamespaceAndMunicipalityIdAndOwnerIdAndErrandEntityAndDescriptionAndCreatedIsAfter(namespace, municipalityId, ownerId, errandEntity, description, created)).thenReturn(false);

		// Act
		final var result = notificationService.doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(municipalityId, namespace, ownerId, errandEntity, description, created);

		// Assert
		assertThat(result).isFalse();

		verify(notificationRepositoryMock).existsByNamespaceAndMunicipalityIdAndOwnerIdAndErrandEntityAndDescriptionAndCreatedIsAfter(namespace, municipalityId, ownerId, errandEntity, description, created);
	}

	void deleteNotificationNotFound() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var errandId = randomUUID().toString();
		final var notificationId = randomUUID().toString();

		// Act
		assertThatThrownBy(() -> notificationService.deleteNotification(municipalityId, namespace, errandId, notificationId))
			.isInstanceOf(Problem.class)
			.hasMessage(String.format("Not Found: Notification with id:'%s' not found in namespace:'%s' for municipality with id:'%s' and errand with id:'%s'", notificationId, namespace, municipalityId, errandId));

		// Assert
		verify(notificationRepositoryMock).existsByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, errandId);
	}
}
