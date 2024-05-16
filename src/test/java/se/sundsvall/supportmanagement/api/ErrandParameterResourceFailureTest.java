package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.parameter.ErrandParameter;
import se.sundsvall.supportmanagement.service.ErrandParameterService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandParameterResourceFailureTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PARAMETER_ID = randomUUID().toString();
	private static final String INVALID = "#invalid#";
	private static final String PATH = "/{namespace}/{municipalityId}/errands/{errandId}/parameters";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandParameterService errandParameterServiceMock;

	@Test
	void createErrandParameterInvalidNamespace() {
		var requestBody = ErrandParameter.create()
			.withName("name")
			.withValue("value");

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("createErrandParameter.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(errandParameterServiceMock);
	}


	@Test
	void createErrandParameterInvalidMunicipalityId() {
		var requestBody = ErrandParameter.create()
			.withName("name")
			.withValue("value");

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("createErrandParameter.municipalityId", "not a valid municipality ID"));


		verifyNoInteractions(errandParameterServiceMock);
	}


	@Test
	void createErrandParameterInvalidId() {
		var requestBody = ErrandParameter.create()
			.withName("name")
			.withValue("value");

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("createErrandParameter.errandId", "not a valid UUID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void createErrandParameterInvalidRequestBody() {

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(ErrandParameter.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("name", "must not be blank"),
				tuple("value", "must not be blank"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void readErrandParameterWithInvalidNamespace() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("readErrandParameter.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void readErrandParameterWithInvalidMunicipalityId() {

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("readErrandParameter.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void readErrandParameterWithInvalidId() {

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("readErrandParameter.errandId", "not a valid UUID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void readErrandParameterWithInvalidParameterId() {

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterId", INVALID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("readErrandParameter.parameterId", "not a valid UUID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void findErrandParametersWithInvalidNamespace() {

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).queryParam("limit", "1").build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("findErrandParameters.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void findErrandParametersWithInvalidMunicipalityId() {

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("findErrandParameters.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void updateErrandParameterInvalidNamespace() {

		final var requestBody = ErrandParameter.create()
			.withName("name")
			.withValue("value");

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("updateErrandParameter.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void updateErrandParameterInvalidMunicipalityId() {

		final var requestBody = ErrandParameter.create()
			.withName("name")
			.withValue("value");

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("updateErrandParameter.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void updateErrandParameterInvalidErrandId() {

		final var requestBody = ErrandParameter.create()
			.withName("name")
			.withValue("value");

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID, "parameterId", PARAMETER_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("updateErrandParameter.errandId", "not a valid UUID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void updateErrandParameterInvalidParameterId() {

		final var requestBody = ErrandParameter.create()
			.withName("name")
			.withValue("value");

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterId", INVALID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("updateErrandParameter.parameterId", "not a valid UUID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void updateErrandParameterInvalidRequestBody() {

		final var requestBody = ErrandParameter.create();

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("name", "must not be blank"),
				tuple("value", "must not be blank"));

		verifyNoInteractions(errandParameterServiceMock);
	}


	@Test
	void deleteErrandParameterWithInvalidNamespace() {

		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("deleteErrandParameter.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void deleteErrandParameterWithInvalidMunicipalityId() {

		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("deleteErrandParameter.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void deleteErrandParameterWithInvalidId() {

		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("deleteErrandParameter.errandId", "not a valid UUID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

	@Test
	void deleteErrandParameterWithInvalidParameterId() {

		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterId", INVALID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("deleteErrandParameter.parameterId", "not a valid UUID"));

		verifyNoInteractions(errandParameterServiceMock);
	}

}
