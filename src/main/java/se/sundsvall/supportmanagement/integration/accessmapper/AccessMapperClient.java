package se.sundsvall.supportmanagement.integration.accessmapper;

import generated.se.sundsvall.accessmapper.AccessGroup;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import se.sundsvall.supportmanagement.integration.accessmapper.configuration.AccessMapperConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.accessmapper.configuration.AccessMapperConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.accessmapper.url}", configuration = AccessMapperConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface AccessMapperClient {

	/**
	 * Everything the access mapper knows about the user within the namespace. The endpoint takes an optional type filter,
	 * which is deliberately not sent: the answer is filtered per type where it is used, and asking once for all of them
	 * keeps labels, roles and resources from being read from three different moments in time.
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/access/ad/{adId}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<List<AccessGroup>> getAccessDetails(
		@PathVariable("municipalityId") String municipalityId,
		@PathVariable("namespace") String namespace,
		@PathVariable("adId") String adId);
}
