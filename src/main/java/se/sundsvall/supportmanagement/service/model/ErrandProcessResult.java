package se.sundsvall.supportmanagement.service.model;

import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;

/**
 * A written process, and whether writing it brought it into being.
 * <p>
 * Both write paths answer differently for a row that was created than for one that was already there, and neither the
 * state nor the report says which happened - only the write itself knows.
 *
 * @param process the process as it stands after the write.
 * @param created whether the write created the row.
 */
public record ErrandProcessResult(ErrandProcess process, boolean created) {
}
