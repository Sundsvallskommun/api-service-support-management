package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_MUNICIPALITY_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.zalando.problem.Status;
import se.sundsvall.dept44.exception.ClientProblem;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;

@ExtendWith(MockitoExtension.class)
class ValidJsonParametersConstraintValidatorTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private JsonSchemaClient jsonSchemaClientMock;

	@Mock
	private RequestAttributes requestAttributesMock;

	@InjectMocks
	private ValidJsonParametersConstraintValidator validator;

	@Test
	void validWithNullList() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
		verifyNoInteractions(jsonSchemaClientMock);
	}

	@Test
	void validWithEmptyList() {
		assertThat(validator.isValid(emptyList(), constraintValidatorContextMock)).isTrue();
		verifyNoInteractions(jsonSchemaClientMock);
	}

	@Test
	void validJsonParameters() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID);
		final var schemaId = "testSchema";
		final var jsonValue = createJsonNode();
		final var jsonParameter = JsonParameter.create()
			.withKey("testKey")
			.withSchemaId(schemaId)
			.withValue(jsonValue);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);

			assertThat(validator.isValid(List.of(jsonParameter), constraintValidatorContextMock)).isTrue();
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);
		}
	}

	@Test
	void invalidJsonParametersClientProblem() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID);
		final var schemaId = "testSchema";
		final var jsonValue = createJsonNode();
		final var jsonParameter = JsonParameter.create()
			.withKey("testKey")
			.withSchemaId(schemaId)
			.withValue(jsonValue);

		final var clientProblem = new ClientProblem(Status.BAD_REQUEST, "error details");

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			doThrow(clientProblem).when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);

			assertThat(validator.isValid(List.of(jsonParameter), constraintValidatorContextMock)).isFalse();
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);
			verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("error details");
		}
	}

	@Test
	void invalidJsonParametersClientProblemWithoutDetail() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID);
		final var schemaId = "testSchema";
		final var jsonValue = createJsonNode();
		final var jsonParameter = JsonParameter.create()
			.withKey("testKey")
			.withSchemaId(schemaId)
			.withValue(jsonValue);

		final var clientProblem = new ClientProblem(Status.BAD_REQUEST, null);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			doThrow(clientProblem).when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);

			assertThat(validator.isValid(List.of(jsonParameter), constraintValidatorContextMock)).isFalse();
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);
			verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("validation failed for schema 'testSchema'");
		}
	}

	@Test
	void invalidJsonParametersThrowableProblem() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID);
		final var schemaId = "testSchema";
		final var jsonValue = createJsonNode();
		final var jsonParameter = JsonParameter.create()
			.withKey("testKey")
			.withSchemaId(schemaId)
			.withValue(jsonValue);

		final var clientProblem = new ClientProblem(Status.BAD_REQUEST, "Schema validation error");

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			doThrow(clientProblem).when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);

			assertThat(validator.isValid(List.of(jsonParameter), constraintValidatorContextMock)).isFalse();
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);
			verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("Schema validation error");
		}
	}

	@Test
	void serverProblemIsHandledAsValidationError() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID);
		final var schemaId = "testSchema";
		final var jsonValue = createJsonNode();
		final var jsonParameter = JsonParameter.create()
			.withKey("testKey")
			.withSchemaId(schemaId)
			.withValue(jsonValue);

		final var serverProblem = new ServerProblem(Status.INTERNAL_SERVER_ERROR, "Internal server error");

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			doThrow(serverProblem).when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);

			assertThat(validator.isValid(List.of(jsonParameter), constraintValidatorContextMock)).isFalse();
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);
			verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("validation could not be performed for schema 'testSchema', the JsonSchema service may be experiencing issues");
		}
	}

	@Test
	void validatesMultipleParameters() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID);
		final var schemaId1 = "schema1";
		final var schemaId2 = "schema2";
		final var jsonValue1 = createJsonNode();
		final var jsonValue2 = createJsonNode();

		final var jsonParameter1 = JsonParameter.create()
			.withKey("key1")
			.withSchemaId(schemaId1)
			.withValue(jsonValue1);

		final var jsonParameter2 = JsonParameter.create()
			.withKey("key2")
			.withSchemaId(schemaId2)
			.withValue(jsonValue2);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);

			assertThat(validator.isValid(List.of(jsonParameter1, jsonParameter2), constraintValidatorContextMock)).isTrue();
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId1, jsonValue1);
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId2, jsonValue2);
		}
	}

	@Test
	void validatesAllParametersEvenWhenSomeFail() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID);
		final var schemaId1 = "schema1";
		final var schemaId2 = "schema2";
		final var schemaId3 = "schema3";
		final var jsonValue1 = createJsonNode();
		final var jsonValue2 = createJsonNode();
		final var jsonValue3 = createJsonNode();

		final var jsonParameter1 = JsonParameter.create()
			.withKey("key1")
			.withSchemaId(schemaId1)
			.withValue(jsonValue1);

		final var jsonParameter2 = JsonParameter.create()
			.withKey("key2")
			.withSchemaId(schemaId2)
			.withValue(jsonValue2);

		final var jsonParameter3 = JsonParameter.create()
			.withKey("key3")
			.withSchemaId(schemaId3)
			.withValue(jsonValue3);

		final var clientProblem1 = new ClientProblem(Status.BAD_REQUEST, "error for schema1");
		final var clientProblem3 = new ClientProblem(Status.BAD_REQUEST, "error for schema3");

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			doThrow(clientProblem1).when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId1, jsonValue1);
			doNothing().when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId2, jsonValue2);
			doThrow(clientProblem3).when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId3, jsonValue3);

			assertThat(validator.isValid(List.of(jsonParameter1, jsonParameter2, jsonParameter3), constraintValidatorContextMock)).isFalse();

			// Verify all three parameters were validated
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId1, jsonValue1);
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId2, jsonValue2);
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId3, jsonValue3);

			// Verify constraint violations were added for both failing parameters
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("error for schema1");
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("error for schema3");
		}
	}

	@Test
	void invalidWithDuplicateKeys() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID);
		final var jsonValue1 = createJsonNode();
		final var jsonValue2 = createJsonNode();
		final var jsonValue3 = createJsonNode();

		final var jsonParameter1 = JsonParameter.create()
			.withKey("duplicateKey")
			.withSchemaId("schema1")
			.withValue(jsonValue1);

		final var jsonParameter2 = JsonParameter.create()
			.withKey("uniqueKey")
			.withSchemaId("schema2")
			.withValue(jsonValue2);

		final var jsonParameter3 = JsonParameter.create()
			.withKey("duplicateKey")
			.withSchemaId("schema3")
			.withValue(jsonValue3);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);

			assertThat(validator.isValid(List.of(jsonParameter1, jsonParameter2, jsonParameter3), constraintValidatorContextMock)).isFalse();

			// Verify that only the unique key was validated against schema
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, "schema2", jsonValue2);
			verifyNoMoreInteractions(jsonSchemaClientMock);

			// Verify constraint violations were added for both occurrences of duplicate keys
			verify(constraintValidatorContextMock, times(2)).buildConstraintViolationWithTemplate("duplicate key 'duplicateKey'");
		}
	}

	private ObjectNode createJsonNode() {
		return OBJECT_MAPPER
			.createObjectNode()
			.put("testField", "testValue");
	}
}
