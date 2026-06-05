package se.sundsvall.supportmanagement.integration.jsonschema;

import generated.se.sundsvall.jsonschema.JsonSchema;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.supportmanagement.integration.jsonschema.configuration.JsonSchemaConfiguration;
import tools.jackson.databind.JsonNode;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.jsonschema.configuration.JsonSchemaConfiguration.CLIENT_ID;

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

	/**
	 * Fetches a JSON schema by its id. Read-only and side-effect free (unlike {@link #validateJson}, which increments the
	 * schema's validation usage counter), making it suitable for checking whether a schema is registered in a namespace.
	 *
	 * <p>
	 * A {@code 404 Not Found} is raised as a {@code NOT_FOUND} problem (the client bypasses 404 in its error decoder rather
	 * than wrapping it in {@code BAD_GATEWAY}), so callers can tell "schema not registered" apart from a genuine upstream
	 * error.
	 * </p>
	 *
	 * @param  municipalityId the municipality identifier
	 * @param  schemaId       the unique identifier of the JSON schema to fetch
	 * @return                the JSON schema, if it exists
	 */
	@GetMapping(path = "/{municipalityId}/schemas/{schemaId}", produces = APPLICATION_JSON_VALUE)
	JsonSchema getSchemaById(
		@PathVariable String municipalityId,
		@PathVariable String schemaId);
}
