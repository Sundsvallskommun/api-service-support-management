package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.RevisionService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class RevisionResourceFailuresTest {

	private static final String NAMESPACE = "name.space";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRANDS_PATH = "{municipalityId}/{namespace}/errands/{errandId}/revisions";

	private static final String ERRAND_NOTES_PATH = "{municipalityId}/{namespace}/errands/{errandId}/notes/{noteId}/revisions";

	@MockBean
	private RevisionService revisionServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	// ==============================================================================================================
	// ERRAND REVISION FAILURE TESTS
	// ==============================================================================================================

	@Test
	void getErrandRevisionsByInvalidErrandId() {

		// Parameter values
		final var errandId = "invalid";

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId)))
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
			.containsExactly(tuple("getErrandRevisions.errandId", "not a valid UUID"));

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandRevisionDifferencesByInvalidErrandId() {

		// Parameter values
		final var errandId = "invalid";

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH + "/difference").queryParam("source", 0).queryParam("target", 1)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId)))
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
			.containsExactly(tuple("getErrandDiffByVersions.errandId", "not a valid UUID"));

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandRevisionDifferencesNoSourceParameter() {
		// Parameter values
		final var errandId = randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH + "/difference").queryParam("target", 0)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId)))
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

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandRevisionDifferencesNoTargetParameter() {
		// Parameter values
		final var errandId = randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH + "/difference").queryParam("source", 1)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId)))
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

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandRevisionDifferencesNegativeValueInSource() {
		// Parameter values
		final var errandId = randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH + "/difference").queryParam("source", -1).queryParam("target", 0)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId)))
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
			.containsExactly(tuple("getErrandDiffByVersions.source", "must be between 0 and 2147483647"));

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandRevisionDifferencesNegativeValueInTarget() {
		// Parameter values
		final var errandId = randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH + "/difference").queryParam("source", 0).queryParam("target", -1)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId)))
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
			.containsExactly(tuple("getErrandDiffByVersions.target", "must be between 0 and 2147483647"));

		verifyNoInteractions(revisionServiceMock);
	}

	// ==============================================================================================================
	// ERRAND NOTES REVISION FAILURE TESTS
	// ==============================================================================================================

	@Test
	void getErrandNoteRevisionsByInvalidErrandId() {

		// Parameter values
		final var errandId = "invalid";
		final var noteId = randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId, "noteId", noteId)))
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
			.containsExactly(tuple("getErrandNoteRevisions.errandId", "not a valid UUID"));

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandNoteRevisionsByInvalidNoteId() {

		// Parameter values
		final var errandId = randomUUID().toString();
		final var noteId = "invalid";

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId, "noteId", noteId)))
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
			.containsExactly(tuple("getErrandNoteRevisions.noteId", "not a valid UUID"));

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandNoteRevisionDifferencesByInvalidErrandId() {

		// Parameter values
		final var errandId = "invalid";
		final var noteId = randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH + "/difference").queryParam("source", 0).queryParam("target", 1)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId, "noteId", noteId)))
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
			.containsExactly(tuple("getErrandNoteDiffByVersions.errandId", "not a valid UUID"));

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandNoteRevisionDifferencesByInvalidNoteId() {

		// Parameter values
		final var errandId = randomUUID().toString();
		final var noteId = "invalid";

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH + "/difference").queryParam("source", 0).queryParam("target", 1)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId, "noteId", noteId)))
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
			.containsExactly(tuple("getErrandNoteDiffByVersions.noteId", "not a valid UUID"));

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandNoteRevisionDifferencesNoSourceParameter() {
		// Parameter values
		final var errandId = randomUUID().toString();
		final var noteId = randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH + "/difference").queryParam("target", 1)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId, "noteId", noteId)))
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

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandNoteRevisionDifferencesNoTargetParameter() {
		// Parameter values
		final var errandId = randomUUID().toString();
		final var noteId = randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH + "/difference").queryParam("source", 1)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId, "noteId", noteId)))
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

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandNoteRevisionDifferencesNegativeValueInSource() {
		// Parameter values
		final var errandId = randomUUID().toString();
		final var noteId = randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH + "/difference").queryParam("source", -1).queryParam("target", 0)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId, "noteId", noteId)))
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
			.containsExactly(tuple("getErrandNoteDiffByVersions.source", "must be between 0 and 2147483647"));

		verifyNoInteractions(revisionServiceMock);
	}

	@Test
	void getErrandNoteRevisionDifferencesNegativeValueInTarget() {
		// Parameter values
		final var errandId = randomUUID().toString();
		final var noteId = randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH + "/difference").queryParam("source", 0).queryParam("target", -1)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId, "noteId", noteId)))
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
			.containsExactly(tuple("getErrandNoteDiffByVersions.target", "must be between 0 and 2147483647"));

		verifyNoInteractions(revisionServiceMock);
	}
}
