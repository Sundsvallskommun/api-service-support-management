package se.sundsvall.supportmanagement.service.purge;

import java.time.OffsetDateTime;

/**
 * What a caller asked a purge run to do, as read from the request, handed to the run and answered in its status.
 *
 * @param olderThan  errands last touched before this point in time are the ones the run removes.
 * @param dryRun     when true the run only counts the errands it would have removed.
 * @param maxErrands highest number of errands the run may handle, or null for a run without a limit.
 */
public record PurgeSettings(OffsetDateTime olderThan, boolean dryRun, Integer maxErrands) {
}
