package se.sundsvall.supportmanagement.service.scheduler.supensions;

import generated.se.sundsvall.eventlog.EventType;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.EventService;

import static java.time.OffsetDateTime.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SUSPENSION;

/**
 * Announces that the suspension of an errand has ended, once for every suspension. The status of the errand is left for
 * its handler to change, so what has been announced is held on the errand itself.
 */
@Component
public class SuspensionWorker {

	private static final String NOTIFICATION_MESSAGE = "Parkering av ärendet har upphört";
	private static final String STATUS_SUSPENDED = "SUSPENDED";

	private final ErrandsRepository errandsRepository;
	private final EventService eventService;

	public SuspensionWorker(final ErrandsRepository errandsRepository, final EventService eventService) {
		this.errandsRepository = errandsRepository;
		this.eventService = eventService;
	}

	@Transactional(readOnly = true)
	public List<String> getErrandIdsWithEndedSuspension() {
		return errandsRepository.findIdsWithUnannouncedSuspensionEndAndDeletedIsNull(STATUS_SUSPENDED, now(ZoneId.systemDefault()));
	}

	/**
	 * Runs in a transaction of its own, so that an errand that fails neither undoes what the others have announced, which
	 * would announce it again on the next run, nor stops the run. An errand that fails is tried again on the next run.
	 *
	 * @param errandId id of the errand to announce the end of the suspension for.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	public void announceSuspensionEnd(final String errandId) {
		errandsRepository.findWithLockingById(errandId)
			.filter(SuspensionWorker::isStillUnannounced)
			.ifPresent(errand -> {
				eventService.createErrandEvent(EventType.UPDATE, NOTIFICATION_MESSAGE, errand, null, null, SUSPENSION);
				errandsRepository.markSuspensionEndNotified(errand.getId(), errand.getSuspendedTo());
			});
	}

	/**
	 * The errand may have been changed, for example given a later end date, since its id was read.
	 */
	private static boolean isStillUnannounced(final ErrandEntity errand) {
		return STATUS_SUSPENDED.equals(errand.getStatus())
			&& isNull(errand.getDeleted())
			&& nonNull(errand.getSuspendedTo())
			&& errand.getSuspendedTo().isBefore(now(ZoneId.systemDefault()))
			&& (isNull(errand.getSuspensionEndNotifiedFor()) || !errand.getSuspensionEndNotifiedFor().isEqual(errand.getSuspendedTo()));
	}
}
