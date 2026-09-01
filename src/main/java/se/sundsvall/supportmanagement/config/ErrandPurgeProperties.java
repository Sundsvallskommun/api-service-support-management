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
 * @param jobRetention      how long a finished run stays readable before it is dropped from the in memory registry.
 * @param maxConcurrentRuns highest number of runs carried out at the same time. One run per namespace is already the
 *                          rule, but nothing stops several namespaces from being purged at once, and each run reaches
 *                          into the same database and the same neighbouring services.
 */
@ConfigurationProperties(prefix = "errand.purge")
public record ErrandPurgeProperties(

	@DefaultValue("P2Y") Period minimumAge,

	@DefaultValue("250") int batchSize,

	@DefaultValue("PT24H") Duration jobRetention,

	@DefaultValue("2") int maxConcurrentRuns) {
}
