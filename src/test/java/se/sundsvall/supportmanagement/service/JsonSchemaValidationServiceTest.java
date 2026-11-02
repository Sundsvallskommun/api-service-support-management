package se.sundsvall.supportmanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.exception.ClientProblem;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class JsonSchemaValidationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String SCHEMA_ID = "schema-id";

	@Mock
	private JsonSchemaClient jsonSchemaClientMock;

	@InjectMocks
	private JsonSchemaValidationService service;

	@Test
	void findViolationWhenValueIsValid() {
		final var value = JsonMapper.builder().build().readTree("{\"key\": \"value\"}");

		assertThat(service.findViolation(MUNICIPALITY_ID, SCHEMA_ID, value)).isEmpty();

		verify(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, SCHEMA_ID, value);
	}

	@Test
	void findViolationWhenSchemaRejectsValue() {
		final var value = JsonMapper.builder().build().readTree("{\"key\": 1}");
		final var rejection = new ClientProblem(BAD_REQUEST, "$.key: integer found, string expected");
		doThrow(rejection).when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, SCHEMA_ID, value);

		assertThat(service.findViolation(MUNICIPALITY_ID, SCHEMA_ID, value)).containsSame(rejection);
	}

	@Test
	void findViolationWhenSchemaServiceFails() {
		final var value = JsonMapper.builder().build().readTree("{\"key\": \"value\"}");
		final var failure = new ServerProblem(BAD_GATEWAY, "json-schema error: {status=503 Service Unavailable}");
		doThrow(failure).when(jsonSchemaClientMock).validateJson(MUNICIPALITY_ID, SCHEMA_ID, value);

		assertThatThrownBy(() -> service.findViolation(MUNICIPALITY_ID, SCHEMA_ID, value)).isSameAs(failure);
	}
}
