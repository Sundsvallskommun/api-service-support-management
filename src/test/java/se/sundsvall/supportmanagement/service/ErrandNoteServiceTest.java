package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CASE_ID;
import static se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper.toCreateNoteRequest;

import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.filter.ExecutingUserSupplier;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.note.FindErrandNotesRequest;
import se.sundsvall.supportmanagement.api.model.note.UpdateErrandNoteRequest;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
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

	private static final String CASE_ID = "caseId";

	private static final ErrandEntity ERRAND_ENTITY = new ErrandEntity().withExternalTags(List.of(DbExternalTag.create().withKey(EXTERNAL_TAG_KEY_CASE_ID).withValue(CASE_ID)));

	private static final String CONTEXT = "context";

	private static final String CREATED_BY = "createdBy";

	private static final String NOTE_ID = "noteId";

	private static final String MODIFIED_BY = "modifiedBy";

	private static final String PARTY_ID = "partyId";

	private static final String ROLE = "role";

	private static final String SUBJECT = "subject";

	private static final String APPLICATION_NAME = "applicationName";

	private static final String EXECUTING_USER = "executingUser";

	@Mock
	private ErrandsRepository repositoryMock;

	@Mock
	private NotesClient notesClientMock;

	@Mock
	private HttpHeaders httpHeadersMock;

	@Mock
	private ResponseEntity<Void> responseEntityWithVoidMock;

	@Mock
	private ResponseEntity<Note> responseEntityWithNoteMock;

	@Mock
	private ExecutingUserSupplier executingUserSupplierMock;

	@Mock
	private EventService eventServiceMock;

	@InjectMocks
	private ErrandNoteService service;

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

	@Test
	void createErrandNote() {

		// Setup
		final var locationUrl = "http://localhost/2281/notes/" + NOTE_ID;
		final var errandNote = buildCreateErrandNoteRequest();
		final var createNoteRequest = toCreateNoteRequest(ERRAND_ID, APPLICATION_NAME, errandNote);

		// Mock
		ReflectionTestUtils.setField(service, "clientId", APPLICATION_NAME);

		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(repositoryMock.getReferenceById(ERRAND_ID)).thenReturn(new ErrandEntity().withExternalTags(List.of(DbExternalTag.create().withKey(EXTERNAL_TAG_KEY_CASE_ID).withValue(CASE_ID))));
		when(executingUserSupplierMock.getAdUser()).thenReturn(EXECUTING_USER);
		when(notesClientMock.createNote(EXECUTING_USER, MUNICIPALITY_ID, createNoteRequest)).thenReturn(responseEntityWithVoidMock);
		when(responseEntityWithVoidMock.getHeaders()).thenReturn(httpHeadersMock);
		when(httpHeadersMock.get(anyString())).thenAnswer(
			(Answer<List<String>>) invocation -> {
				final String argument = invocation.getArgument(0);
				return switch (argument) {
					case "x-current-revision" -> List.of("currentRevision");
					case "x-current-version" -> List.of("0");
					case LOCATION -> List.of(locationUrl);
					default -> null;
				};
			});

		// Call
		final var result = service.createErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, errandNote);

		// Assertions and verifications
		assertThat(result).isEqualTo(NOTE_ID);

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(repositoryMock).getReferenceById(ERRAND_ID);
		verify(notesClientMock).createNote(EXECUTING_USER, MUNICIPALITY_ID, createNoteRequest);
		verify(eventServiceMock).createErrandNoteEvent(EventType.CREATE, "Ärendenotering har skapats.", ERRAND_ID, ERRAND_ENTITY, NOTE_ID, Revision.create().withId("currentRevision").withVersion(0), null);
	}

	@Test
	void readErrandNote() {

		// Mock
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(notesClientMock.findNoteById(MUNICIPALITY_ID, NOTE_ID)).thenReturn(new Note());

		// Call
		final var result = service.readErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID);

		// Assertions and verifications
		assertThat(result).isNotNull();

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(notesClientMock).findNoteById(MUNICIPALITY_ID, NOTE_ID);
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
		when(notesClientMock.findNotes(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
			.thenReturn(new FindNotesResponse().notes(List.of(new Note())));

		// Call
		final var result = service.findErrandNotes(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, findErrandNotesRequest);

		// Assertions and verifications
		assertThat(result).isNotNull();
		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(notesClientMock).findNotes(MUNICIPALITY_ID, CONTEXT, ROLE, ERRAND_ID, APPLICATION_NAME, PARTY_ID, PAGE, LIMIT);
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
		when(repositoryMock.getReferenceById(ERRAND_ID)).thenReturn(new ErrandEntity().withExternalTags(List.of(DbExternalTag.create().withKey(EXTERNAL_TAG_KEY_CASE_ID).withValue(CASE_ID))));
		when(executingUserSupplierMock.getAdUser()).thenReturn(EXECUTING_USER);
		when(notesClientMock.updateNoteById(EXECUTING_USER, MUNICIPALITY_ID, NOTE_ID, updateNoteRequest)).thenReturn(responseEntityWithNoteMock);
		when(responseEntityWithNoteMock.getHeaders()).thenReturn(httpHeadersMock);
		when(responseEntityWithNoteMock.getBody()).thenReturn(new Note());

		when(httpHeadersMock.get(anyString())).thenAnswer(
			(Answer<List<String>>) invocation -> {
				final String argument = invocation.getArgument(0);
				return switch (argument) {
					case "x-current-revision" -> List.of("currentRevision");
					case "x-current-version" -> List.of("1");
					case "x-previous-revision" -> List.of("previousRevision");
					case "x-previous-version" -> List.of("0");
					default -> null;
				};
			});

		// Call
		final var result = service.updateErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID, errandNote);

		// Assertions and verifications
		assertThat(result).isNotNull();

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(repositoryMock).getReferenceById(ERRAND_ID);
		verify(notesClientMock).updateNoteById(EXECUTING_USER, MUNICIPALITY_ID, NOTE_ID, updateNoteRequest);
		verify(eventServiceMock).createErrandNoteEvent(EventType.UPDATE, "Ärendenotering har uppdaterats.", ERRAND_ID, ERRAND_ENTITY, NOTE_ID, Revision.create().withId("currentRevision").withVersion(1), Revision.create().withId("previousRevision")
			.withVersion(0));
	}

	@Test
	void updateErrandWithNoChangedNote() {

		// Setup
		final var errandNote = buildUpdateErrandNoteRequest();
		final var updateNoteRequest = ErrandNoteMapper.toUpdateNoteRequest(errandNote);

		// Mock
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(executingUserSupplierMock.getAdUser()).thenReturn(EXECUTING_USER);
		when(notesClientMock.updateNoteById(EXECUTING_USER, MUNICIPALITY_ID, NOTE_ID, updateNoteRequest)).thenReturn(responseEntityWithNoteMock);
		when(responseEntityWithNoteMock.getHeaders()).thenReturn(httpHeadersMock);
		when(responseEntityWithNoteMock.getBody()).thenReturn(new Note());

		// Call
		final var result = service.updateErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID, errandNote);

		// Assertions and verifications
		assertThat(result).isNotNull();

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(notesClientMock).updateNoteById(EXECUTING_USER, MUNICIPALITY_ID, NOTE_ID, updateNoteRequest);
		verify(repositoryMock, never()).getReferenceById(ERRAND_ID);
		verify(eventServiceMock, never()).createErrandNoteEvent(any(), any(), any(), any(), any(), any(), any());
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
		when(repositoryMock.getReferenceById(ERRAND_ID)).thenReturn(new ErrandEntity().withExternalTags(List.of(DbExternalTag.create().withKey(EXTERNAL_TAG_KEY_CASE_ID).withValue(CASE_ID))));
		when(executingUserSupplierMock.getAdUser()).thenReturn(EXECUTING_USER);
		when(notesClientMock.deleteNoteById(EXECUTING_USER, MUNICIPALITY_ID, NOTE_ID)).thenReturn(responseEntityWithVoidMock);
		when(responseEntityWithVoidMock.getHeaders()).thenReturn(httpHeadersMock);
		when(httpHeadersMock.get(anyString())).thenAnswer(
			(Answer<List<String>>) invocation -> {
				final String argument = invocation.getArgument(0);
				return switch (argument) {
					case "x-current-revision" -> List.of("currentRevision");
					case "x-current-version" -> List.of("1");
					default -> null;
				};
			});

		// Call
		service.deleteErrandNote(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, NOTE_ID);

		// Assertions and verifications
		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(repositoryMock).getReferenceById(ERRAND_ID);
		verify(notesClientMock).deleteNoteById(EXECUTING_USER, MUNICIPALITY_ID, NOTE_ID);
		verify(eventServiceMock).createErrandNoteEvent(EventType.DELETE, "Ärendenotering har raderats.", ERRAND_ID, ERRAND_ENTITY, NOTE_ID, Revision.create().withId("currentRevision").withVersion(1), null);
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

}
