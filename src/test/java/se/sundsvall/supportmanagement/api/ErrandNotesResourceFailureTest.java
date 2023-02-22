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

	private static final String NAMESPACE = "name.space";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String NOTE_ID = randomUUID().toString();
	private static final String INVALID = "#invalid#";
	private static final String PATH = "/{namespace}/{municipalityId}/errands/{id}/notes/";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandNoteService errandNotesServiceMock;

	@Test
	void readErrandNoteWithInvalidNamespace() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "noteId", NOTE_ID)))
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
			.containsExactlyInAnyOrder(tuple("readErrandNote.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void readErrandNoteWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID, "noteId", NOTE_ID)))
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
			.containsExactlyInAnyOrder(tuple("readErrandNote.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void readErrandNoteWithInvalidId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID, "noteId", NOTE_ID)))
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

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "noteId", INVALID)))
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
	void findErrandNotesWithInvalidNamespace() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).queryParam("limit", "1").build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
			.containsExactly(tuple("findErrandNotes.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void findErrandNotesWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).queryParam("limit", "1").build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID)))
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
			.containsExactlyInAnyOrder(tuple("findErrandNotes.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void findErrandNotesWithInvalidSearchParams() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).queryParam("limit", "1001").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
	void createErrandNoteInvalidNamespace() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandNoteRequest())
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
			.containsExactlyInAnyOrder(tuple("createErrandNote.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void createErrandNoteInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(createErrandNoteRequest())
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
			.containsExactlyInAnyOrder(tuple("createErrandNote.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void createErrandNoteInvalidId() {

		// Parameters
		final var requestBody = createErrandNoteRequest();

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID)))
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

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(ErrandNote.create())
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
	void updateErrandNoteInvalidNamespace() {

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "noteId", NOTE_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(updateErrandNoteRequest())
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
			.containsExactlyInAnyOrder(tuple("updateErrandNote.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void updateErrandNoteInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID, "noteId", NOTE_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(updateErrandNoteRequest())
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
			.containsExactlyInAnyOrder(tuple("updateErrandNote.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void updateErrandNoteInvalidId() {

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID, "noteId", NOTE_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(updateErrandNoteRequest())
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

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "noteId", INVALID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(updateErrandNoteRequest())
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

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "noteId", NOTE_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(UpdateErrandNoteRequest.create())
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
	void deleteErrandNoteWithInvalidNamespace() {

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "noteId", NOTE_ID)))
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
			.containsExactlyInAnyOrder(tuple("deleteErrandNote.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void deleteErrandNoteWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID, "noteId", NOTE_ID)))
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
			.containsExactlyInAnyOrder(tuple("deleteErrandNote.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(errandNotesServiceMock);
	}

	@Test
	void deleteErrandNoteWithInvalidId() {

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID, "noteId", NOTE_ID)))
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

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "noteId", INVALID)))
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

	private static CreateErrandNoteRequest createErrandNoteRequest() {
		return CreateErrandNoteRequest.create()
			.withBody("body")
			.withContext("context")
			.withCreatedBy("createdBy")
			.withRole("role")
			.withSubject("subject");
	}

	private static UpdateErrandNoteRequest updateErrandNoteRequest() {
		return UpdateErrandNoteRequest.create()
			.withBody("body")
			.withModifiedBy("createdBy")
			.withSubject("subject");
	}
}
