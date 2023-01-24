package se.sundsvall.supportmanagement.integration.notes;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.notes.configuration.NotesConfiguration.CLIENT_REGISTRATION_ID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import generated.se.sundsvall.notes.CreateNoteRequest;
import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import generated.se.sundsvall.notes.UpdateNoteRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.notes.configuration.NotesConfiguration;

@FeignClient(name = CLIENT_REGISTRATION_ID, url = "${integration.notes.url}", configuration = NotesConfiguration.class)
@CircuitBreaker(name = CLIENT_REGISTRATION_ID)
public interface NotesClient {

	/**
	 * Find all notes, filtered by provided parameters.
	 *
	 * @param context
	 * @param role
	 * @param clientId
	 * @param page
	 * @param limit
	 * @return
	 */
	@GetMapping(path = "/notes", produces = APPLICATION_JSON_VALUE)
	FindNotesResponse findNotes(
		@RequestParam(name = "context") String context,
		@RequestParam(name = "role") String role,
		@RequestParam(name = "caseId") String caseId,
		@RequestParam(name = "clientId") String clientId,
		@RequestParam(name = "partyId") String partyId,
		@RequestParam(name = "page") int page,
		@RequestParam(name = "limit") int limit);

	/**
	 * Find note by id.
	 *
	 * @param id
	 * @return
	 */
	@GetMapping(path = "/notes/{id}", produces = APPLICATION_JSON_VALUE)
	Note findNoteById(@PathVariable(name = "id") String id);

	/**
	 * Delete note by id.
	 *
	 * @param id
	 * @return
	 */
	@DeleteMapping(path = "/notes/{id}")
	ResponseEntity<Void> deleteNoteById(@PathVariable(name = "id") String id);

	/**
	 * Update note by id.
	 *
	 * @param id
	 * @param updateNoteRequest
	 * @return
	 */
	@PatchMapping(path = "/notes/{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	Note updateNoteById(@PathVariable(name = "id") String id, @RequestBody UpdateNoteRequest updateNoteRequest);

	/**
	 * Create note.
	 *
	 * @param id
	 * @param updateNoteRequest
	 * @return
	 */
	@PostMapping(path = "/notes", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createNote(@RequestBody CreateNoteRequest createNoteRequest);
}
