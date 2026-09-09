package se.sundsvall.supportmanagement.service.model;

import java.util.Map;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;

/**
 * What mapping an errand needs beyond the errand row itself.
 * <p>
 * The process of an errand deliberately has no JPA relation: a revision is a copy of the whole errand, and a report
 * from every work step would fill the revision table with copies saying nothing about the errand. It is therefore read
 * separately - once for a whole page of errands rather than once per errand - and handed to the mapper here, keyed by
 * errand id.
 *
 * @param processes the latest process per errand id, holding no entry for an errand that has none.
 */
public record ErrandEnrichment(Map<String, ErrandProcess> processes) {

	private static final ErrandEnrichment EMPTY = new ErrandEnrichment(emptyMap());

	/** Nothing read, which maps an errand exactly as it did before the process field existed. */
	public static ErrandEnrichment empty() {
		return EMPTY;
	}

	public static ErrandEnrichment of(final Map<String, ErrandProcess> processes) {
		return isNull(processes) || processes.isEmpty() ? EMPTY : new ErrandEnrichment(processes);
	}

	public ErrandProcess processOf(final String errandId) {
		return isNull(errandId) ? null : processes.get(errandId);
	}
}
