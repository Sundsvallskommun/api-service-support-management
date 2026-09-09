package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Whether SM gives the start permission to an errand event by itself, or leaves it to a handler pressing a button.
 * <p>
 * Read from the {@code processStartMode} attribute of the same label that carries the process key, and never on its
 * own: an errand whose labels point in two directions would otherwise be able to take the key from one of them and the
 * mode from the other.
 * <p>
 * Not a mapped column. The value is stored as free label metadata, and this enum is what SM is willing to read out of
 * it.
 */
public enum ProcessStartMode {

	/** SM sets the start permission on the first errand event that could start the process. */
	AUTOMATIC,

	/** Only the manual start command sets the permission. */
	MANUAL
}
