package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
	private PlatformTransactionManager transactionManagerMock;

	@Mock
	private TransactionStatus transactionStatusMock;

	@Mock
	private AttachmentEntity attachmentEntityMock;

	@Mock
	private AttachmentDataEntity attachmentDataEntityMock;

	@Mock
	private Blob blobMock;

	private AttachmentHashWorker attachmentHashWorker;

	@BeforeEach
	void setUp() {
		attachmentHashWorker = new AttachmentHashWorker(attachmentRepositoryMock, transactionManagerMock, BATCH_SIZE);
	}

	@Test
	void computeHashWhenNoAttachmentsWithoutHash() {

		// Arrange
		when(attachmentRepositoryMock.findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE))).thenReturn(Collections.emptyList());

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(attachmentRepositoryMock).findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE));
		verifyNoMoreInteractions(attachmentRepositoryMock);
	}

	@Test
	void computeHashForAttachmentSuccessfully() throws Exception {

		// Arrange
		final var content = "test content".getBytes();
		when(transactionManagerMock.getTransaction(any())).thenReturn(transactionStatusMock);
		when(attachmentRepositoryMock.findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE))).thenReturn(List.of(ATTACHMENT_ID_1));
		when(attachmentRepositoryMock.findById(ATTACHMENT_ID_1)).thenReturn(Optional.of(attachmentEntityMock));
		when(attachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenReturn(new ByteArrayInputStream(content));

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(attachmentEntityMock).setHash(any(String.class));
		verify(attachmentRepositoryMock).saveAndFlush(attachmentEntityMock);
	}

	@Test
	void computeHashWhenAttachmentNotFound() {

		// Arrange
		when(transactionManagerMock.getTransaction(any())).thenReturn(transactionStatusMock);
		when(attachmentRepositoryMock.findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE))).thenReturn(List.of(ATTACHMENT_ID_1));
		when(attachmentRepositoryMock.findById(ATTACHMENT_ID_1)).thenReturn(Optional.empty());

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(attachmentRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void computeHashContinuesAfterBlobReadFailure() throws SQLException {

		// Arrange
		when(transactionManagerMock.getTransaction(any())).thenReturn(transactionStatusMock);
		when(attachmentRepositoryMock.findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE))).thenReturn(List.of(ATTACHMENT_ID_1, ATTACHMENT_ID_2));
		when(attachmentRepositoryMock.findById(ATTACHMENT_ID_1)).thenReturn(Optional.of(attachmentEntityMock));
		when(attachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenThrow(new SQLException("Blob read error"));
		when(attachmentRepositoryMock.findById(ATTACHMENT_ID_2)).thenReturn(Optional.empty());

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify - should continue to next attachment despite exception
		verify(attachmentRepositoryMock).findById(ATTACHMENT_ID_1);
		verify(attachmentRepositoryMock).findById(ATTACHMENT_ID_2);
		verify(attachmentRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void computeHashStopsWhenThreadIsInterrupted() {

		// Arrange
		when(attachmentRepositoryMock.findIdsByHashIsNull(Pageable.ofSize(BATCH_SIZE))).thenReturn(List.of(ATTACHMENT_ID_1, ATTACHMENT_ID_2));
		Thread.currentThread().interrupt();

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify - should stop before processing any attachment
		verify(attachmentRepositoryMock, never()).findById(any());

		// Clear interrupt flag so it doesn't leak into other tests
		Thread.interrupted();
	}
}
