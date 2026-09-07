package se.sundsvall.supportmanagement.service.purge;

import java.time.OffsetDateTime;

/**
 * What a caller asked a run to do. Kept together because the three always travel as one: they are read from the same
 * request, handed to the same run and answered in the same status.
 *
 * @param olderThan  errands last touched before this point in time are the ones the run removes.
 * @param dryRun     when true the run only counts the errands it would have removed.
 * @param maxErrands highest number of errands the run may handle, or null for a run without a limit.
 */
public record PurgeSettings(OffsetDateTime olderThan, boolean dryRun, Integer maxErrands) {
}
