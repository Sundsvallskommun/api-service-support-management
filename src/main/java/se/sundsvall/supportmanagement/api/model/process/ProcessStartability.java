package se.sundsvall.supportmanagement.api.model.process;

/**
 * Whether a process may be started for an errand, and when it may not, why not.
 * <p>
 * The values that may be written into {@link ProcessStartable#getStatus()}, which is a string.
 */
public enum ProcessStartability {

	/** A process may be started right now. */
	AVAILABLE,

	/** A process is already running for the errand. */
	LIVE_INSTANCE,

	/** A process has already run to its end. An errand has one process life; a new process means a new errand. */
	PROCESS_COMPLETED,

	/** No label of the errand carries a process key, so there is nothing to start. */
	NO_PROCESS_KEY,

	/** The namespace does not run processes at all. */
	NO_PROCESS_ENGINE
}
