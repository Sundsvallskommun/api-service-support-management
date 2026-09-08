package se.sundsvall.supportmanagement.api.model.process;

/**
 * Whether a process may be started for an errand, and when it may not, why not.
 * <p>
 * One field rather than a flag beside a reason: with a flag the two can contradict each other, and a client is left
 * weighing them against one another instead of asking whether the value is AVAILABLE.
 * <p>
 * Deliberately not the type of the field it fills. {@link ProcessStartable#getStatus()} is a string, and this enum is
 * what may be written into it - the closed set is kept on the side that produces the value, so that adding a case here
 * is a release of this service rather than a new version of its API.
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
