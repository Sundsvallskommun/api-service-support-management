package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the nightly cleanup of the process integration.
 *
 * @param batchSize         how many rows one delete removes. Each batch is a transaction of its own.
 * @param activityRetention how long an entry of the activity log is kept.
 */
@ConfigurationProperties(prefix = "scheduler.process-cleanup")
public record ProcessEventCleanupProperties(

	@DefaultValue("1000") int batchSize,

	@DefaultValue("P365D") Duration activityRetention) {
}
