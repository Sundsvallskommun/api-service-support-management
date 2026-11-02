package se.sundsvall.supportmanagement.service.model;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.process.ProcessStartability;

/**
 * Whether a process may be started for an errand, and with which keys. Read both by the {@code startable} field of the
 * process overview and by the start command.
 *
 * @param status      whether a start is possible, and why not when it is not.
 * @param processKeys the keys a start may name. At least one when the status is {@link ProcessStartability#AVAILABLE},
 *                    and always empty otherwise.
 */
public record ProcessStartOptions(ProcessStartability status, List<String> processKeys) {

	/**
	 * No start is possible, for the reason given.
	 *
	 * @param  status why no process may be started.
	 * @return        the answer, naming no key.
	 */
	public static ProcessStartOptions unavailable(final ProcessStartability status) {
		return new ProcessStartOptions(status, List.of());
	}
}
