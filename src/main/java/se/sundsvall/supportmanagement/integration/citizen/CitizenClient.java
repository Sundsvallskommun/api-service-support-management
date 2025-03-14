package se.sundsvall.supportmanagement.integration.citizen;

import static org.springframework.http.MediaType.ALL_VALUE;
import static se.sundsvall.supportmanagement.integration.citizen.configuration.CitizenConfiguration.CLIENT_ID;

import generated.se.sundsvall.citizen.CitizenExtended;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import se.sundsvall.supportmanagement.integration.citizen.configuration.CitizenConfiguration;

@FeignClient(
	name = CLIENT_ID,
	configuration = CitizenConfiguration.class,
	url = "${integration.citizen.url}")
@CircuitBreaker(name = CLIENT_ID)
public interface CitizenClient {

	@GetMapping(path = "/{municipalityId}/{personId}", produces = ALL_VALUE)
	CitizenExtended getPerson(
		@PathVariable("municipalityId") final String municipalityId,
		@PathVariable("personId") final String personId);
}
