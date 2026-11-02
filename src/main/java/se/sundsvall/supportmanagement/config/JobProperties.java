package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the job table, which every long running piece of work reports against. Jobs are never removed, so
 * there is no retention setting.
 *
 * @param staleAfter how long a job may go without being written to before it is taken to have ended with the instance
 *                   carrying it out. Measured against how often a run reports: a run writes to its job on a timer of
 *                   its own ({@link ErrandPurgeProperties#progressInterval()}) and not only when a batch ends. Until
 *                   then, a job left by an instance that is gone rules out another run of its kind in the same
 *                   namespace.
 */
@ConfigurationProperties(prefix = "job")
public record JobProperties(

	@DefaultValue("PT30M") Duration staleAfter) {
}
