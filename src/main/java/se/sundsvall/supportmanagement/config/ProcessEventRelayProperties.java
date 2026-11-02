package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the relay that hands the rows of the process event outbox to the process engine.
 *
 * @param batchSize         how many rows one scheduled run tries to deliver at most, and how many rows of one errand
 *                          a direct run takes.
 * @param transactionBuffer how old a row must be before the scheduled run takes it, which gives a transaction still
 *                          being committed time to finish. The direct run is not held to it.
 * @param maxAge            how old an undelivered row may get before it is dropped undelivered, which is logged as an
 *                          error.
 * @param unhealthyAfter    how old the oldest undelivered row may get before the relay reports itself unhealthy.
 */
@ConfigurationProperties(prefix = "scheduler.process-event")
public record ProcessEventRelayProperties(

	@DefaultValue("200") int batchSize,

	@DefaultValue("PT5S") Duration transactionBuffer,

	@DefaultValue("P30D") Duration maxAge,

	@DefaultValue("PT15M") Duration unhealthyAfter) {
}
