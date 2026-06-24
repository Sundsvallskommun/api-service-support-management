package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentHashWorkerTest {

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private AttachmentEntity attachmentEntityMock;

	@Mock
	private AttachmentDataEntity attachmentDataEntityMock;

	@Mock
	private Blob blobMock;

	@InjectMocks
	private AttachmentHashWorker attachmentHashWorker;

	@Test
	void computeHashWhenNoAttachmentsWithoutHash() {

		// Arrange
		when(attachmentRepositoryMock.findByHashIsNull()).thenReturn(emptyList());

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(attachmentRepositoryMock).findByHashIsNull();
		verify(attachmentRepositoryMock, never()).save(any());
		verifyNoMoreInteractions(attachmentRepositoryMock);
	}

	@Test
	void computeHashForAttachment() throws Exception {

		// Arrange
		final var content = "test content".getBytes();
		when(attachmentRepositoryMock.findByHashIsNull()).thenReturn(List.of(attachmentEntityMock));
		when(attachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenReturn(new ByteArrayInputStream(content));

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(attachmentRepositoryMock).findByHashIsNull();
		verify(attachmentEntityMock).getAttachmentData();
		verify(attachmentEntityMock).setHash(any(String.class));
		verify(attachmentRepositoryMock).save(attachmentEntityMock);
		verifyNoMoreInteractions(attachmentRepositoryMock);
	}

	@Test
	void computeHashWhenBlobReadFails() throws Exception {

		// Arrange
		when(attachmentRepositoryMock.findByHashIsNull()).thenReturn(List.of(attachmentEntityMock));
		when(attachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentEntityMock.getId()).thenReturn("attachment-id");
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenThrow(new SQLException("Blob read error"));

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(attachmentRepositoryMock).findByHashIsNull();
		verify(attachmentEntityMock).getAttachmentData();
		verify(attachmentRepositoryMock, never()).save(any());
		verifyNoMoreInteractions(attachmentRepositoryMock);
	}
}
