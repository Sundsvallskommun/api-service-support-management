package se.sundsvall.supportmanagement.service.model;

import java.util.Map;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;

import static java.util.Collections.emptyMap;

/**
 * What mapping an errand needs beyond the errand row itself.
 * <p>
 * The process of an errand has no JPA relation to the errand. It is read separately - once for a whole page of
 * errands - and handed to the mapper here, keyed by errand id.
 *
 * @param processes the latest process per errand id, holding no entry for an errand that has none.
 */
public record ErrandEnrichment(Map<String, ErrandProcess> processes) {

	private static final ErrandEnrichment EMPTY = new ErrandEnrichment(emptyMap());

	/** Nothing read, which maps an errand without its process. */
	public static ErrandEnrichment empty() {
		return EMPTY;
	}

	public ErrandProcess processOf(final String errandId) {
		return processes.get(errandId);
	}
}
