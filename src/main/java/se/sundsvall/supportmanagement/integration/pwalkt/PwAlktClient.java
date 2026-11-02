package se.sundsvall.supportmanagement.integration.pwalkt;

import generated.se.sundsvall.pwalkt.ErrandEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.supportmanagement.integration.pwalkt.configuration.PwAlktConfiguration;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.pwalkt.configuration.PwAlktConfiguration.CLIENT_ID;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.pw-alkt.url}",
	configuration = PwAlktConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface PwAlktClient {

	/**
	 * Hand an errand event to pw-alkt, which starts, wakes or deletes the process of the errand
	 *
	 * @param  municipalityId the municipalityId of the errand
	 * @param  namespace      the namespace of the errand
	 * @param  errandEvent    the event, which carries nothing out of the errand
	 * @return                the response entity with no content, once pw-alkt has taken the event in or deliberately
	 *                        ignored it
	 */
	@PostMapping(path = "/{municipalityId}/{namespace}/process/errand-events", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	ResponseEntity<Void> handleErrandEvent(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@RequestBody final ErrandEvent errandEvent);
}
