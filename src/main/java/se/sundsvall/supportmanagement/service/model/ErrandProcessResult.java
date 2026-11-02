package se.sundsvall.supportmanagement.service.model;

import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;

/**
 * A written process, and whether writing it brought it into being.
 * <p>
 * Lets both write paths answer differently for a row that was created than for one that was already there.
 *
 * @param process the process as it stands after the write.
 * @param created whether the write created the row.
 */
public record ErrandProcessResult(ErrandProcess process, boolean created) {
}
