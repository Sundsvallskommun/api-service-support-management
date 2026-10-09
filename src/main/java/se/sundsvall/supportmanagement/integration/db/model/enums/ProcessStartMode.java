package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Whether SM gives the start permission to an errand event by itself, or leaves it to a handler pressing a button.
 * <p>
 * Read from the {@code processStartMode} attribute of the same label that carries the process key, and never on its
 * own.
 * <p>
 * Not a mapped column. The value is stored as free label metadata, and this enum holds the values SM reads out of it.
 */
public enum ProcessStartMode {

	/** SM sets the start permission on the first errand event that could start the process. */
	AUTOMATIC,

	/** Only the manual start command sets the permission. */
	MANUAL
}
