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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
		when(attachmentRepositoryMock.findByHashIsNull(any(PageRequest.class))).thenReturn(Page.empty());

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(attachmentRepositoryMock).findByHashIsNull(any(PageRequest.class));
		verify(attachmentRepositoryMock, never()).saveAll(any());
		verifyNoMoreInteractions(attachmentRepositoryMock);
	}

	@Test
	void computeHashForAttachment() throws Exception {

		// Arrange
		final var content = "test content".getBytes();
		final var pageWithData = new PageImpl<>(List.of(attachmentEntityMock), PageRequest.of(0, 100), 1);
		when(attachmentRepositoryMock.findByHashIsNull(any(PageRequest.class)))
			.thenReturn(pageWithData)
			.thenReturn(Page.empty());
		when(attachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenReturn(new ByteArrayInputStream(content));

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(attachmentEntityMock).getAttachmentData();
		verify(attachmentEntityMock).setHash(any(String.class));
		verify(attachmentRepositoryMock).saveAll(List.of(attachmentEntityMock));
	}

	@Test
	void computeHashWhenBlobReadFails() throws Exception {

		// Arrange
		final var pageWithData = new PageImpl<>(List.of(attachmentEntityMock), PageRequest.of(0, 100), 1);
		when(attachmentRepositoryMock.findByHashIsNull(any(PageRequest.class)))
			.thenReturn(pageWithData)
			.thenReturn(Page.empty());
		when(attachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentEntityMock.getId()).thenReturn("attachment-id");
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenThrow(new SQLException("Blob read error"));

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(attachmentEntityMock).getAttachmentData();
		verify(attachmentRepositoryMock, never()).saveAll(any());
	}
}
