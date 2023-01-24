package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.MetaData;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.ErrandNote;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesRequest;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesResponse;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;
import se.sundsvall.supportmanagement.service.ErrandNoteService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandNotesResourceTest {

	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/errands/{id}/notes/";

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@MockBean
	private ErrandNoteService errandNotesServiceMock;

	@Test
	void createErrandNote() {

		// Parameter values
		final var partyId = randomUUID().toString();
		final var requestBody = CreateErrandNoteRequest.create()
			.withBody("body")
			.withContext("context")
			.withCreatedBy("createdBy")
			.withPartyId(partyId)
			.withRole("role")
			.withSubject("subject");

		// Mock
		final var noteId = randomUUID().toString();
		when(errandNotesServiceMock.createErrandNote(ERRAND_ID, requestBody)).thenReturn(noteId);

		// Call
		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("http://localhost:".concat(String.valueOf(port)).concat(fromPath("/errands/{id}/notes/{noteId}").build(Map.of("id", ERRAND_ID, "noteId", noteId)).toString()))
			.expectBody().isEmpty();

		// Verification
		assertThat(response).isNotNull();
		verify(errandNotesServiceMock).createErrandNote(ERRAND_ID, requestBody);
	}

	@Test
	void readErrandNote() {

		// Parameter values
		final var noteId = randomUUID().toString();

		// Mock
		when(errandNotesServiceMock.readErrandNote(ERRAND_ID, noteId)).thenReturn(ErrandNote.create());

		final var response = webTestClient.get().uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("id", ERRAND_ID, "noteId", noteId)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ErrandNote.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		verify(errandNotesServiceMock).readErrandNote(ERRAND_ID, noteId);
	}

	@Test
	void findErrandNotes() {

		// Parameter values
		final var partyId = randomUUID().toString();
		final var requestParameter = FindErrandNotesRequest.create()
			.withPartyId(partyId);

		// Mock
		final var findErrandNotesResponse = FindErrandNotesResponse.create()
			.withNotes(List.of(ErrandNote.create().withBody("testBody").withSubject("testSubject")))
			.withMetaData(MetaData.create());
		when(errandNotesServiceMock.findErrandNotes(ERRAND_ID, requestParameter)).thenReturn(findErrandNotesResponse);

		final var response = webTestClient.get().uri(builder -> builder.path(PATH).queryParam("partyId", partyId).build(Map.of("id", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(FindErrandNotesResponse.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getResponseBody().getNotes()).hasSize(1);
		verify(errandNotesServiceMock).findErrandNotes(ERRAND_ID, requestParameter);
	}

	@Test
	void updateErrandNotes() {

		// Parameter values
		final var noteId = randomUUID().toString();
		final var requestBody = UpdateErrandNoteRequest.create()
			.withBody("body")
			.withModifiedBy("modifiedBy")
			.withSubject("subject");

		// Mock
		when(errandNotesServiceMock.updateErrandNote(ERRAND_ID, noteId, requestBody)).thenReturn(ErrandNote.create());

		webTestClient.patch().uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("id", ERRAND_ID, "noteId", noteId)))
			.accept(APPLICATION_JSON)
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ErrandNote.class)
			.returnResult();

		// Verification
		verify(errandNotesServiceMock).updateErrandNote(ERRAND_ID, noteId, requestBody);
	}

	@Test
	void deleteErrandNote() {

		// Parameter values
		final var noteId = randomUUID().toString();

		webTestClient.delete().uri(builder -> builder.path(PATH.concat("{noteId}")).build(Map.of("id", ERRAND_ID, "noteId", noteId)))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().doesNotExist(CONTENT_TYPE);

		// Verification
		verify(errandNotesServiceMock).deleteErrandNote(ERRAND_ID, noteId);
	}

}
