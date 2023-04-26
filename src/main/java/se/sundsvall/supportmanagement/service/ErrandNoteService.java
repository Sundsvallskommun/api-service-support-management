package se.sundsvall.supportmanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.ErrandNote;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesRequest;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesResponse;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toCreateNoteRequest;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toErrandNote;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toFindErrandNotesResponse;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toUpdateNoteRequest;

@Service
public class ErrandNoteService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";

	@Value("${spring.application.name:}")
	private String clientId;

	@Autowired
	private NotesClient notesClient;

	@Autowired
	private ErrandsRepository errandsRepository;

	public String createErrandNote(String namespace, String municipalityId, String id, CreateErrandNoteRequest createErrandNoteRequest) {
		verifyExistingErrand(id, namespace, municipalityId);
		return extractNoteIdFromLocationHeader(notesClient.createNote(toCreateNoteRequest(municipalityId, id, clientId, createErrandNoteRequest)));
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
		return toErrandNote(notesClient.updateNoteById(noteId, toUpdateNoteRequest(updateErrandNoteRequest)));
	}

	public void deleteErrandNote(String namespace, String municipalityId, String id, String noteId) {
		verifyExistingErrand(id, namespace, municipalityId);
		notesClient.deleteNoteById(noteId);
	}

	private String extractNoteIdFromLocationHeader(final ResponseEntity<Void> response) {
		final var locationValue = Optional.ofNullable(response.getHeaders().get(LOCATION)).orElse(emptyList()).stream().findFirst();
		if (locationValue.isPresent()) {
			return locationValue.get().substring(locationValue.get().lastIndexOf('/') + 1);
		}
		return EMPTY;
	}

	private void verifyExistingErrand(String id, String namespace, String municipalityId) {
		if (!errandsRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, namespace, municipalityId));
		}
	}
}
