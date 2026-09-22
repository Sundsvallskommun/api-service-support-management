package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * The state of a process instance, and with it the answer to whether the errand may be given a new one.
 */
public enum ProcessStatus {

	/** A work step is running right now. */
	RUNNING(false),

	/** Waiting for a handler, a timer or an external party. Alive, but not working. */
	WAITING(false),

	/** An attempt failed and the process engine will try again. Alive, but not working. */
	RETRYING(false),

	/** The process reached its end. */
	COMPLETED(true),

	/** Retries exhausted, an incident was raised, or the start never succeeded. */
	FAILED(true);

	private final boolean terminal;

	ProcessStatus(final boolean terminal) {
		this.terminal = terminal;
	}

	/**
	 * Drives the active marker, and with it how many live instances an errand can have. WAITING is not terminal: a
	 * waiting process is alive and keeps the marker.
	 *
	 * @return whether the process has run its course.
	 */
	public boolean isTerminal() {
		return terminal;
	}
}
