package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the retention purge.
 *
 * @param minimumAge        floor for how recent a purge cutoff may be, guarding against a mistyped timestamp emptying a
 *                          namespace. Subtracted as a calendar amount, so that the floor lands on the same date of the
 *                          year regardless of leap years.
 * @param batchSize         errands read per batch. Each errand is still removed in a transaction of its own.
 * @param maxConcurrentRuns highest number of runs carried out at the same time, across all namespaces. A namespace has
 *                          at most one run at a time.
 * @param progressInterval  how long a run may go without writing to its job before it reports from inside the batch it
 *                          is on. The check is made after every errand, so a slow batch still writes to its job often
 *                          enough not to be taken as abandoned by {@link JobProperties#staleAfter()}.
 */
@ConfigurationProperties(prefix = "errand.purge")
public record ErrandPurgeProperties(

	@DefaultValue("P2Y") Period minimumAge,

	@DefaultValue("250") int batchSize,

	@DefaultValue("2") int maxConcurrentRuns,

	@DefaultValue("PT1M") Duration progressInterval) {
}
