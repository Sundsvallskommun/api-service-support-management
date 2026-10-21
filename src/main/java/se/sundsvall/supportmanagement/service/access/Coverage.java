package se.sundsvall.supportmanagement.service.access;

/**
 * The three ways a user may hold an errand, since each of them answers with a field set of its own.
 */
public enum Coverage {
	/** The labels of the user cover the errand at read or read/write. */
	FULL,
	/** Their labels reach the errand, but only at limited read. */
	LIMITED,
	/** No label of theirs reaches the errand, which leaves its reporter holding it as its reporter alone. */
	REPORTER_ONLY
}
