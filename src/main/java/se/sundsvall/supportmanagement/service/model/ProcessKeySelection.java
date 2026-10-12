package se.sundsvall.supportmanagement.service.model;

import java.util.List;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode;

/**
 * What the labels of an errand say about which process it runs, and about who may start it.
 * <p>
 * The key and the start mode are handed back together and always come from the same label.
 *
 * @param processKey the one key the labels resolve to, or null when they resolve to none or to more than one.
 * @param startMode  the start mode of the label that gave the key, and null whenever the key is.
 * @param keys       every distinct key found among the labels. Holds all of them when the errand is ambiguous, for the
 *                   activity log to name.
 */
public record ProcessKeySelection(String processKey, ProcessStartMode startMode, List<String> keys) {

	/** No label of the errand carries a process key. Not an error - the errand simply runs no process. */
	public static final ProcessKeySelection NONE = new ProcessKeySelection(null, null, List.of());

	public boolean isAmbiguous() {
		return keys.size() > 1;
	}
}
