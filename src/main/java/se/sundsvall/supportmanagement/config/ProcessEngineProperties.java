package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the process engine that errand events are published to.
 *
 * @param loopGuard the emergency brake against two services waking each other for ever
 * @param directRun the delivery a publication starts as soon as its transaction is committed
 */
@ConfigurationProperties(prefix = "process-engine")
public record ProcessEngineProperties(

	@DefaultValue LoopGuard loopGuard,

	@DefaultValue DirectRun directRun) {

	/**
	 * @param maxEventsPerErrand how many events one errand may have delivered to its process within the window before
	 *                           further events are dropped. Only delivered events are counted, since counting the ones
	 *                           still waiting would let a delivery outage trip the brake by itself and turn lost time
	 *                           into lost events.
	 * @param window             how far back the count reaches.
	 */
	public record LoopGuard(

		@DefaultValue("20") int maxEventsPerErrand,

		@DefaultValue("PT10M") Duration window) {
	}

	/**
	 * The direct run only brings a delivery forward: the scheduled run delivers every row a direct run did not, within a
	 * minute. That is why a direct run arriving at a full pool is dropped rather than waited for.
	 *
	 * @param enabled       whether a publication starts a delivery of its own once its transaction is committed. Off,
	 *                      every row waits for the scheduled run.
	 * @param corePoolSize  the threads kept for direct runs.
	 * @param maxPoolSize   the threads the pool grows to once the queue is full.
	 * @param queueCapacity how many direct runs may wait for a thread before further ones are dropped.
	 */
	public record DirectRun(

		@DefaultValue("true") boolean enabled,

		@DefaultValue("2") int corePoolSize,

		@DefaultValue("4") int maxPoolSize,

		@DefaultValue("500") int queueCapacity) {
	}
}
