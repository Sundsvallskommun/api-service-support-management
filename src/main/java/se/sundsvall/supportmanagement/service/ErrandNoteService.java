package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CASE_ID;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toCreateNoteRequest;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toErrandNote;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toFindErrandNotesResponse;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toUpdateNoteRequest;

import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.filter.ExecutingUserSupplier;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.ErrandNote;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesRequest;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesResponse;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;

@Service
public class ErrandNoteService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String EVENT_LOG_CREATE_ERRAND_NOTE = "Ärendenotering har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND_NOTE = "Ärendenotering har uppdaterats.";
	private static final String EVENT_LOG_DELETE_ERRAND_NOTE = "Ärendenotering har raderats.";

	@Value("${spring.application.name:}")
	private String clientId;

	@Autowired
	private NotesClient notesClient;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private EventService eventService;

	@Autowired
	private ExecutingUserSupplier executingUserSupplier;

	public String createErrandNote(String namespace, String municipalityId, String id, CreateErrandNoteRequest createErrandNoteRequest) {
		verifyExistingErrand(id, namespace, municipalityId);

		final var response = notesClient.createNote(executingUserSupplier.getAdUser(), toCreateNoteRequest(municipalityId, id, clientId, createErrandNoteRequest));

		// Create log event
		final var currentRevision = extractRevisionInformationFromHeader(response, RevisionType.CURRENT);
		eventService.createErrandNoteEvent(CREATE, EVENT_LOG_CREATE_ERRAND_NOTE, id, extractCaseId(errandsRepository.getReferenceById(id)), currentRevision, null); // Create only has a currentRevision

		return extractNoteIdFromLocationHeader(response);
	}

	public ErrandNote readErrandNote(String namespace, String municipalityId, String id, String noteId) {
		verifyExistingErrand(id, namespace, municipalityId);
		return toErrandNote(notesClient.findNoteById(noteId));
	}

	public FindErrandNotesResponse findErrandNotes(String namespace, String municipalityId, String id, FindErrandNotesRequest findErrandNotesRequest) {
		verifyExistingErrand(id, namespace, municipalityId);
		return toFindErrandNotesResponse(notesClient.findNotes(
			municipalityId,
			findErrandNotesRequest.getContext(),
			findErrandNotesRequest.getRole(),
			id,
			clientId,
			findErrandNotesRequest.getPartyId(),
			findErrandNotesRequest.getPage(),
			findErrandNotesRequest.getLimit()));
	}

	public ErrandNote updateErrandNote(String namespace, String municipalityId, String id, String noteId, UpdateErrandNoteRequest updateErrandNoteRequest) {
		verifyExistingErrand(id, namespace, municipalityId);

		final var response = notesClient.updateNoteById(executingUserSupplier.getAdUser(), noteId, toUpdateNoteRequest(updateErrandNoteRequest));

		// Create log event if the update has modified the note (and thus has created a new revision)
		final var currentRevision = extractRevisionInformationFromHeader(response, RevisionType.CURRENT);
		if (nonNull(currentRevision)) {
			final var previousRevision = extractRevisionInformationFromHeader(response, RevisionType.PREVIOUS);
			eventService.createErrandNoteEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND_NOTE, id, extractCaseId(errandsRepository.getReferenceById(id)), currentRevision, previousRevision);
		}

		return toErrandNote(response.getBody());
	}

	public void deleteErrandNote(String namespace, String municipalityId, String id, String noteId) {
		verifyExistingErrand(id, namespace, municipalityId);

		final var response = notesClient.deleteNoteById(executingUserSupplier.getAdUser(), noteId);

		// Create log event
		final var currentRevision = extractRevisionInformationFromHeader(response, RevisionType.CURRENT);
		eventService.createErrandNoteEvent(DELETE, EVENT_LOG_DELETE_ERRAND_NOTE, id, extractCaseId(errandsRepository.getReferenceById(id)), currentRevision, null); // Delete only has a currentRevision
	}

	private String extractNoteIdFromLocationHeader(final ResponseEntity<Void> response) {
		final var locationValue = Optional.ofNullable(response.getHeaders().get(LOCATION)).orElse(emptyList()).stream().findFirst();
		if (locationValue.isPresent()) {
			return locationValue.get().substring(locationValue.get().lastIndexOf('/') + 1);
		}
		return EMPTY;
	}

	private Revision extractRevisionInformationFromHeader(ResponseEntity<?> response, RevisionType revision) {
		final var rev = extractHeader(response, String.format("x-%s-revision", revision.getValue()));
		final var ver = extractHeader(response, String.format("x-%s-version", revision.getValue()));

		if (rev.isPresent() && ver.isPresent()) {
			return Revision.create()
				.withId(rev.get())
				.withVersion(Integer.parseInt(ver.get()));
		}

		return null;
	}

	private Optional<String> extractHeader(ResponseEntity<?> response, String headerName) {
		return Optional.ofNullable(response.getHeaders())
			.map(headers -> headers.get(headerName))
			.orElse(emptyList())
			.stream()
			.findFirst();
	}

	private String extractCaseId(ErrandEntity errand) {
		return ofNullable(errand)
			.map(ErrandEntity::getExternalTags)
			.orElse(emptyList())
			.stream()
			.filter(et -> Objects.equals(EXTERNAL_TAG_KEY_CASE_ID, et.getKey()))
			.map(DbExternalTag::getValue)
			.findAny()
			.orElse(null);

	}

	private void verifyExistingErrand(String id, String namespace, String municipalityId) {
		if (!errandsRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, namespace, municipalityId));
		}
	}
}
