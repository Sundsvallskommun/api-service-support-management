package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.revision.DifferenceResponse;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.service.RevisionService;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class RevisionResourceTest {

	private static final String ERRANDS_PATH = "/errands/{id}/revisions";
	private static final String ERRAND_NOTES_PATH = "/errands/{id}/notes/{noteId}/revisions";

	@MockBean
	private RevisionService revisionServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	// ==============================================================================================================
	// ERRAND REVISION TESTS
	// ==============================================================================================================

	@Test
	void getErrandRevisions() {
		// Parameter values
		final var id = UUID.randomUUID().toString();

		// Mock
		when(revisionServiceMock.getRevisions(id)).thenReturn(List.of(Revision.create()));

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Revision.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verify(revisionServiceMock).getRevisions(id);

	}

	@Test
	void getErrandDifference() {
		// Parameter values
		final var id = UUID.randomUUID().toString();
		final var source = 1;
		final var target = 2;

		// Mock
		when(revisionServiceMock.compareRevisionVersions(id, source, target)).thenReturn(DifferenceResponse.create());

		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH + "/difference").queryParam("source", source).queryParam("target", target).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DifferenceResponse.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verify(revisionServiceMock).compareRevisionVersions(id, source, target);
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
		// TODO: Will be verified in task UF-5000
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
		// TODO: Will be verified in task UF-5000
	}
}
