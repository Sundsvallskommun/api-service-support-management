package se.sundsvall.supportmanagement.integration.jsonschema;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;

@ExtendWith(MockitoExtension.class)
class JsonSchemaIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Mock
	private JsonSchemaClient jsonSchemaClientMock;

	@InjectMocks
	private JsonSchemaIntegration jsonSchemaIntegration;

	@Test
	void validateJsonParametersWithValidDataCallsClient() {
		// Arrange
		final var schemaId = "testSchema";
		final var jsonValue = createJsonNode();
		final var jsonParameters = List.of(JsonParameter.create()
			.withKey("testKey")
			.withSchemaId(schemaId)
			.withValue(jsonValue));

		// Act
		jsonSchemaIntegration.validateJsonParameters(MUNICIPALITY_ID, jsonParameters);

		// Assert
		verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);
	}

	@Test
	void validateJsonParametersWithNullListDoesNotCallClient() {
		// Act
		jsonSchemaIntegration.validateJsonParameters(MUNICIPALITY_ID, null);

		// Assert
		verifyNoInteractions(jsonSchemaClientMock);
	}

	@Test
	void validateJsonParametersWithEmptyListDoesNotCallClient() {
		// Act
		jsonSchemaIntegration.validateJsonParameters(MUNICIPALITY_ID, emptyList());

		// Assert
		verifyNoInteractions(jsonSchemaClientMock);
	}

	@Test
	void validateJsonParametersWithMultipleParamsCallsClientForEach() {
		// Arrange
		final var schemaId1 = "schema1";
		final var schemaId2 = "schema2";
		final var jsonValue1 = createJsonNode();
		final var jsonValue2 = createJsonNode();
		final var jsonParameters = List.of(
			JsonParameter.create()
				.withKey("key1")
				.withSchemaId(schemaId1)
				.withValue(jsonValue1),
			JsonParameter.create()
				.withKey("key2")
				.withSchemaId(schemaId2)
				.withValue(jsonValue2));

		// Act
		jsonSchemaIntegration.validateJsonParameters(MUNICIPALITY_ID, jsonParameters);

		// Assert
		verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId1, jsonValue1);
		verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId2, jsonValue2);
	}

	@Test
	void validateJsonParametersWhenValidationFailsThrowsException() {
		// Arrange
		final var schemaId = "testSchema";
		final var jsonValue = createJsonNode();
		final var jsonParameters = List.of(JsonParameter.create()
			.withKey("testKey")
			.withSchemaId(schemaId)
			.withValue(jsonValue));

		final var request = Request.create(Request.HttpMethod.POST, "url", Map.of(), null, new RequestTemplate());
		final var feignException = new FeignException.BadRequest("Validation failed", request, null, null);

		doThrow(feignException)
			.when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);

		// Act and assert
		assertThatThrownBy(() -> jsonSchemaIntegration.validateJsonParameters(MUNICIPALITY_ID, jsonParameters))
			.isInstanceOf(FeignException.class)
			.hasMessageContaining("Validation failed");
	}

	private ObjectNode createJsonNode() {
		return OBJECT_MAPPER
			.createObjectNode()
			.put("testField", "testValue");
	}
}
