package se.sundsvall.supportmanagement.service;

import static org.springframework.data.domain.Sort.unsorted;
import static org.springframework.util.StringUtils.hasText;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.NotificationMapper.toNotificationEntity;
import static se.sundsvall.supportmanagement.service.mapper.NotificationMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getAdUser;

import generated.se.sundsvall.employee.PortalPersonData;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Strings;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;
import se.sundsvall.supportmanagement.service.mapper.NotificationMapper;

@Service
public class NotificationService {

	private static final String NOTIFICATION_ENTITY_NOT_FOUND = "Notification with id:'%s' not found in namespace:'%s' for municipality with id:'%s' and errand with id:'%s'";
	private static final String NAMESPACE_ENTITY_NOT_FOUND = "Namespace with name:'%s' and municiplaityId '%s' not found!";
	private static final String ERRAND_ENTITY_NOT_FOUND = "Errand with id:'%s' not found in namespace:'%s' for municipality with id:'%s'";

	private final NotificationRepository notificationRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final ErrandsRepository errandsRepository;
	private final EmployeeService employeeService;

	public NotificationService(
		final NotificationRepository notificationRepository,
		final NamespaceConfigRepository namespaceConfigRepository,
		final ErrandsRepository errandsRepository,
		final EmployeeService employeeService) {

		this.notificationRepository = notificationRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.errandsRepository = errandsRepository;
		this.employeeService = employeeService;
	}

	public Notification getNotification(final String municipalityId, final String namespace, final String errandId, final String notificationId) {
		return notificationRepository.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, errandId)
			.map(NotificationMapper::toNotification)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOTIFICATION_ENTITY_NOT_FOUND.formatted(notificationId, namespace, municipalityId, errandId)));
	}

	public List<Notification> getNotificationsByOwnerId(final String municipalityId, final String namespace, final String ownerId) {
		return notificationRepository.findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId)
			.stream()
			.map(NotificationMapper::toNotification)
			.toList();
	}

	public List<Notification> getNotificationsByErrandId(final String municipalityId, final String namespace, final String errandId, final Sort sort) {
		return notificationRepository.findAllByNamespaceAndMunicipalityIdAndErrandEntityId(namespace, municipalityId, errandId, sort)
			.stream()
			.map(NotificationMapper::toNotification)
			.toList();
	}

	public List<Notification> getNotifications(final String municipalityId, final String namespace, final String ownerId) {
		return notificationRepository.findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId).stream().map(NotificationMapper::toNotification).toList();
	}

	public String createNotification(final String municipalityId, final String namespace, final String errandId, final Notification notification) {

		final var namespaceEntity = namespaceConfigRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NAMESPACE_ENTITY_NOT_FOUND.formatted(namespace, municipalityId)));

		final var errandEntity = errandsRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_ENTITY_NOT_FOUND.formatted(notification.getErrandId(), namespace, municipalityId)));

		final var entity = toNotificationEntity(namespace, municipalityId, namespaceEntity.getNotificationTTLInDays(), notification, errandEntity);

		applyBusinessLogicForCreate(entity);

		return notificationRepository.save(entity).getId();
	}

	public boolean doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(final String municipalityId, final String namespace, final String ownerId, final ErrandEntity errandEntity, final String description,
		final OffsetDateTime created) {
		return notificationRepository.existsByNamespaceAndMunicipalityIdAndOwnerIdAndErrandEntityAndDescriptionAndCreatedIsAfter(namespace,
			municipalityId,
			ownerId,
			errandEntity,
			description,
			created);
	}

	@Transactional
	public void updateNotifications(final String municipalityId, final String namespace, final List<Notification> notifications) {
		notifications.forEach(notification -> updateNotification(municipalityId, namespace, notification.getId(), notification));
	}

	public void globalAcknowledgeNotificationsByErrandId(final String municipalityId, final String namespace, final String errandId) {

		final var errandEntityList = notificationRepository.findAllByNamespaceAndMunicipalityIdAndErrandEntityId(namespace, municipalityId, errandId, unsorted());

		errandEntityList.forEach(errand -> errand.withGlobalAcknowledged(true));

		notificationRepository.saveAll(errandEntityList);
	}

	private void updateNotification(final String municipalityId, final String namespace, final String notificationId, final Notification notification) {
		final var entity = notificationRepository.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, notification.getErrandId())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOTIFICATION_ENTITY_NOT_FOUND.formatted(notificationId, namespace, municipalityId, notification.getErrandId())));

		updateEntity(entity, notification);

		applyBusinessLogicForUpdate(notification, entity);

		notificationRepository.save(entity);
	}

	public void deleteNotification(final String municipalityId, final String namespace, final String errandId, final String notificationId) {
		if (!notificationRepository.existsByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(notificationId, namespace, municipalityId, errandId)) {
			throw Problem.valueOf(NOT_FOUND, NOTIFICATION_ENTITY_NOT_FOUND.formatted(notificationId, namespace, municipalityId, errandId));
		}
		notificationRepository.deleteById(notificationId);
	}

	private void applyBusinessLogicForCreate(final NotificationEntity notificationEntity) {

		final var executingUser = getAdUser();
		final var municipalityId = notificationEntity.getMunicipalityId();

		// If notification is created by the user that owns the notification (ownnerId) it should be acknowledged from start.
		if (Strings.CI.equals(notificationEntity.getOwnerId(), getAdUser())) {
			notificationEntity.setAcknowledged(true);
		}

		// If ownerId is set, use this to fetch "ownerFullName".
		if (hasText(notificationEntity.getOwnerId())) {
			final var ownerFullName = Optional.ofNullable(employeeService.getEmployeeByLoginName(municipalityId, notificationEntity.getOwnerId()))
				.map(PortalPersonData::getFullname)
				.orElse(null);

			notificationEntity.setOwnerFullName(ownerFullName);
		}

		// If executingUser is set, use this to populate "createdBy" and createdByFullName (but only if createdBy is empty).
		if (hasText(executingUser)) {

			final var createdByFullName = Optional.ofNullable(employeeService.getEmployeeByLoginName(municipalityId, executingUser))
				.map(PortalPersonData::getFullname)
				.orElse(null);

			notificationEntity.setCreatedBy(executingUser);
			notificationEntity.setCreatedByFullName(createdByFullName);
		}
	}

	private void applyBusinessLogicForUpdate(final Notification notification, final NotificationEntity notificationEntity) {

		// If a notification is acknowledged, it's also global_acknowledged.
		if (notification.isAcknowledged()) {
			notificationEntity.setGlobalAcknowledged(true);
		}

		// If ownerId is set, fetch "ownerFullName" again.
		if (hasText(notification.getOwnerId())) {
			final var ownerFullName = Optional.ofNullable(employeeService.getEmployeeByLoginName(notificationEntity.getMunicipalityId(), notification.getOwnerId()))
				.map(PortalPersonData::getFullname)
				.orElse(null);

			notificationEntity.setOwnerFullName(ownerFullName);
		}
	}
}
