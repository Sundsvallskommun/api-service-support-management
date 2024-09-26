package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
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

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class RevisionResourceTest {

	private static final String NAMESPACE = "name.space";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRANDS_PATH = "{municipalityId}/{namespace}/errands/{id}/revisions";

	private static final String ERRAND_NOTES_PATH = "{municipalityId}/{namespace}/errands/{id}/notes/{noteId}/revisions";

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
		when(revisionServiceMock.getErrandRevisions(NAMESPACE,MUNICIPALITY_ID,id)).thenReturn(List.of(Revision.create()));

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Revision.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verify(revisionServiceMock).getErrandRevisions(NAMESPACE,MUNICIPALITY_ID,id);

	}

	@Test
	void getErrandDifference() {
		// Parameter values
		final var id = UUID.randomUUID().toString();
		final var source = 1;
		final var target = 2;

		// Mock
		when(revisionServiceMock.compareErrandRevisionVersions(NAMESPACE,MUNICIPALITY_ID,id, source, target)).thenReturn(DifferenceResponse.create());

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(ERRANDS_PATH + "/difference").queryParam("source", source).queryParam("target", target)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE,"id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DifferenceResponse.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verify(revisionServiceMock).compareErrandRevisionVersions(NAMESPACE,MUNICIPALITY_ID,id, source, target);
	}

	// ==============================================================================================================
	// ERRAND NOTES REVISION TESTS
	// ==============================================================================================================

	@Test
	void getErrandNoteRevisions() {
		// Parameter values
		final var id = UUID.randomUUID().toString();
		final var noteId = UUID.randomUUID().toString();

		// Mock
		when(revisionServiceMock.getNoteRevisions(NAMESPACE,MUNICIPALITY_ID,id, noteId)).thenReturn(List.of(Revision.create()));

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE,"id", id, "noteId", noteId)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Revision.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verify(revisionServiceMock).getNoteRevisions(NAMESPACE,MUNICIPALITY_ID,id, noteId);
	}

	@Test
	void getErrandNoteDifference() {
		// Parameter values
		final var id = UUID.randomUUID().toString();
		final var noteId = UUID.randomUUID().toString();
		final var source = 1;
		final var target = 2;

		// Mock
		when(revisionServiceMock.compareNoteRevisionVersions(NAMESPACE,MUNICIPALITY_ID,id, noteId, source, target)).thenReturn(DifferenceResponse.create());

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(ERRAND_NOTES_PATH + "/difference").queryParam("source", source).queryParam("target", target)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE,"id", id, "noteId", noteId)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DifferenceResponse.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verify(revisionServiceMock).compareNoteRevisionVersions(NAMESPACE,MUNICIPALITY_ID,id, noteId, source, target);
	}

}
