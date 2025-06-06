package se.sundsvall.supportmanagement.service.scheduler.supensions;

import static java.time.OffsetDateTime.now;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.SUSPENSION;

import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.EmployeeService;
import se.sundsvall.supportmanagement.service.NotificationService;

@Component
public class SuspensionWorker {

	private static final String NOTIFICATION_MESSAGE = "Parkering av ärendet har upphört";

	private static final String NOTIFICATION_TYPE = "UPDATE";

	private final ErrandsRepository errandsRepository;

	private final NotificationService notificationService;

	private final EmployeeService employeeService;

	public SuspensionWorker(final ErrandsRepository errandsRepository, final NotificationService notificationService, final EmployeeService employeeService) {
		this.errandsRepository = errandsRepository;
		this.notificationService = notificationService;
		this.employeeService = employeeService;
	}

	public void processExpiredSuspensions() {
		errandsRepository
			.findAllBySuspendedToBefore(now())
			.forEach(entity -> {

				if (!"SUSPENDED".equals(entity.getStatus())) {
					return;
				}

				if (!notificationService.doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(entity.getMunicipalityId(), entity.getNamespace(), entity.getAssignedUserId(), entity, NOTIFICATION_MESSAGE,
					entity.getSuspendedFrom())) {

					notificationService
						.createNotification(entity.getMunicipalityId(), entity.getNamespace(), entity.getId(), createNotification(entity));
				}
			});
	}

	private Notification createNotification(final ErrandEntity errand) {

		final var ownerFullName = fetchFullNameByMunicipalityIdAndUserId(errand.getMunicipalityId(), errand.getAssignedUserId());

		return Notification.create()
			.withOwnerFullName(ownerFullName)
			.withOwnerId(errand.getAssignedUserId())
			.withType(NOTIFICATION_TYPE)
			.withSubtype(SUSPENSION.getValue())
			.withDescription(NOTIFICATION_MESSAGE)
			.withErrandId(errand.getId())
			.withErrandNumber(errand.getErrandNumber());
	}

	private String fetchFullNameByMunicipalityIdAndUserId(final String municipalityId, final String userId) {
		if (!isBlank(userId)) {
			final var owner = employeeService.getEmployeeByLoginName(municipalityId, userId);
			if (nonNull(owner)) {
				return owner.getFullname();
			}
		}

		return "UNKNOWN";
	}
}
