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
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.ErrandNote;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;
import se.sundsvall.supportmanagement.service.ErrandNoteService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandNotesResourceFailureTest {

	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/errands/{id}/notes/";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandNoteService errandNotesServiceMock;

	@Test
	void readErrandNoteWithInvalidId() {

		// Parameters
		final var id = "invalid-uuid";
		final var noteId = randomUUID().toString();

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("id", id, "noteId", noteId)))
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
			.containsExactlyInAnyOrder(tuple("readErrandNote.id", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void readErrandNoteWithInvalidNoteId() {

		// Parameters
		final var id = ERRAND_ID;
		final var noteId = "invalid-uuid";

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("id", id, "noteId", noteId)))
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
			.containsExactlyInAnyOrder(tuple("readErrandNote.noteId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void findErrandNotesWithInvalidSearchParams() {

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH).queryParam("limit", "1001", "partyId", "invalidPartyId").build(Map.of("id", ERRAND_ID)))
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
			.containsExactlyInAnyOrder(tuple("limit", "must be less than or equal to 1000"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void createErrandNoteInvalidId() {

		// Parameters
		final var id = "invalid-uuid";
		final var requestBody = CreateErrandNoteRequest.create()
			.withBody("body")
			.withContext("context")
			.withCreatedBy("createdBy")
			.withRole("role")
			.withSubject("subject");

		// Call
		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("id", id)))
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
			.containsExactlyInAnyOrder(tuple("createErrandNote.id", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void createErrandNoteInvalidRequestBody() {

		// Parameters
		final var id = ERRAND_ID;
		final var requestBody = ErrandNote.create();

		// Call
		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("id", id)))
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
			.containsExactlyInAnyOrder(
				tuple("body", "must not be blank"),
				tuple("context", "must not be blank"),
				tuple("createdBy", "must not be blank"),
				tuple("role", "must not be blank"),
				tuple("subject", "must not be blank"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void updateErrandNoteInvalidId() {

		// Parameters
		final var id = "invalid-uuid";
		final var noteId = randomUUID().toString();
		final var requestBody = ErrandNote.create()
			.withBody("body")
			.withModifiedBy("createdBy")
			.withSubject("subject");

		// Call
		final var response = webTestClient.patch().uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("id", id, "noteId", noteId)))
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
			.containsExactlyInAnyOrder(tuple("updateErrandNote.id", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void updateErrandNoteInvalidNoteId() {

		// Parameters
		final var id = ERRAND_ID;
		final var noteId = "invalid-uuid";
		final var requestBody = UpdateErrandNoteRequest.create()
			.withBody("body")
			.withModifiedBy("createdBy")
			.withSubject("subject");

		// Call
		final var response = webTestClient.patch().uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("id", id, "noteId", noteId)))
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
			.containsExactlyInAnyOrder(tuple("updateErrandNote.noteId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void updateErrandNoteInvalidRequestBody() {

		// Parameters
		final var id = ERRAND_ID;
		final var noteId = randomUUID().toString();
		final var requestBody = UpdateErrandNoteRequest.create();

		// Call
		final var response = webTestClient.patch().uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("id", id, "noteId", noteId)))
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
				tuple("body", "must not be blank"),
				tuple("modifiedBy", "must not be blank"),
				tuple("subject", "must not be blank"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void deleteErrandNoteWithInvalidId() {

		// Parameters
		final var id = "invalid-uuid";
		final var noteId = randomUUID().toString();

		// Call
		final var response = webTestClient.delete().uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("id", id, "noteId", noteId)))
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
			.containsExactlyInAnyOrder(tuple("deleteErrandNote.id", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void deleteErrandNoteWithInvalidNoteId() {

		// Parameters
		final var id = ERRAND_ID;
		final var noteId = "invalid-uuid";

		// Call
		final var response = webTestClient.delete().uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("id", id, "noteId", noteId)))
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
			.containsExactlyInAnyOrder(tuple("deleteErrandNote.noteId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}
}
