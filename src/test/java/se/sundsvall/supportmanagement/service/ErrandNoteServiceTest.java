package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toCreateNoteRequest;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.zalando.problem.ThrowableProblem;

import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesRequest;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;
import se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper;

@ExtendWith(MockitoExtension.class)
class ErrandNoteServiceTest {

	private static final int LIMIT = 99;
	private static final int PAGE = 2;
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String BODY = "body";
	private static final String CONTEXT = "context";
	private static final String CREATED_BY = "createdBy";
	private static final String NOTE_ID = "noteId";
	private static final String MODIFIED_BY = "modifiedBy";
	private static final String PARTY_ID = "partyId";
	private static final String ROLE = "role";
	private static final String SUBJECT = "subject";
	private static final String APPLICATION_NAME = "applicationName";

	@Mock
	private ErrandsRepository repositoryMock;

	@Mock
	private NotesClient notesClientMock;

	@Mock
	private HttpHeaders httpHeadersMock;

	@Mock
	private ResponseEntity<Void> responseEntityMock;

	@InjectMocks
	private ErrandNoteService service;

	@Test
	void createErrandNote() {

		// Setup
		final var locationUrl = "http://localhost/notes/" + NOTE_ID;
		final var errandNote = buildCreateErrandNoteRequest();
		final var createNoteRequest = toCreateNoteRequest(ERRAND_ID, APPLICATION_NAME, errandNote);

		// Mock
		ReflectionTestUtils.setField(service, "clientId", APPLICATION_NAME);
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(httpHeadersMock.get(LOCATION)).thenReturn(List.of(locationUrl));
		when(responseEntityMock.getHeaders()).thenReturn(httpHeadersMock);
		when(notesClientMock.createNote(createNoteRequest)).thenReturn(responseEntityMock);

		// Call
		final var result = service.createErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, errandNote);

		// Assertions and verifications
		assertThat(result).isEqualTo(NOTE_ID);

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(notesClientMock).createNote(createNoteRequest);
	}

	@Test
	void readErrandNote() {

		// Mock
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(notesClientMock.findNoteById(NOTE_ID)).thenReturn(new Note());

		// Call
		final var result = service.readErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID);

		// Assertions and verifications
		assertThat(result).isNotNull();

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(notesClientMock).findNoteById(NOTE_ID);
	}

	@Test
	void readErrandNoteErrandNotFound() {

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.readErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(notesClientMock);
	}

	@Test
	void findErrandNotes() {

		// Setup
		final var findErrandNotesRequest = FindErrandNotesRequest.create()
			.withContext(CONTEXT)
			.withLimit(LIMIT)
			.withPage(PAGE)
			.withPartyId(PARTY_ID)
			.withRole(ROLE);

		// Mock
		ReflectionTestUtils.setField(service, "clientId", APPLICATION_NAME);
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(notesClientMock.findNotes(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
			.thenReturn(new FindNotesResponse().notes(List.of(new Note())));

		// Call
		final var result = service.findErrandNotes(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, findErrandNotesRequest);

		// Assertions and verifications
		assertThat(result).isNotNull();
		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(notesClientMock).findNotes(CONTEXT, ROLE, ERRAND_ID, APPLICATION_NAME, PARTY_ID, PAGE, LIMIT);
	}

	@Test
	void findErrandNotesErrandNotFound() {

		// Setup
		final var findErrandNotesRequest = FindErrandNotesRequest.create()
			.withContext(CONTEXT)
			.withLimit(LIMIT)
			.withPage(PAGE)
			.withPartyId(PARTY_ID)
			.withRole(ROLE);

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.findErrandNotes(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, findErrandNotesRequest));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(notesClientMock);
	}

	@Test
	void updateErrandNote() {

		// Setup
		final var errandNote = buildUpdateErrandNoteRequest();
		final var updateNoteRequest = ErrandNoteMapper.toUpdateNoteRequest(errandNote);

		// Mock
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(notesClientMock.updateNoteById(NOTE_ID, updateNoteRequest)).thenReturn(new Note());

		// Call
		final var result = service.updateErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID, errandNote);

		// Assertions and verifications
		assertThat(result).isNotNull();

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(notesClientMock).updateNoteById(NOTE_ID, updateNoteRequest);
	}

	@Test
	void updateErrandNoteErrandNotFound() {

		// Setup
		final var errandNote = buildUpdateErrandNoteRequest();

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.updateErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID, errandNote));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(notesClientMock);
	}

	@Test
	void deleteErrandNote() {

		// Mock
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(notesClientMock.deleteNoteById(NOTE_ID)).thenReturn(responseEntityMock);

		// Call
		service.deleteErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID);

		// Assertions and verifications
		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(notesClientMock).deleteNoteById(NOTE_ID);
	}

	@Test
	void deleteErrandNoteErrandNotFound() {

		// Call
		final var exception = assertThrows(ThrowableProblem.class, () -> service.deleteErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID));

		// Assertions and verifications
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(notesClientMock);
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
}
