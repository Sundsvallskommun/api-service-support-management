package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.notes.Note;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.ErrandNote;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesRequest;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesResponse;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;
import se.sundsvall.supportmanagement.service.model.RevisionType;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toCreateNoteRequest;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toErrandNote;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toFindErrandNotesResponse;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toUpdateNoteRequest;

@Service
public class ErrandNoteService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandNoteService.class);

	private static final String EVENT_LOG_CREATE_ERRAND_NOTE = "Ärendenotering har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND_NOTE = "Ärendenotering har uppdaterats.";
	private static final String EVENT_LOG_DELETE_ERRAND_NOTE = "Ärendenotering har raderats.";
	private static final String CLIENT_ID = "support-management";
	private static final String NOTE_NOT_FOUND_ON_ERRAND = "A note with id '%s' could not be found on errand with id '%s'";

	private final NotesClient notesClient;
	private final AccessControlService accessControlService;
	private final EventService eventService;

	public ErrandNoteService(final NotesClient notesClient,
		final AccessControlService accessControlService, final EventService eventService) {
		this.notesClient = notesClient;
		this.accessControlService = accessControlService;
		this.eventService = eventService;
	}

	public String createErrandNote(final String namespace, final String municipalityId, final String id, final CreateErrandNoteRequest createErrandNoteRequest) {
		var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false, ProtectedResource.NOTE, RW);

		final var response = notesClient.createNote(municipalityId, toCreateNoteRequest(id, CLIENT_ID, createErrandNoteRequest));

		final var currentRevision = extractRevisionInformationFromHeader(response, RevisionType.CURRENT);
		final var noteId = extractNoteIdFromLocationHeader(response);

		try {
			eventService.createErrandNoteEvent(CREATE, EVENT_LOG_CREATE_ERRAND_NOTE, id, errandEntity, noteId, currentRevision, null);
		} catch (final Exception e) {
			final var sanitizedId = sanitizeForLogging(id);
			final var sanitizedNoteId = sanitizeForLogging(noteId);
			LOG.warn("Failed to log CREATE note event for errand {} note {}: {}", sanitizedId, sanitizedNoteId, e.getMessage());
		}

		return noteId;
	}

	public ErrandNote readErrandNote(final String namespace, final String municipalityId, final String id, final String noteId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, id, ProtectedResource.NOTE, LR);
		return toErrandNote(findNoteOnErrand(municipalityId, id, noteId));
	}

	/**
	 * Fetches a note and verifies it belongs to sent in errand. Notes live in another service, which has no endpoint for
	 * fetching a note by id and case together, so the link has to be checked here. Authorising the errand says nothing
	 * about a note hanging off a different one.
	 * <p>
	 * A note belonging elsewhere answers 404 rather than 401, so the response cannot be used to tell whether a note
	 * exists on an errand the user may not reach. A note without a case id is treated the same way, since nothing ties
	 * it to this errand.
	 *
	 * @param  municipalityId municipality id
	 * @param  errandId       id of the errand the note must belong to, which notes carry as their case id
	 * @param  noteId         id of the note
	 * @return                the note, when it belongs to the errand
	 */
	public Note findNoteOnErrand(final String municipalityId, final String errandId, final String noteId) {
		final var note = notesClient.findNoteById(municipalityId, noteId);
		if (isNull(note) || !Objects.equals(note.getCaseId(), errandId)) {
			throw Problem.valueOf(NOT_FOUND, NOTE_NOT_FOUND_ON_ERRAND.formatted(noteId, errandId));
		}
		return note;
	}

	public FindErrandNotesResponse findErrandNotes(final String namespace, final String municipalityId, final String id, final FindErrandNotesRequest findErrandNotesRequest) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, id, ProtectedResource.NOTE, LR);
		return toFindErrandNotesResponse(notesClient.findNotes(
			municipalityId,
			findErrandNotesRequest.getContext(),
			findErrandNotesRequest.getRole(),
			id,
			CLIENT_ID,
			findErrandNotesRequest.getPartyId(),
			findErrandNotesRequest.getPage(),
			findErrandNotesRequest.getLimit()));
	}

	public ErrandNote updateErrandNote(final String namespace, final String municipalityId, final String id, final String noteId, final UpdateErrandNoteRequest updateErrandNoteRequest) {
		var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false, ProtectedResource.NOTE, RW);
		findNoteOnErrand(municipalityId, id, noteId);

		final var response = notesClient.updateNoteById(municipalityId, noteId, toUpdateNoteRequest(updateErrandNoteRequest));

		final var currentRevision = extractRevisionInformationFromHeader(response, RevisionType.CURRENT);
		if (nonNull(currentRevision)) {
			final var previousRevision = extractRevisionInformationFromHeader(response, RevisionType.PREVIOUS);
			try {
				eventService.createErrandNoteEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND_NOTE, id, errandEntity, noteId, currentRevision, previousRevision);
			} catch (final Exception e) {
				final var sanitizedId = sanitizeForLogging(id);
				final var sanitizedNoteId = sanitizeForLogging(noteId);
				LOG.warn("Failed to log UPDATE note event for errand {} note {}: {}", sanitizedId, sanitizedNoteId, e.getMessage());
			}
		}

		return toErrandNote(response.getBody());
	}

	public void deleteErrandNote(final String namespace, final String municipalityId, final String id, final String noteId) {
		var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false, ProtectedResource.NOTE, RW);
		findNoteOnErrand(municipalityId, id, noteId);

		final var response = notesClient.deleteNoteById(municipalityId, noteId);

		final var currentRevision = extractRevisionInformationFromHeader(response, RevisionType.CURRENT);
		try {
			eventService.createErrandNoteEvent(DELETE, EVENT_LOG_DELETE_ERRAND_NOTE, id, errandEntity, noteId, currentRevision, null);
		} catch (final Exception e) {
			final var sanitizedId = sanitizeForLogging(id);
			final var sanitizedNoteId = sanitizeForLogging(noteId);
			LOG.warn("Failed to log DELETE note event for errand {} note {}: {}", sanitizedId, sanitizedNoteId, e.getMessage());
		}
	}

	private String extractNoteIdFromLocationHeader(final ResponseEntity<Void> response) {
		final var locationValue = Optional.ofNullable(response.getHeaders().get(LOCATION)).orElse(emptyList()).stream().findFirst();
		return locationValue.map(string -> string.substring(string.lastIndexOf('/') + 1)).orElse(EMPTY);
	}

	private Revision extractRevisionInformationFromHeader(final ResponseEntity<?> response, final RevisionType revision) {
		final var rev = extractHeader(response, String.format("x-%s-revision", revision.getValue()));
		final var ver = extractHeader(response, String.format("x-%s-version", revision.getValue()));

		if (rev.isPresent() && ver.isPresent()) {
			return Revision.create()
				.withId(rev.get())
				.withVersion(Integer.parseInt(ver.get()));
		}

		return null;
	}

	private Optional<String> extractHeader(final ResponseEntity<?> response, final String headerName) {
		return Optional.of(response.getHeaders())
			.map(headers -> headers.get(headerName))
			.orElse(emptyList())
			.stream()
			.findFirst();
	}
}
