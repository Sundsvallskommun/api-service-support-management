package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the job table, which every long running piece of work reports against.
 * <p>
 * Nothing here says how long a job is kept, because a job is kept. What a run leaves behind is one small row, and for a
 * purge that row is the only lasting record that a few hundred thousand errands were removed on purpose. That is worth
 * more than the space it takes.
 *
 * @param staleAfter how long a job may go without being written to before it is taken to have ended with the instance
 *                   carrying it out. Measured against how often a run reports rather than against how long the work
 *                   takes: a run writes to its job on a timer of its own
 *                   ({@link ErrandPurgeProperties#progressInterval()}) and not only when a batch ends, so a run that is
 *                   merely slow keeps saying so. Silence for many times that interval is therefore an instance that is
 *                   gone rather than work that is taking its time. Kept short for a reason - a job under way rules out
 *                   another run of its kind in the same namespace, so every minute this is set to is a minute a
 *                   namespace stays blocked by a run that ended with its instance. Ending a live run early costs
 *                   little by comparison: it stops at the next batch, having removed what it removed, and can be
 *                   started again.
 */
@ConfigurationProperties(prefix = "job")
public record JobProperties(

	@DefaultValue("PT30M") Duration staleAfter) {
}
