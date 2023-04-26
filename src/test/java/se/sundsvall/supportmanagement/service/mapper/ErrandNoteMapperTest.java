package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.ErrandNote;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class ErrandNoteMapperTest {

	private static final String BODY = "body";
	private static final String CASE_ID = "caseId";
	private static final String CLIENT_ID = "clientId";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String CONTEXT = "context";
	private static final OffsetDateTime CREATED = now().minusWeeks(1);
	private static final String CREATED_BY = "createdBy";
	private static final String ID = "id";
	private static final OffsetDateTime MODIFIED = now();
	private static final String MODIFIED_BY = "modifiedBy";
	private static final String PARTY_ID = "partyId";
	private static final String ROLE = "role";
	private static final String SUBJECT = "subject";
	private static final int COUNT = 11;
	private static final int LIMIT = 22;
	private static final int PAGE = 33;
	private static final int TOTAL_PAGES = 44;
	private static final long TOTAL_RECORDS = 55;

	@Test
	void toCreateNoteRequest() {

		final var result = ErrandNoteMapper.toCreateNoteRequest(MUNICIPALITY_ID, CASE_ID, CLIENT_ID, buildCreateErrandNoteRequest());

		assertThat(result.getBody()).isEqualTo(BODY);
		assertThat(result.getCaseId()).isEqualTo(CASE_ID);
		assertThat(result.getCaseLink()).isNull();
		assertThat(result.getCaseType()).isNull();
		assertThat(result.getClientId()).isEqualTo(CLIENT_ID);
		assertThat(result.getContext()).isEqualTo(CONTEXT);
		assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(result.getExternalCaseId()).isNull();
		assertThat(result.getPartyId()).isEqualTo(PARTY_ID);
		assertThat(result.getRole()).isEqualTo(ROLE);
		assertThat(result.getSubject()).isEqualTo(SUBJECT);
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
	}

	@Test
	void toCreateNoteRequestAllNulls() {

		assertThat(ErrandNoteMapper.toCreateNoteRequest(null, null, null, null))
			.isNotNull()
			.hasAllNullFieldsOrProperties();
	}

	@Test
	void toUpdateNoteRequest() {

		final var result = ErrandNoteMapper.toUpdateNoteRequest(buildUpdateErrandNoteRequest());

		assertThat(result.getBody()).isEqualTo(BODY);
		assertThat(result.getCaseId()).isNull();
		assertThat(result.getCaseLink()).isNull();
		assertThat(result.getCaseType()).isNull();
		assertThat(result.getExternalCaseId()).isNull();
		assertThat(result.getModifiedBy()).isEqualTo(MODIFIED_BY);
		assertThat(result.getSubject()).isEqualTo(SUBJECT);
	}

	@Test
	void toUpdateNoteRequestFromNullInput() {

		assertThat(ErrandNoteMapper.toUpdateNoteRequest(null))
			.isNotNull()
			.hasAllNullFieldsOrProperties();
	}

	@Test
	void toErrandNote() {

		final var result = ErrandNoteMapper.toErrandNote(buildNote());

		assertThat(result.getBody()).isEqualTo(BODY);
		assertThat(result.getCaseId()).isEqualTo(CASE_ID);
		assertThat(result.getClientId()).isEqualTo(CLIENT_ID);
		assertThat(result.getContext()).isEqualTo(CONTEXT);
		assertThat(result.getCreated()).isEqualTo(CREATED);
		assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(result.getId()).isEqualTo(ID);
		assertThat(result.getModified()).isEqualTo(MODIFIED);
		assertThat(result.getModifiedBy()).isEqualTo(MODIFIED_BY);
		assertThat(result.getPartyId()).isEqualTo(PARTY_ID);
		assertThat(result.getRole()).isEqualTo(ROLE);
		assertThat(result.getSubject()).isEqualTo(SUBJECT);
	}

	@Test
	void toErrandNoteFromNullInput() {
		assertThat(ErrandNoteMapper.toErrandNote(null)).isNull();
	}

	@Test
	void toFindErrandNotesResponse() {

		// Parameters.
		final var findNotesResponse = new FindNotesResponse()
			.notes(List.of(buildNote()))
			.meta(buildMetaData());

		final var result = ErrandNoteMapper.toFindErrandNotesResponse(findNotesResponse);

		assertThat(result).isNotNull();
		assertThat(result.getMetaData().getCount()).isEqualTo(COUNT);
		assertThat(result.getMetaData().getLimit()).isEqualTo(LIMIT);
		assertThat(result.getMetaData().getPage()).isEqualTo(PAGE);
		assertThat(result.getMetaData().getTotalPages()).isEqualTo(TOTAL_PAGES);
		assertThat(result.getMetaData().getTotalRecords()).isEqualTo(TOTAL_RECORDS);
		assertThat(result.getNotes())
			.hasSize(1)
			.extracting(
				ErrandNote::getBody,
				ErrandNote::getCaseId,
				ErrandNote::getClientId,
				ErrandNote::getContext,
				ErrandNote::getCreated,
				ErrandNote::getCreatedBy,
				ErrandNote::getId,
				ErrandNote::getModified,
				ErrandNote::getModifiedBy,
				ErrandNote::getPartyId,
				ErrandNote::getRole,
				ErrandNote::getSubject)
			.containsExactly(tuple(
				BODY,
				CASE_ID,
				CLIENT_ID,
				CONTEXT,
				CREATED,
				CREATED_BY,
				ID,
				MODIFIED,
				MODIFIED_BY,
				PARTY_ID,
				ROLE,
				SUBJECT));
	}

	@Test
	void toErrandNotesFromNullInput() {
		assertThat(ErrandNoteMapper.toFindErrandNotesResponse(null)).isNull();
	}

	@Test
	void toMetaData() {

		final var result = ErrandNoteMapper.toMetaData(buildMetaData());

		assertThat(result).isNotNull();
		assertThat(result.getCount()).isEqualTo(COUNT);
		assertThat(result.getLimit()).isEqualTo(LIMIT);
		assertThat(result.getPage()).isEqualTo(PAGE);
		assertThat(result.getTotalPages()).isEqualTo(TOTAL_PAGES);
		assertThat(result.getTotalRecords()).isEqualTo(TOTAL_RECORDS);
	}

	@Test
	void toMetaDataFromNullInput() {
		assertThat(ErrandNoteMapper.toMetaData(null)).isNull();
	}

	private static CreateErrandNoteRequest buildCreateErrandNoteRequest() {
		return CreateErrandNoteRequest.create()
			.withBody(BODY)
			.withContext(CONTEXT)
			.withCreatedBy(CREATED_BY)
			.withPartyId(PARTY_ID)
			.withRole(ROLE)
			.withSubject(SUBJECT);
	}

	private static UpdateErrandNoteRequest buildUpdateErrandNoteRequest() {
		return UpdateErrandNoteRequest.create()
			.withBody(BODY)
			.withModifiedBy(MODIFIED_BY)
			.withSubject(SUBJECT);
	}

	private static Note buildNote() {
		return new Note()
			.body(BODY)
			.caseId(CASE_ID)
			.clientId(CLIENT_ID)
			.context(CONTEXT)
			.created(CREATED)
			.createdBy(CREATED_BY)
			.id(ID)
			.modified(MODIFIED)
			.modifiedBy(MODIFIED_BY)
			.partyId(PARTY_ID)
			.role(ROLE)
			.subject(SUBJECT);
	}

	private static generated.se.sundsvall.notes.MetaData buildMetaData() {
		return new generated.se.sundsvall.notes.MetaData()
			.count(COUNT)
			.limit(LIMIT)
			.page(PAGE)
			.totalPages(TOTAL_PAGES)
			.totalRecords(TOTAL_RECORDS);
	}
}
