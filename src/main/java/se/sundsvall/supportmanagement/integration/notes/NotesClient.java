package se.sundsvall.supportmanagement.integration.notes;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.Constants.AD_USER_HEADER_KEY;
import static se.sundsvall.supportmanagement.integration.notes.configuration.NotesConfiguration.CLIENT_ID;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import generated.se.sundsvall.notes.CreateNoteRequest;
import generated.se.sundsvall.notes.DifferenceResponse;
import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import generated.se.sundsvall.notes.Revision;
import generated.se.sundsvall.notes.UpdateNoteRequest;
import se.sundsvall.supportmanagement.integration.notes.configuration.NotesConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.notes.url}", configuration = NotesConfiguration.class)
public interface NotesClient {

	/**
	 * Find all notes, filtered by provided parameters.
	 *
	 * @param municipalityId the municipalityId of the note
	 * @param context the context of the note
	 * @param role the role of the note
	 * @param clientId the client id of the note
	 * @param page the page number of the result
	 * @param limit the number of results per page
	 * @return the notes
	 */
	@GetMapping(path = "/notes", produces = APPLICATION_JSON_VALUE)
	FindNotesResponse findNotes(
		@RequestParam(name = "municipalityId") String municipalityId,
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
	 * @param id the id of the note to find
	 * @return the note
	 */
	@GetMapping(path = "/notes/{id}", produces = APPLICATION_JSON_VALUE)
	Note findNoteById(@PathVariable(name = "id") String id);

	/**
	 * Delete note by id.
	 *
	 * @param id the id of the note to delete
	 */
	@DeleteMapping(path = "/notes/{id}")
	ResponseEntity<Void> deleteNoteById(@RequestHeader(AD_USER_HEADER_KEY) String executingUser, @PathVariable(name = "id") String id);

	/**
	 * Update note by id.
	 *
	 * @param id the id of the note to update
	 * @param updateNoteRequest the note to update
	 * @return the updated note
	 */
	@PatchMapping(path = "/notes/{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Note> updateNoteById(@RequestHeader(AD_USER_HEADER_KEY) String executingUser, @PathVariable(name = "id") String id, @RequestBody UpdateNoteRequest updateNoteRequest);

	/**
	 * Create note.
	 *
	 * @param createNoteRequest the note to create
	 */
	@PostMapping(path = "/notes", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createNote(@RequestHeader(AD_USER_HEADER_KEY) String executingUser, @RequestBody CreateNoteRequest createNoteRequest);

	/**
	 * Find all revisions for a note by id.
	 *
	 * @param id the id of the note to find revisions for
	 * @return a list of revisions connected to the note
	 */
	@GetMapping(path = "/notes/{id}/revisions", produces = APPLICATION_JSON_VALUE)
	List<Revision> findAllNoteRevisions(@PathVariable(name = "id") String id);

	/**
	 * Compare two revision versions of a note to each other.
	 *
	 * @param id            the id of the note to compare
	 * @param sourceVersion the version to use as source
	 * @param targetVersion the version to use as target
	 * @return all found differences between the source and target version of the note
	 */
	@GetMapping(path = "/notes/{id}/revisions/difference", produces = APPLICATION_JSON_VALUE)
	DifferenceResponse compareNoteRevisions(@PathVariable(name = "id") String id, @RequestParam(name = "source") Integer sourceVersion, @RequestParam(name = "target") Integer targetVersion);
}
