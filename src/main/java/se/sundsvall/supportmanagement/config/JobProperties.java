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
 *                   carrying it out. Held far above the time a run needs between two reports rather than close to it. A
 *                   run reports on its job once per batch, and a batch of errands whose neighbouring services have gone
 *                   slow can take hours on its own without anything being wrong - while a job ended here stops the run
 *                   behind it. Days rather than hours, therefore, and the sweep that acts on it runs hourly: waiting
 *                   for a job that has been quiet for three days is not made better by asking every five minutes. A job
 *                   that is genuinely stuck need not wait this out either: stopping it through the resource it belongs
 *                   to ends it there and then.
 */
@ConfigurationProperties(prefix = "job")
public record JobProperties(

	@DefaultValue("P3D") Duration staleAfter) {
}
