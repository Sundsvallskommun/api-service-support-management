package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService;
import tools.jackson.databind.node.JsonNodeFactory;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandJsonParameterResourceFailureTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String KEY = "formData";
	private static final String INVALID = "#invalid#";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/json-parameters";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandJsonParameterService errandJsonParameterServiceMock;

	@Test
	void readJsonParameterInvalidNamespace() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/{key}").build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "key", KEY)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting("field", "message")
			.containsExactlyInAnyOrder(tuple("readJsonParameter.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(errandJsonParameterServiceMock);
	}

	@Test
	void readJsonParameterInvalidErrandId() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/{key}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID, "key", KEY)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting("field", "message")
			.containsExactlyInAnyOrder(tuple("readJsonParameter.errandId", "not a valid UUID"));

		verifyNoInteractions(errandJsonParameterServiceMock);
	}

	@Test
	void updateJsonParameterInvalidNamespace() {
		final var requestBody = JsonParameter.create()
			.withKey(KEY)
			.withSchemaId("test-schema-1.0")
			.withValue(JsonNodeFactory.instance.objectNode().put("name", "test"));

		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{key}").build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "key", KEY)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting("field", "message")
			.containsExactlyInAnyOrder(tuple("updateJsonParameter.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(errandJsonParameterServiceMock);
	}

	@Test
	void updateJsonParameterMissingBody() {
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{key}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "key", KEY)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();

		verifyNoInteractions(errandJsonParameterServiceMock);
	}

	@Test
	void deleteJsonParameterInvalidNamespace() {
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{key}").build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "key", KEY)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting("field", "message")
			.containsExactlyInAnyOrder(tuple("deleteJsonParameter.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(errandJsonParameterServiceMock);
	}
}
