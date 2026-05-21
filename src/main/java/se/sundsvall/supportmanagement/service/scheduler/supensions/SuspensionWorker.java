package se.sundsvall.supportmanagement.service.scheduler.supensions;

import generated.se.sundsvall.eventlog.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.NotificationService;

import static java.time.OffsetDateTime.now;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SUSPENSION;

@Component
public class SuspensionWorker {

	private static final Logger LOG = LoggerFactory.getLogger(SuspensionWorker.class);

	private static final String NOTIFICATION_MESSAGE = "Parkering av ärendet har upphört";

	private final ErrandsRepository errandsRepository;
	private final EventService eventService;
	private final NotificationService notificationService;

	public SuspensionWorker(final ErrandsRepository errandsRepository, final EventService eventService, final NotificationService notificationService) {
		this.errandsRepository = errandsRepository;
		this.eventService = eventService;
		this.notificationService = notificationService;
	}

	@Transactional
	public void processExpiredSuspensions() {
		errandsRepository
			.findAllBySuspendedToBefore(now())
			.forEach(entity -> {

				if (!"SUSPENDED".equals(entity.getStatus())) {
					return;
				}

				if (!notificationService.doesNotificationWithSpecificDescriptionExistForOwnerAndErrandAndNotificationIsCreatedAfter(entity.getMunicipalityId(), entity.getNamespace(), entity.getAssignedUserId(), entity, NOTIFICATION_MESSAGE,
					entity.getSuspendedFrom())) {

					try {
						eventService.createErrandEvent(EventType.UPDATE, NOTIFICATION_MESSAGE, entity, null, null, SUSPENSION);
					} catch (final Exception e) {
						LOG.warn("Failed to log suspension-expired event for errand {}: {}", entity.getId(), e.getMessage());
					}
				}
			});
	}
}
