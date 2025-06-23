package se.sundsvall.supportmanagement.integration.relation;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.relation.configuration.RelationConfiguration.CLIENT_ID;

import generated.se.sundsvall.relation.Relation;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import se.sundsvall.supportmanagement.integration.relation.configuration.RelationConfiguration;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.relation.url}",
	configuration = RelationConfiguration.class,
	dismiss404 = true)
@CircuitBreaker(name = CLIENT_ID)
public interface RelationClient {

	@GetMapping(path = "/{municipalityId}/relations/{id}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Relation> getRelation(
		@PathVariable("municipalityId") final String municipalityId,
		@PathVariable("id") final String id);
}
