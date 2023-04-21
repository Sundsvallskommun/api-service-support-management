package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.revision.DifferenceResponse;
import se.sundsvall.supportmanagement.api.model.revision.Revision;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class RevisionResourceTest {

	private static final String ERRANDS_PATH = "/errands/{id}/revisions";
	private static final String ERRAND_NOTES_PATH = "/errands/{id}/notes/{noteId}/revisions";

	@Autowired
	private WebTestClient webTestClient;

	// ==============================================================================================================
	// ERRAND REVISION TESTS
	// ==============================================================================================================

	@Test
	void getErrandRevisions() {
		// Parameter values
		final var id = UUID.randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Revision.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		// TODO: Add verification for call to service
	}

	@Test
	void getSpecificErrandRevision() {
		// Parameter values
		final var id = UUID.randomUUID().toString();
		final var revisionId = UUID.randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH + "/{revisionId}").build(Map.of("id", id, "revisionId", revisionId)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		//TODO: Add verification for call to service
	}

	@Test
	void getErrandDifference() {
		// Parameter values
		final var id = UUID.randomUUID().toString();
		final var source = 1;
		final var target = 2;

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH + "/difference").queryParam("source", source).queryParam("target", target).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DifferenceResponse.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		//TODO: Add verification for call to service
	}

	// ==============================================================================================================
	// ERRAND NOTES REVISION TESTS
	// ==============================================================================================================

	@Test
	void getErrandNoteRevisions() {
		// Parameter values
		final var id = UUID.randomUUID().toString();
		final var noteId = UUID.randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH).build(Map.of("id", id, "noteId", noteId)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Revision.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		// TODO: Add verification for call to service
	}

	@Test
	void getSpecificErrandNoteRevision() {
		// Parameter values
		final var id = UUID.randomUUID().toString();
		final var noteId = UUID.randomUUID().toString();
		final var revisionId = UUID.randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH + "/{revisionId}").build(Map.of("id", id, "noteId", noteId, "revisionId", revisionId)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		// TODO: Add verification for call to service
	}

	@Test
	void getErrandNoteDifference() {
		// Parameter values
		final var id = UUID.randomUUID().toString();
		final var noteId = UUID.randomUUID().toString();
		final var source = 1;
		final var target = 2;

		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH + "/difference").queryParam("source", source).queryParam("target", target).build(Map.of("id", id, "noteId", noteId)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DifferenceResponse.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		// TODO: Add verification for call to service
	}
}
