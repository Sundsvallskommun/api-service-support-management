package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import se.sundsvall.dept44.exception.ClientProblem;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_MUNICIPALITY_ID;

@ExtendWith(MockitoExtension.class)
class ValidJsonParameterConstraintValidatorTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private JsonSchemaClient jsonSchemaClientMock;

	@Mock
	private RequestAttributes requestAttributesMock;

	@InjectMocks
	private ValidJsonParameterConstraintValidator validator;

	@Test
	void validWithNull() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
		verifyNoInteractions(jsonSchemaClientMock);
	}

	@Test
	void validJsonParameter() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID, "key", "testKey");
		final var schemaId = "testSchema";
		final var jsonValue = createJsonNode();
		final var jsonParameter = JsonParameter.create()
			.withKey("testKey")
			.withSchemaId(schemaId)
			.withValue(jsonValue);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);

			assertThat(validator.isValid(jsonParameter, constraintValidatorContextMock)).isTrue();
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);
		}
	}

	@Test
	void keyMismatch() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID, "key", "pathKey");
		final var jsonParameter = JsonParameter.create()
			.withKey("bodyKey")
			.withSchemaId("testSchema")
			.withValue(createJsonNode());

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);

			assertThat(validator.isValid(jsonParameter, constraintValidatorContextMock)).isFalse();
			verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("Key in body 'bodyKey' does not match key in URL path 'pathKey'");
			verifyNoInteractions(jsonSchemaClientMock);
		}
	}

	private static Stream<Arguments> clientProblemTestCases() {
		return Stream.of(
			Arguments.of("error details", "Bad Request: error details"),
			Arguments.of(null, "Bad Request"),
			Arguments.of("Schema validation error", "Bad Request: Schema validation error"));
	}

	@ParameterizedTest
	@MethodSource("clientProblemTestCases")
	void invalidJsonParameterClientProblem(final String problemDetail, final String expectedMessage) {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID, "key", "testKey");
		final var schemaId = "testSchema";
		final var jsonValue = createJsonNode();
		final var jsonParameter = JsonParameter.create()
			.withKey("testKey")
			.withSchemaId(schemaId)
			.withValue(jsonValue);

		final var clientProblem = new ClientProblem(BAD_REQUEST, problemDetail);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			doThrow(clientProblem).when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);

			assertThat(validator.isValid(jsonParameter, constraintValidatorContextMock)).isFalse();
			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);
			verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate(expectedMessage);
			verifyNoMoreInteractions(jsonSchemaClientMock);
		}
	}

	@Test
	void serverProblemIsRethrown() {
		final var attributes = Map.of(PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID, "key", "testKey");
		final var schemaId = "testSchema";
		final var jsonValue = createJsonNode();
		final var jsonParameter = JsonParameter.create()
			.withKey("testKey")
			.withSchemaId(schemaId)
			.withValue(jsonValue);

		final var serverProblem = new ServerProblem(INTERNAL_SERVER_ERROR, "Internal server error");

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			doThrow(serverProblem).when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);

			assertThatThrownBy(() -> validator.isValid(jsonParameter, constraintValidatorContextMock))
				.isSameAs(serverProblem);

			verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, schemaId, jsonValue);
			verifyNoInteractions(constraintValidatorContextMock);
		}
	}

	private ObjectNode createJsonNode() {
		return OBJECT_MAPPER
			.createObjectNode()
			.put("testField", "testValue");
	}
}
