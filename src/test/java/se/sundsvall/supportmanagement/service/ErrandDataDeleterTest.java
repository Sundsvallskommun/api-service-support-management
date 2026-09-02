package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.HandoverIdempotencyRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriberNotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * The deleter is what a single errand delete and a retention purge have in common, so what is worth pinning down is
 * that every table and every neighbouring service the errand reaches into is covered, and that the removal does not
 * come apart when one of those services is unavailable.
 */
@ExtendWith(MockitoExtension.class)
class ErrandDataDeleterTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String ERRAND_NUMBER = "KC-23090001";

	@Mock
	private ConversationService conversationServiceMock;

	@Mock
	private CommunicationService communicationServiceMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private NotesClient notesClientMock;

	@Mock
	private SubscriberNotificationRepository subscriberNotificationRepositoryMock;

	@Mock
	private HandoverIdempotencyRepository handoverIdempotencyRepositoryMock;

	@InjectMocks
	private ErrandDataDeleter deleter;

	@Test
	void deleteRelatedData() {
		final var entity = errandEntity();

		when(notesClientMock.findNotes(MUNICIPALITY_ID, null, null, ERRAND_ID, null, null, 1, 100))
			.thenReturn(new FindNotesResponse().notes(List.of(new Note().id("noteId"))));

		deleter.deleteRelatedData(entity, List.of("attachmentId"));

		verify(conversationServiceMock).deleteByErrandId(entity);
		verify(communicationServiceMock).deleteAllCommunicationsByErrandNumber(ERRAND_NUMBER, NAMESPACE, MUNICIPALITY_ID);
		verify(attachmentRepositoryMock).deleteById("attachmentId");
		verify(notesClientMock).findNotes(MUNICIPALITY_ID, null, null, ERRAND_ID, null, null, 1, 100);
		verify(notesClientMock).deleteNoteById(MUNICIPALITY_ID, "noteId");
		verify(subscriberNotificationRepositoryMock).deleteAllByErrandId(ERRAND_ID);
		verify(handoverIdempotencyRepositoryMock).deleteAllBySourceErrandIdOrNewErrandId(ERRAND_ID, ERRAND_ID);
		verifyNoMoreInteractions(conversationServiceMock, communicationServiceMock, attachmentRepositoryMock, notesClientMock,
			subscriberNotificationRepositoryMock, handoverIdempotencyRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a failing conversation removal is reported rather than swallowed, since what reaches here is a database write that has already doomed the transaction the removal runs in")
	void deleteRelatedDataWhenConversationRemovalFails() {
		final var entity = errandEntity();
		final var attachmentIds = List.of("attachmentId");

		doThrow(new RuntimeException("Could not remove conversations")).when(conversationServiceMock).deleteByErrandId(any());

		assertThatThrownBy(() -> deleter.deleteRelatedData(entity, attachmentIds))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Could not remove conversations");

		// Carrying on would remove the rest of the errand for a transaction that cannot commit, and answer the caller
		// with a removal that never happened.
		verifyNoInteractions(communicationServiceMock, attachmentRepositoryMock, notesClientMock,
			subscriberNotificationRepositoryMock, handoverIdempotencyRepositoryMock);
	}

	@Test
	@DisplayName("Verification that an unreachable notes service does not keep the rest of the errand in place")
	void deleteRelatedDataWhenNotesClientFails() {
		final var entity = errandEntity();

		when(notesClientMock.findNotes(anyString(), any(), any(), anyString(), any(), any(), anyInt(), anyInt()))
			.thenThrow(new RuntimeException("Notes service down"));

		assertThatNoException().isThrownBy(() -> deleter.deleteRelatedData(entity, emptyList()));

		verify(conversationServiceMock).deleteByErrandId(entity);
		verify(notesClientMock, never()).deleteNoteById(anyString(), anyString());
		verify(subscriberNotificationRepositoryMock).deleteAllByErrandId(ERRAND_ID);
		verify(handoverIdempotencyRepositoryMock).deleteAllBySourceErrandIdOrNewErrandId(ERRAND_ID, ERRAND_ID);
	}

	@Test
	@DisplayName("Verification that an errand without attachments is handled rather than reaching a null collection")
	void deleteRelatedDataWithoutAttachments() {
		final var entity = errandEntity();

		when(notesClientMock.findNotes(anyString(), any(), any(), anyString(), any(), any(), anyInt(), anyInt()))
			.thenReturn(new FindNotesResponse().notes(emptyList()));

		assertThatNoException().isThrownBy(() -> deleter.deleteRelatedData(entity, null));

		verify(attachmentRepositoryMock, never()).deleteById(anyString());
		verify(subscriberNotificationRepositoryMock).deleteAllByErrandId(ERRAND_ID);
		verify(handoverIdempotencyRepositoryMock).deleteAllBySourceErrandIdOrNewErrandId(ERRAND_ID, ERRAND_ID);
	}

	@Test
	@DisplayName("Verification that a removed attachment is taken out of the errand as well, since one left behind is written back by the cascade with its data reference nulled")
	void deleteRelatedDataTakesAttachmentsOutOfTheErrand() {
		final var removed = AttachmentEntity.create().withId("attachmentId");
		final var kept = AttachmentEntity.create().withId("otherAttachmentId");
		final var entity = errandEntity().withAttachments(new ArrayList<>(List.of(removed, kept)));

		when(notesClientMock.findNotes(anyString(), any(), any(), anyString(), any(), any(), anyInt(), anyInt()))
			.thenReturn(new FindNotesResponse().notes(emptyList()));

		deleter.deleteRelatedData(entity, List.of("attachmentId"));

		assertThat(entity.getAttachments()).containsExactly(kept);
		verify(attachmentRepositoryMock).deleteById("attachmentId");
		verify(attachmentRepositoryMock, never()).deleteById("otherAttachmentId");
	}

	@Test
	@DisplayName("Verification that an errand carrying more notes than fit in one page has all of them removed, not just the first page")
	void deleteRelatedDataWithMoreNotesThanOnePage() {
		final var entity = errandEntity();

		when(notesClientMock.findNotes(anyString(), any(), any(), anyString(), any(), any(), anyInt(), anyInt()))
			.thenReturn(new FindNotesResponse().notes(notes(100)))
			.thenReturn(new FindNotesResponse().notes(notes(7)));

		deleter.deleteRelatedData(entity, emptyList());

		// The full page is what says there may be more, and the page that did not fill up is what says there is not.
		verify(notesClientMock, times(2)).findNotes(MUNICIPALITY_ID, null, null, ERRAND_ID, null, null, 1, 100);
		verify(notesClientMock, times(107)).deleteNoteById(eq(MUNICIPALITY_ID), anyString());
	}

	private static List<Note> notes(final int count) {
		return IntStream.range(0, count)
			.mapToObj(index -> new Note().id("noteId-" + index))
			.toList();
	}

	private static ErrandEntity errandEntity() {
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withErrandNumber(ERRAND_NUMBER);
	}
}
