package se.sundsvall.supportmanagement.service.model;

import java.util.List;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode;

/**
 * What the labels of an errand say about which process it runs, and about who may start it.
 * <p>
 * The key and the start mode are handed back together because they have to come from the same label. Two lookups could
 * take the key from one label of an ambiguous errand and the mode from the other, and the pair is what keeps that from
 * being expressible.
 *
 * @param processKey the one key the labels resolve to, or null when they resolve to none or to more than one.
 * @param startMode  the start mode of the label that gave the key, and null whenever the key is.
 * @param keys       every distinct key found among the labels. Holds both of them when the errand is ambiguous, which
 *                   is what lets the activity log name what has to be untangled.
 */
public record ProcessKeySelection(String processKey, ProcessStartMode startMode, List<String> keys) {

	/** No label of the errand carries a process key. Not an error - the errand simply runs no process. */
	public static final ProcessKeySelection NONE = new ProcessKeySelection(null, null, List.of());

	public boolean isAmbiguous() {
		return keys.size() > 1;
	}
}
