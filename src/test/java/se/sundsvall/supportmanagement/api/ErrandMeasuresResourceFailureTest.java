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
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.service.ErrandMeasureService;

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
class ErrandMeasuresResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/measures";
	private static final String PATH_WITH_ID = "/{municipalityId}/{namespace}/errands/{errandId}/measures/{measureId}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String INVALID = "invalid";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandMeasureService serviceMock;

	@Test
	void createErrandMeasureWithBlankFields() {

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(new Measure().withType(" ").withAddedByUser(" ").withAddedByRole(" "))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(
				tuple("type", "must not be blank"),
				tuple("addedByUser", "must not be blank"),
				tuple("addedByRole", "must not be blank"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrandMeasureWithNullFields() {

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(new Measure())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(
				tuple("type", "must not be blank"),
				tuple("addedByUser", "must not be blank"),
				tuple("addedByRole", "must not be blank"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrandMeasureInvalidNamespace() {

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", "invalid!", "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(new Measure().withType("INTERVENTION").withAddedByUser("jo12doe").withAddedByRole("MANAGER"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("createErrandMeasure.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrandMeasureInvalidMunicipalityId() {

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(new Measure().withType("INTERVENTION").withAddedByUser("jo12doe").withAddedByRole("MANAGER"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("createErrandMeasure.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrandMeasureInvalidErrandId() {

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(new Measure().withType("INTERVENTION").withAddedByUser("jo12doe").withAddedByRole("MANAGER"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("createErrandMeasure.errandId", "not a valid UUID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readErrandMeasureInvalidMeasureId() {

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_WITH_ID).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "measureId", INVALID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("readErrandMeasure.measureId", "not a valid UUID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateErrandMeasureInvalidMeasureId() {

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "measureId", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(new Measure().withType("INTERVENTION").withAddedByUser("jo12doe").withAddedByRole("MANAGER"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("updateErrandMeasure.measureId", "not a valid UUID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteErrandMeasureInvalidMeasureId() {

		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH_WITH_ID).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "measureId", INVALID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("deleteErrandMeasure.measureId", "not a valid UUID"));

		verifyNoInteractions(serviceMock);
	}

	/**
	 * The patch body is validated exactly as the create body is. Without that, an unknown accept value reaches the mapper
	 * and comes back as a 500 rather than the bad request it is.
	 */
	@Test
	void updateErrandMeasureWithInvalidAccept() {

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH_WITH_ID).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "measureId", randomUUID().toString())))
			.contentType(APPLICATION_JSON)
			.bodyValue(new Measure().withAccept("MAYBE"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		// Only accept - a patch says nothing about the fields it omits, so the create time requirements do not apply
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("accept", "must be one of: [TRUE, FALSE, REWORK]"));

		verifyNoInteractions(serviceMock);
	}
}
