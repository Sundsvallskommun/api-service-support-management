package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;

import static java.time.temporal.ChronoUnit.MILLIS;
import static se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity.MESSAGE_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;

/**
 * The error entries SM itself writes on an errand, about faults that keep its process from being told or started.
 * <p>
 * Each is written once per errand, fault and window, not once per occurrence.
 */
@Component
public class ProcessErrorLog {

	/**
	 * A fault in how the errand or its labels are set up, rather than in anything the process did.
	 */
	static final String CONFIG_ACTIVITY_TYPE = "CONFIG";

	private final ErrandProcessActivityRepository activityRepository;
	private final ProcessEngineProperties processEngineProperties;
	private final Clock clock;

	public ProcessErrorLog(final ErrandProcessActivityRepository activityRepository, final ProcessEngineProperties processEngineProperties, final Clock clock) {
		this.activityRepository = activityRepository;
		this.processEngineProperties = processEngineProperties;
		this.clock = clock;
	}

	/**
	 * Writes an error entry on an errand, unless one for the same fault has already been written inside the window.
	 *
	 * @param errandId        the errand to write the entry on.
	 * @param errandProcessId the process row the entry belongs to, or null when the fault happened without one.
	 * @param activityType    the kind of entry.
	 * @param errorCode       the code of the fault, which is what a repetition is recognised by.
	 * @param message         what is wrong and what to do about it. Cut to fit its column.
	 */
	public void writeOncePerWindow(final String errandId, final String errandProcessId, final String activityType, final String errorCode, final String message) {
		final var now = OffsetDateTime.now(clock).truncatedTo(MILLIS);

		if (activityRepository.existsByErrandIdAndErrorCodeAndCreatedAfter(errandId, errorCode, windowStart(now))) {
			return;
		}

		activityRepository.save(ErrandProcessActivityEntity.create()
			.withErrandProcessId(errandProcessId)
			.withErrandId(errandId)
			.withActivityType(activityType)
			.withSeverity(ERROR)
			.withMessage(StringUtils.truncate(message, MESSAGE_LENGTH))
			.withErrorCode(errorCode)
			.withOccurredAt(now));
	}

	/**
	 * How far back the log is asked before another entry for the same fault is written: the window of the emergency
	 * brake, so changing the brake window also changes how often a fault is reported.
	 */
	private OffsetDateTime windowStart(final OffsetDateTime now) {
		return now.minus(processEngineProperties.loopGuard().window());
	}
}
