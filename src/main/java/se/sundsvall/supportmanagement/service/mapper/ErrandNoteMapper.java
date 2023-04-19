package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.notes.CreateNoteRequest;
import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import generated.se.sundsvall.notes.UpdateNoteRequest;
import se.sundsvall.supportmanagement.api.model.MetaData;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.ErrandNote;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesResponse;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

public class ErrandNoteMapper {

	private ErrandNoteMapper() {}

	public static CreateNoteRequest toCreateNoteRequest(final String municipalityId, final String caseId, final String clientId, final CreateErrandNoteRequest createErrandNoteRequest) {
		final var param = Optional.ofNullable(createErrandNoteRequest).orElse(CreateErrandNoteRequest.create());
		return new CreateNoteRequest()
			.body(param.getBody())
			.municipalityId(municipalityId)
			.caseId(caseId)
			.clientId(clientId)
			.context(param.getContext())
			.createdBy(param.getCreatedBy())
			.partyId(param.getPartyId())
			.role(param.getRole())
			.subject(param.getSubject());
	}

	public static UpdateNoteRequest toUpdateNoteRequest(final UpdateErrandNoteRequest updateErrandNoteRequest) {
		final var param = Optional.ofNullable(updateErrandNoteRequest).orElse(UpdateErrandNoteRequest.create());
		return new UpdateNoteRequest()
			.body(param.getBody())
			.modifiedBy(param.getModifiedBy())
			.subject(param.getSubject());
	}

	public static ErrandNote toErrandNote(final Note note) {
		if (isNull(note)) {
			return null;
		}
		return new ErrandNote()
			.withBody(note.getBody())
			.withCaseId(note.getCaseId())
			.withClientId(note.getClientId())
			.withContext(note.getContext())
			.withCreated(note.getCreated())
			.withCreatedBy(note.getCreatedBy())
			.withId(note.getId())
			.withModified(note.getModified())
			.withModifiedBy(note.getModifiedBy())
			.withPartyId(note.getPartyId())
			.withRole(note.getRole())
			.withSubject(note.getSubject());
	}

	public static FindErrandNotesResponse toFindErrandNotesResponse(final FindNotesResponse findNotesResponse) {
		if (isNull(findNotesResponse)) {
			return null;
		}
		return FindErrandNotesResponse.create()
			.withMetaData(toMetaData(findNotesResponse.getMeta()))
			.withNotes(Optional.ofNullable(findNotesResponse.getNotes()).orElse(emptyList()).stream()
				.map(ErrandNoteMapper::toErrandNote)
				.toList());
	}

	public static MetaData toMetaData(final generated.se.sundsvall.notes.MetaData metaData) {
		if (isNull(metaData)) {
			return null;
		}
		return MetaData.create()
			.withCount(metaData.getCount())
			.withLimit(metaData.getLimit())
			.withPage(metaData.getPage())
			.withTotalPages(metaData.getTotalPages())
			.withTotalRecords(metaData.getTotalRecords());
	}
}
