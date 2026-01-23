package se.sundsvall.supportmanagement.integration.jsonschema;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.jsonschema.configuration.JsonSchemaConfiguration.CLIENT_ID;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.supportmanagement.integration.jsonschema.configuration.JsonSchemaConfiguration;

/**
 * Feign client for integrating with the JSON Schema service.
 * Provides functionality to validate JSON data against predefined schemas.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.json-schema.url}", configuration = JsonSchemaConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface JsonSchemaClient {

	/**
	 * Validates JSON data against a specified schema.
	 *
	 * @param municipalityId the municipality identifier
	 * @param schemaId       the unique identifier of the JSON schema to validate against
	 * @param jsonData       the JSON data to validate
	 */
	@PostMapping(path = "/{municipalityId}/schemas/{schemaId}/validation", consumes = APPLICATION_JSON_VALUE)
	void validateJson(
		@PathVariable String municipalityId,
		@PathVariable String schemaId,
		@RequestBody JsonNode jsonData);
}
