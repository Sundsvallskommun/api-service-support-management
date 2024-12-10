package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRAND_ID = randomUUID().toString();

	private static final String NOTE_ID = randomUUID().toString();

	private static final String PATH = "/{municipalityId}/" + NAMESPACE + "/errands/{errandId}/notes";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandNoteService errandNotesServiceMock;

	@Test
	void createErrandNote() {

		// Parameter values
		final var requestBody = CreateErrandNoteRequest.create()
			.withBody("body")
			.withContext("context")
			.withCreatedBy("createdBy")
			.withPartyId(randomUUID().toString())
			.withRole("role")
			.withSubject("subject");

		// Mock
		final var noteId = randomUUID().toString();
		when(errandNotesServiceMock.createErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestBody)).thenReturn(noteId);

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/notes/" + noteId)
			.expectBody().isEmpty();

		// Verification
		assertThat(response).isNotNull();
		verify(errandNotesServiceMock).createErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestBody);
	}

	@Test
	void readErrandNote() {

		// Mock
		when(errandNotesServiceMock.readErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID)).thenReturn(ErrandNote.create());

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{noteId}")).build(Map.of("municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "noteId", NOTE_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ErrandNote.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		verify(errandNotesServiceMock).readErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID);
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
		when(errandNotesServiceMock.findErrandNotes(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestParameter)).thenReturn(findErrandNotesResponse);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).queryParam("partyId", partyId).build(Map.of("municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(FindErrandNotesResponse.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getResponseBody().getNotes()).hasSize(1);
		verify(errandNotesServiceMock).findErrandNotes(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestParameter);
	}

	@Test
	void updateErrandNotes() {

		// Parameter values
		final var requestBody = UpdateErrandNoteRequest.create()
			.withBody("body")
			.withModifiedBy("modifiedBy")
			.withSubject("subject");

		// Mock
		when(errandNotesServiceMock.updateErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID, requestBody)).thenReturn(ErrandNote.create());

		webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("/{noteId}")).build(Map.of("municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "noteId", NOTE_ID)))
			.accept(APPLICATION_JSON)
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ErrandNote.class)
			.returnResult();

		// Verification
		verify(errandNotesServiceMock).updateErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID, requestBody);
	}

	@Test
	void deleteErrandNote() {

		webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{noteId}")).build(Map.of("municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "noteId", NOTE_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL_VALUE);

		// Verification
		verify(errandNotesServiceMock).deleteErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID);
	}
}
