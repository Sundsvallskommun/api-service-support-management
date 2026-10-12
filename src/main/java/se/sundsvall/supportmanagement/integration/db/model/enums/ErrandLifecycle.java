package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Life cycle of an errand, the same in every namespace and independent of the status the namespace gives it.
 */
public enum ErrandLifecycle {

	/**
	 * Being prepared and not yet handled: no process is started or woken and no action is created. An errand leaves the
	 * draft by becoming active, and never returns to it.
	 */
	DRAFT,

	/** Handled as any errand is. */
	ACTIVE
}
