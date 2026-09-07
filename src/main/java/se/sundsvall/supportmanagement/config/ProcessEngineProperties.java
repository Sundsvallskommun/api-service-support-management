package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the process engines that errand events are published to.
 *
 * @param consumers the register of known process consumers. The name is the address: the same string is the Feign
 *                  target, the OAuth2 client registration and what the PROCESS_CONSUMER property of a namespace points
 *                  out, so there is no separate identifier that can drift apart from the name it is configured under.
 *                  The register is what lets a mistyped PROCESS_CONSUMER be refused when it is written rather than
 *                  quietly stop working later, and connecting a namespace to a consumer already listed here is the only
 *                  part of adding a process engine that is configuration rather than a release.
 * @param loopGuard the emergency brake against two services waking each other for ever
 */
@ConfigurationProperties(prefix = "process-engine")
public record ProcessEngineProperties(

	@DefaultValue List<String> consumers,

	@DefaultValue LoopGuard loopGuard) {

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
}
