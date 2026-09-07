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
	 * Drives the active marker, and with it how many live instances an errand can have.
	 * <p>
	 * WAITING is the trap: the process is not working, which reads as done. Counted as terminal it frees the marker,
	 * and the errand can be given a second live instance.
	 *
	 * @return whether the process has run its course.
	 */
	public boolean isTerminal() {
		return terminal;
	}
}
