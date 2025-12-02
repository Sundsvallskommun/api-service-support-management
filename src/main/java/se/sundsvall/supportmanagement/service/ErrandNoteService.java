package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpHeaders.LOCATION;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toCreateNoteRequest;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toErrandNote;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toFindErrandNotesResponse;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toUpdateNoteRequest;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.ErrandNote;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesRequest;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesResponse;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;
import se.sundsvall.supportmanagement.service.model.RevisionType;

@Service
public class ErrandNoteService {

	private static final String EVENT_LOG_CREATE_ERRAND_NOTE = "Ärendenotering har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND_NOTE = "Ärendenotering har uppdaterats.";
	private static final String EVENT_LOG_DELETE_ERRAND_NOTE = "Ärendenotering har raderats.";
	private static final String CLIENT_ID = "support-management";

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
		var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false, RW);

		final var response = notesClient.createNote(municipalityId, toCreateNoteRequest(id, CLIENT_ID, createErrandNoteRequest));

		// Create log event
		final var currentRevision = extractRevisionInformationFromHeader(response, RevisionType.CURRENT);
		final var noteId = extractNoteIdFromLocationHeader(response);

		eventService.createErrandNoteEvent(CREATE, EVENT_LOG_CREATE_ERRAND_NOTE, id, errandEntity, noteId, currentRevision, null); // Create only has a currentRevision

		return noteId;
	}

	public ErrandNote readErrandNote(final String namespace, final String municipalityId, final String id, final String noteId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, id, R, RW);
		return toErrandNote(notesClient.findNoteById(municipalityId, noteId));
	}

	public FindErrandNotesResponse findErrandNotes(final String namespace, final String municipalityId, final String id, final FindErrandNotesRequest findErrandNotesRequest) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, id, R, RW);
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
		var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false, RW);

		final var response = notesClient.updateNoteById(municipalityId, noteId, toUpdateNoteRequest(updateErrandNoteRequest));

		// Create log event if the update has modified the note (and thus has created a new revision)
		final var currentRevision = extractRevisionInformationFromHeader(response, RevisionType.CURRENT);
		if (nonNull(currentRevision)) {
			final var previousRevision = extractRevisionInformationFromHeader(response, RevisionType.PREVIOUS);
			eventService.createErrandNoteEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND_NOTE, id, errandEntity, noteId, currentRevision, previousRevision);
		}

		return toErrandNote(response.getBody());
	}

	public void deleteErrandNote(final String namespace, final String municipalityId, final String id, final String noteId) {
		var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false, RW);

		final var response = notesClient.deleteNoteById(municipalityId, noteId);

		// Create log event
		final var currentRevision = extractRevisionInformationFromHeader(response, RevisionType.CURRENT);
		eventService.createErrandNoteEvent(DELETE, EVENT_LOG_DELETE_ERRAND_NOTE, id, errandEntity, noteId, currentRevision, null); // Delete only has a currentRevision
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
