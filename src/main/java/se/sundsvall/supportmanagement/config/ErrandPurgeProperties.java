package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the retention purge, gathered in one place so that the defaults cannot drift apart from the values in
 * application.yml the way scattered annotations do.
 *
 * @param minimumAge        floor for how recent a purge cutoff may be, guarding against a mistyped timestamp emptying a
 *                          namespace. Subtracted as a calendar amount rather than a fixed number of days, so that the
 *                          floor lands on the same date of the year regardless of leap years.
 * @param batchSize         errands read per batch. Each errand is still removed in a transaction of its own.
 * @param maxConcurrentRuns highest number of runs carried out at the same time. One run per namespace is already the
 *                          rule, but nothing stops several namespaces from being purged at once, and each run reaches
 *                          into the same database and the same neighbouring services.
 * @param progressInterval  how long a run may go without writing to its job before it reports from inside the batch it
 *                          is on. A batch is normally a minute's work, but one whose neighbouring services have gone
 *                          slow can take far longer, and a job that goes quiet is ended as abandoned. This is what
 *                          keeps the quiet stretch down to a single errand instead of a whole batch, and is therefore
 *                          what lets {@link JobProperties#staleAfter()} be measured in minutes rather than days.
 */
@ConfigurationProperties(prefix = "errand.purge")
public record ErrandPurgeProperties(

	@DefaultValue("P2Y") Period minimumAge,

	@DefaultValue("250") int batchSize,

	@DefaultValue("2") int maxConcurrentRuns,

	@DefaultValue("PT1M") Duration progressInterval) {
}
