package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

import generated.se.sundsvall.notes.CreateNoteRequest;
import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import generated.se.sundsvall.notes.UpdateNoteRequest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.MetaData;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.ErrandNote;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesResponse;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.revision.DifferenceResponse;
import se.sundsvall.supportmanagement.api.model.revision.Operation;
import se.sundsvall.supportmanagement.api.model.revision.Revision;

public class ErrandNoteMapper {

	private ErrandNoteMapper() {}

	public static CreateNoteRequest toCreateNoteRequest(final String caseId, final String clientId, final CreateErrandNoteRequest createErrandNoteRequest) {
		final var param = Optional.ofNullable(createErrandNoteRequest).orElse(CreateErrandNoteRequest.create());
		return new CreateNoteRequest()
			.body(param.getBody())
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

	public static List<Revision> toRevisions(List<generated.se.sundsvall.notes.Revision> revisions) {
		return ofNullable(revisions).orElse(emptyList()).stream()
			.map(ErrandNoteMapper::toRevision)
			.filter(Objects::nonNull)
			.toList();
	}

	private static Revision toRevision(generated.se.sundsvall.notes.Revision revision) {
		return ofNullable(revision)
			.map(r -> Revision.create()
				.withCreated(r.getCreated())
				.withEntityId(r.getEntityId())
				.withEntityType(r.getEntityType())
				.withId(r.getId())
				.withVersion(r.getVersion()))
			.orElse(null);
	}

	public static DifferenceResponse toDifferenceResponse(generated.se.sundsvall.notes.DifferenceResponse differenceResponse) {
		return ofNullable(differenceResponse)
			.map(r -> DifferenceResponse.create()
				.withOperations(toOperations(r.getOperations())))
			.orElse(null);
	}

	private static List<Operation> toOperations(List<generated.se.sundsvall.notes.Operation> operations) {
		return ofNullable(operations).orElse(emptyList()).stream()
			.map(ErrandNoteMapper::toOperation)
			.filter(Objects::nonNull)
			.toList();
	}

	private static Operation toOperation(generated.se.sundsvall.notes.Operation operation) {
		return ofNullable(operation)
			.map(o -> Operation.create()
				.withFromValue(o.getFromValue())
				.withOp(o.getOp())
				.withPath(o.getPath())
				.withValue(o.getValue()))
			.orElse(null);
	}
}
