package se.sundsvall.supportmanagement.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;
import tools.jackson.databind.JsonNode;

/**
 * Validates JSON values against the schemas registered in the JSON schema service.
 */
@Service
public class JsonSchemaValidationService {

	private final JsonSchemaClient jsonSchemaClient;

	public JsonSchemaValidationService(final JsonSchemaClient jsonSchemaClient) {
		this.jsonSchemaClient = jsonSchemaClient;
	}

	/**
	 * Validates sent in value against the schema with sent in id.
	 * <p>
	 * A value the schema rejects is answered with the problem the schema service gave, so that the caller can tell why. A
	 * failure of the schema service itself is thrown, since it says nothing about the value.
	 *
	 * @param  municipalityId the municipality the schema belongs to
	 * @param  schemaId       id of the schema to validate against
	 * @param  value          the value to validate
	 * @return                the problem the schema service rejected the value with, or empty if the value is valid
	 */
	public Optional<ThrowableProblem> findViolation(final String municipalityId, final String schemaId, final JsonNode value) {
		try {
			jsonSchemaClient.validateJson(municipalityId, schemaId, value);
			return Optional.empty();
		} catch (final ServerProblem e) {
			throw e;
		} catch (final ThrowableProblem e) {
			return Optional.of(e);
		}
	}
}
