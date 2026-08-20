package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentHashWorkerTest {

	private static final int BATCH_SIZE = 250;
	private static final String ATTACHMENT_ID_1 = "attachment-id-1";
	private static final String ATTACHMENT_ID_2 = "attachment-id-2";

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private AttachmentHashBatchProcessor batchProcessorMock;

	private AttachmentHashWorker attachmentHashWorker;

	@Test
	void computeHashWhenNoAttachmentsWithoutHash() {

		// Arrange
		attachmentHashWorker = new AttachmentHashWorker(attachmentRepositoryMock, batchProcessorMock, BATCH_SIZE);
		when(attachmentRepositoryMock.findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE))).thenReturn(Collections.emptyList());

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(attachmentRepositoryMock).findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE));
		verifyNoInteractions(batchProcessorMock);
		verifyNoMoreInteractions(attachmentRepositoryMock);
	}

	@Test
	void computeHashForAttachment() {

		// Arrange
		attachmentHashWorker = new AttachmentHashWorker(attachmentRepositoryMock, batchProcessorMock, BATCH_SIZE);
		when(attachmentRepositoryMock.findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE))).thenReturn(List.of(ATTACHMENT_ID_1));
		when(batchProcessorMock.processAttachment(ATTACHMENT_ID_1)).thenReturn(true);

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(batchProcessorMock).processAttachment(ATTACHMENT_ID_1);
		verifyNoMoreInteractions(batchProcessorMock);
	}

	@Test
	void computeHashProcessesEachAttachmentOnlyOnce() {

		// Arrange
		attachmentHashWorker = new AttachmentHashWorker(attachmentRepositoryMock, batchProcessorMock, BATCH_SIZE);
		when(attachmentRepositoryMock.findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE))).thenReturn(List.of(ATTACHMENT_ID_1, ATTACHMENT_ID_2));
		when(batchProcessorMock.processAttachment(ATTACHMENT_ID_1)).thenReturn(false);
		when(batchProcessorMock.processAttachment(ATTACHMENT_ID_2)).thenReturn(true);

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify - each attachment processed exactly once, even if it fails
		verify(batchProcessorMock, times(1)).processAttachment(ATTACHMENT_ID_1);
		verify(batchProcessorMock, times(1)).processAttachment(ATTACHMENT_ID_2);
		verifyNoMoreInteractions(batchProcessorMock);
	}

	@Test
	void computeHashContinuesAfterException() {

		// Arrange
		attachmentHashWorker = new AttachmentHashWorker(attachmentRepositoryMock, batchProcessorMock, BATCH_SIZE);
		when(attachmentRepositoryMock.findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE))).thenReturn(List.of(ATTACHMENT_ID_1, ATTACHMENT_ID_2));
		when(batchProcessorMock.processAttachment(ATTACHMENT_ID_1)).thenThrow(new RuntimeException("Connection error"));
		when(batchProcessorMock.processAttachment(ATTACHMENT_ID_2)).thenReturn(true);

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify - should continue to next attachment despite exception
		verify(batchProcessorMock).processAttachment(ATTACHMENT_ID_1);
		verify(batchProcessorMock).processAttachment(ATTACHMENT_ID_2);
	}
}
