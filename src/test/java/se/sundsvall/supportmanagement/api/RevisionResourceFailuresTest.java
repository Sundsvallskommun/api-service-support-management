package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.supportmanagement.Application;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class RevisionResourceFailuresTest {

	private static final String PATH = "/errands/{id}/revisions";

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getRevisionsByInvalidId() {

		// Parameter values
		final var id = "invalid";

		final var response = webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getRevisionsByErrandId.id", "not a valid UUID"));
	}

	@Test
	void getDifferenceByInvalidId() {

		// Parameter values
		final var id = "invalid";

		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/difference").queryParam("source", 0).queryParam("target", 1).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getDifferenceByVersions.id", "not a valid UUID"));
	}

	@Test
	void getDifferenceNoSourceParameter() {
		// Parameter values
		final var id = UUID.randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/difference").queryParam("target", 0).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("Required request parameter 'source' for method parameter type Integer is not present");
	}

	@Test
	void getDifferenceNoTargetParameter() {
		// Parameter values
		final var id = UUID.randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/difference").queryParam("source", 1).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("Required request parameter 'target' for method parameter type Integer is not present");
	}

	@Test
	void getDifferenceNegativeValueInSource() {
		// Parameter values
		final var id = UUID.randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/difference").queryParam("source", -1).queryParam("target", 0).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getDifferenceByVersions.source", "must be between 0 and 2147483647"));
	}

	@Test
	void getDifferenceNegativeValueInTarget() {
		// Parameter values
		final var id = UUID.randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/difference").queryParam("source", 0).queryParam("target", -1).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getDifferenceByVersions.target", "must be between 0 and 2147483647"));
	}

}
