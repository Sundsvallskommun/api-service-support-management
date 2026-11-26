package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the job table, which every long running piece of work reports against.
 *
 * @param staleAfter how long a job may go without being written to before it is taken to have ended with the instance
 *                   carrying it out. Measured against how often a run reports: a run writes to its job on a timer of
 *                   its own ({@link ErrandPurgeProperties#progressInterval()}) and not only when a batch ends. Until
 *                   then, a job left by an instance that is gone rules out another run of its kind in the same
 *                   namespace.
 * @param retention  how long a job that has ended is kept after it was created. The job is the record of what its run
 *                   did, of a purge in particular, and is removed by the hourly maintenance of the job table once it is
 *                   older than this. A job still under way is kept whatever its age.
 */
@ConfigurationProperties(prefix = "job")
public record JobProperties(
	@DefaultValue("PT30M") Duration staleAfter,
	@DefaultValue("P90D") Duration retention) {
}
