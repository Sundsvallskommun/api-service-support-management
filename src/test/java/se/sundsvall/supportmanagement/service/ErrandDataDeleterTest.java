package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.notes.FindNotesResponse;
import generated.se.sundsvall.notes.Note;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.AttachmentDataRepository;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.HandoverIdempotencyRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriberNotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataIdProjection;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
 * <p>
 * The chunked deleter is used for real rather than mocked, since handing the ids over is part of what these tests are
 * checking.
 */
@ExtendWith(MockitoExtension.class)
class ErrandDataDeleterTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String ERRAND_NUMBER = "KC-23090001";
	private static final String ATTACHMENT_ID = "attachmentId";
	private static final int ATTACHMENT_DATA_ID = 101;

	@Mock
	private ConversationService conversationServiceMock;

	@Mock
	private CommunicationService communicationServiceMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private AttachmentDataRepository attachmentDataRepositoryMock;

	@Mock
	private NotesClient notesClientMock;

	@Mock
	private SubscriberNotificationRepository subscriberNotificationRepositoryMock;

	@Mock
	private HandoverIdempotencyRepository handoverIdempotencyRepositoryMock;

	@Mock
	private EntityManager entityManagerMock;

	private ErrandDataDeleter deleter;

	@BeforeEach
	void setUp() {
		deleter = new ErrandDataDeleter(conversationServiceMock, communicationServiceMock, attachmentRepositoryMock,
			attachmentDataRepositoryMock, notesClientMock, subscriberNotificationRepositoryMock,
			handoverIdempotencyRepositoryMock, entityManagerMock, new ChunkedDeleter(entityManagerMock));
	}

	@Test
	void deleteRelatedData() {
		final var entity = errandEntity();

		when(attachmentRepositoryMock.findByIdIn(List.of(ATTACHMENT_ID))).thenReturn(List.of(dataId(ATTACHMENT_DATA_ID)));
		when(notesClientMock.findNotes(MUNICIPALITY_ID, null, null, ERRAND_ID, null, null, 1, 100))
			.thenReturn(new FindNotesResponse().notes(List.of(new Note().id("noteId"))));

		deleter.deleteRelatedData(entity, List.of(ATTACHMENT_ID));

		verify(conversationServiceMock).deleteByErrandId(entity);
		verify(communicationServiceMock).deleteAllCommunicationsByErrandNumber(ERRAND_NUMBER, NAMESPACE, MUNICIPALITY_ID);
		verify(attachmentRepositoryMock).findByIdIn(List.of(ATTACHMENT_ID));
		verify(attachmentRepositoryMock).deleteAllByIdInBatch(List.of(ATTACHMENT_ID));
		verify(attachmentDataRepositoryMock).deleteAllByIdInBatch(List.of(ATTACHMENT_DATA_ID));
		verify(notesClientMock).findNotes(MUNICIPALITY_ID, null, null, ERRAND_ID, null, null, 1, 100);
		verify(notesClientMock).deleteNoteById(MUNICIPALITY_ID, "noteId");
		verify(subscriberNotificationRepositoryMock).deleteAllByErrandId(ERRAND_ID);
		verify(handoverIdempotencyRepositoryMock).deleteAllBySourceErrandIdOrNewErrandId(ERRAND_ID, ERRAND_ID);
		verifyNoMoreInteractions(conversationServiceMock, communicationServiceMock, attachmentRepositoryMock,
			attachmentDataRepositoryMock, notesClientMock, subscriberNotificationRepositoryMock, handoverIdempotencyRepositoryMock);
	}

	@Test
	@DisplayName("Verification that a failing conversation removal is reported rather than swallowed, since what reaches here is a database write that has already doomed the transaction the removal runs in")
	void deleteRelatedDataWhenConversationRemovalFails() {
		final var entity = errandEntity();
		final var attachmentIds = List.of(ATTACHMENT_ID);

		doThrow(new RuntimeException("Could not remove conversations")).when(conversationServiceMock).deleteByErrandId(any());

		assertThatThrownBy(() -> deleter.deleteRelatedData(entity, attachmentIds))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Could not remove conversations");

		// Carrying on would remove the rest of the errand for a transaction that cannot commit, and answer the caller
		// with a removal that never happened.
		verifyNoInteractions(communicationServiceMock, attachmentRepositoryMock, attachmentDataRepositoryMock, notesClientMock,
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

		// Nothing to remove is not the same as removing nothing: an errand without attachments must not cost a query at
		// all, neither for the ids of the data rows nor for an empty batch.
		verifyNoInteractions(attachmentRepositoryMock, attachmentDataRepositoryMock);
		verify(subscriberNotificationRepositoryMock).deleteAllByErrandId(ERRAND_ID);
		verify(handoverIdempotencyRepositoryMock).deleteAllBySourceErrandIdOrNewErrandId(ERRAND_ID, ERRAND_ID);
	}

	@Test
	@DisplayName("Verification that the attachments and the files they hold are removed by naming the rows rather than loading them, after the communications and with the errand out of the persistence context")
	void deleteRelatedDataRemovesAttachmentsWithTheErrandDetached() {
		final var removed = AttachmentEntity.create().withId(ATTACHMENT_ID);
		final var kept = AttachmentEntity.create().withId("otherAttachmentId");
		final var entity = errandEntity().withAttachments(new ArrayList<>(List.of(removed, kept)));

		when(attachmentRepositoryMock.findByIdIn(List.of(ATTACHMENT_ID))).thenReturn(List.of(dataId(ATTACHMENT_DATA_ID)));
		when(notesClientMock.findNotes(anyString(), any(), any(), anyString(), any(), any(), anyInt(), anyInt()))
			.thenReturn(new FindNotesResponse().notes(emptyList()));

		deleter.deleteRelatedData(entity, List.of(ATTACHMENT_ID));

		// The communications first: one can arrive carrying an attachment whose blob the copy kept on the errand points
		// at too, and removing the errand's copy takes that blob with it. Then the errand out of the context, since an
		// attachment left in the collection of a managed errand is written back by the cascade with its data reference
		// nulled. Then the ids of the data rows, which nothing can say once the attachments are gone. Only then the
		// rows, attachments before the data they point at, since the attachments hold the foreign key.
		final var inOrder = inOrder(communicationServiceMock, entityManagerMock, attachmentRepositoryMock, attachmentDataRepositoryMock);
		inOrder.verify(communicationServiceMock).deleteAllCommunicationsByErrandNumber(ERRAND_NUMBER, NAMESPACE, MUNICIPALITY_ID);
		inOrder.verify(entityManagerMock).detach(entity);
		inOrder.verify(attachmentRepositoryMock).findByIdIn(List.of(ATTACHMENT_ID));
		inOrder.verify(attachmentRepositoryMock).deleteAllByIdInBatch(List.of(ATTACHMENT_ID));
		inOrder.verify(attachmentDataRepositoryMock).deleteAllByIdInBatch(List.of(ATTACHMENT_DATA_ID));

		// Loading an attachment to remove it is what this exists to avoid: the removal reaches its data through the
		// cascade, and reaching a data row means holding the whole file it carries.
		verify(attachmentRepositoryMock, never()).deleteById(anyString());
		verify(attachmentRepositoryMock, never()).deleteAllById(anyList());
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

	private static AttachmentDataIdProjection dataId(final int id) {
		return () -> id;
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
