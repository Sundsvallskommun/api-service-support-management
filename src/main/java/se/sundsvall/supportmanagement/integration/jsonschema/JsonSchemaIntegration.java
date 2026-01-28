package se.sundsvall.supportmanagement.integration.jsonschema;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;

/**
 * Integration component for validating JSON parameters against schemas.
 * Delegates to JsonSchemaClient for actual validation.
 */
@Component
public class JsonSchemaIntegration {

	private final JsonSchemaClient jsonSchemaClient;

	public JsonSchemaIntegration(final JsonSchemaClient jsonSchemaClient) {
		this.jsonSchemaClient = jsonSchemaClient;
	}

	/**
	 * Validates a list of JSON parameters against their respective schemas.
	 *
	 * @param municipalityId the municipality identifier
	 * @param jsonParameters the list of JSON parameters to validate
	 */
	public void validateJsonParameters(final String municipalityId, final List<JsonParameter> jsonParameters) {
		if (isEmpty(jsonParameters)) {
			return;
		}
		for (final JsonParameter param : jsonParameters) {
			jsonSchemaClient.validateJson(municipalityId, param.getSchemaId(), param.getValue());
		}
	}
}
