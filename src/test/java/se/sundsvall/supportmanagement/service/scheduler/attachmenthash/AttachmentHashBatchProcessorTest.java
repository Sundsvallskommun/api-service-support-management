package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentHashBatchProcessorTest {

	private static final String ATTACHMENT_ID = "attachment-id";

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private EntityManager entityManagerMock;

	@Mock
	private AttachmentEntity attachmentEntityMock;

	@Mock
	private AttachmentDataEntity attachmentDataEntityMock;

	@Mock
	private Blob blobMock;

	@InjectMocks
	private AttachmentHashBatchProcessor batchProcessor;

	@Test
	void processBatchSuccessfully() throws Exception {

		// Arrange
		final var content = "test content".getBytes();
		when(attachmentRepositoryMock.findById(ATTACHMENT_ID)).thenReturn(Optional.of(attachmentEntityMock));
		when(attachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenReturn(new ByteArrayInputStream(content));

		// Act
		final var result = batchProcessor.processBatch(List.of(ATTACHMENT_ID));

		// Assert
		assertThat(result).isEqualTo(1);
		verify(attachmentEntityMock).setHash(any(String.class));
		verify(attachmentRepositoryMock).saveAndFlush(attachmentEntityMock);
		verify(entityManagerMock).detach(attachmentEntityMock);
	}

	@Test
	void processBatchWhenBlobReadFails() throws Exception {

		// Arrange
		when(attachmentRepositoryMock.findById(ATTACHMENT_ID)).thenReturn(Optional.of(attachmentEntityMock));
		when(attachmentEntityMock.getAttachmentData()).thenReturn(attachmentDataEntityMock);
		when(attachmentDataEntityMock.getFile()).thenReturn(blobMock);
		when(blobMock.getBinaryStream()).thenThrow(new SQLException("Blob read error"));

		// Act
		final var result = batchProcessor.processBatch(List.of(ATTACHMENT_ID));

		// Assert
		assertThat(result).isZero();
		verify(attachmentRepositoryMock, never()).saveAndFlush(any());
		verify(entityManagerMock).detach(attachmentEntityMock);
	}

	@Test
	void processBatchWhenAttachmentNotFound() {

		// Arrange
		when(attachmentRepositoryMock.findById(ATTACHMENT_ID)).thenReturn(Optional.empty());

		// Act
		final var result = batchProcessor.processBatch(List.of(ATTACHMENT_ID));

		// Assert
		assertThat(result).isZero();
		verify(attachmentRepositoryMock, never()).saveAndFlush(any());
		verifyNoInteractions(entityManagerMock);
	}
}
