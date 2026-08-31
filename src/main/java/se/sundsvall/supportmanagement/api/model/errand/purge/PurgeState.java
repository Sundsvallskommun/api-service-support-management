package se.sundsvall.supportmanagement.api.model.errand.purge;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "State of a purge job")
public enum PurgeState {

	/**
	 * The job is working its way through the errands. Only one job per namespace and municipality may hold this state at
	 * any given time.
	 */
	RUNNING,

	/**
	 * The job reached the end of the eligible errands. Individual errands may still have failed, which is what the failure
	 * counter reports.
	 */
	COMPLETED,

	/**
	 * The job stopped before the end because a stop was requested, or because the configured limit for the run was
	 * reached.
	 */
	STOPPED,

	/**
	 * The job aborted on an error that made it pointless to continue, as opposed to an error on a single errand. The
	 * message carries the reason.
	 */
	FAILED
}
