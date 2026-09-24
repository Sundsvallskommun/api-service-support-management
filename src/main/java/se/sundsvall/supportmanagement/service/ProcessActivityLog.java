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
 * The entries SM itself writes in the activity log of an errand, as opposed to those a process reports.
 * <p>
 * Every entry is stamped with the time it is written and has its message cut to fit its column. The error entries
 * about faults that keep a process from being told or started are written once per errand, fault and window, not once
 * per occurrence.
 */
@Component
public class ProcessActivityLog {

	/** A fault in how the errand or its labels are set up, rather than in anything the process did. */
	public static final String CONFIG_ACTIVITY_TYPE = "CONFIG";

	/** The emergency brake of the loop guard tripped for the errand. */
	public static final String LOOP_GUARD_ACTIVITY_TYPE = "LOOP_GUARD";

	/** The process engine refused an event about the errand for good. */
	public static final String DELIVERY_ACTIVITY_TYPE = "DELIVERY";

	/** Two work steps of the same instance were running at once. */
	public static final String CONCURRENCY_ACTIVITY_TYPE = "CONCURRENCY";

	/** A handler sent a signal to the process of the errand. */
	public static final String SIGNAL_ACTIVITY_TYPE = "SIGNAL";

	/** A handler asked for the process of the errand to be started. */
	public static final String START_ACTIVITY_TYPE = "START";

	private final ErrandProcessActivityRepository activityRepository;
	private final ProcessEngineProperties processEngineProperties;
	private final Clock clock;

	public ProcessActivityLog(final ErrandProcessActivityRepository activityRepository, final ProcessEngineProperties processEngineProperties, final Clock clock) {
		this.activityRepository = activityRepository;
		this.processEngineProperties = processEngineProperties;
		this.clock = clock;
	}

	/**
	 * Writes an entry, stamped with the time it is written and with its message cut to fit its column.
	 *
	 * @param entry the entry to write.
	 */
	public void write(final ErrandProcessActivityEntity entry) {
		activityRepository.save(entry
			.withMessage(StringUtils.truncate(entry.getMessage(), MESSAGE_LENGTH))
			.withOccurredAt(OffsetDateTime.now(clock).truncatedTo(MILLIS)));
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
		if (activityRepository.existsByErrandIdAndErrorCodeAndCreatedAfter(errandId, errorCode, windowStart())) {
			return;
		}

		write(ErrandProcessActivityEntity.create()
			.withErrandProcessId(errandProcessId)
			.withErrandId(errandId)
			.withActivityType(activityType)
			.withSeverity(ERROR)
			.withMessage(message)
			.withErrorCode(errorCode));
	}

	/**
	 * How far back the log is asked before another entry for the same fault is written: the window of the emergency
	 * brake, so changing the brake window also changes how often a fault is reported.
	 */
	private OffsetDateTime windowStart() {
		return OffsetDateTime.now(clock).truncatedTo(MILLIS).minus(processEngineProperties.loopGuard().window());
	}
}
