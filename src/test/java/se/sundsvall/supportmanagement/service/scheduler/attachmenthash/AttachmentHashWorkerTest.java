package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

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
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentHashWorkerTest {

	private static final String ATTACHMENT_ID = "attachment-id";

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private AttachmentHashBatchProcessor batchProcessorMock;

	@Mock
	private AttachmentEntity attachmentEntityMock;

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
		verifyNoInteractions(batchProcessorMock);
		verifyNoMoreInteractions(attachmentRepositoryMock);
	}

	@Test
	void computeHashForAttachment() {

		// Arrange
		final var pageWithData = new PageImpl<>(List.of(attachmentEntityMock), PageRequest.of(0, 100), 1);
		when(attachmentEntityMock.getId()).thenReturn(ATTACHMENT_ID);
		when(attachmentRepositoryMock.findByHashIsNull(any(PageRequest.class)))
			.thenReturn(pageWithData)
			.thenReturn(Page.empty());
		when(batchProcessorMock.processBatch(any())).thenReturn(1);

		// Act
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();

		// Verify
		verify(batchProcessorMock).processBatch(List.of(ATTACHMENT_ID));
		verifyNoMoreInteractions(batchProcessorMock);
	}
}
