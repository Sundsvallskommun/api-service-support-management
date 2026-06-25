package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AttachmentHashSchedulerTest {

	@Mock
	private AttachmentHashWorker attachmentHashWorkerMock;

	@InjectMocks
	private AttachmentHashScheduler attachmentHashScheduler;

	@Test
	void computeAttachmentHashes() {

		// Act
		attachmentHashScheduler.computeAttachmentHashes();

		// Verify
		verify(attachmentHashWorkerMock).computeHashForAttachmentsWithoutHash();
		verifyNoMoreInteractions(attachmentHashWorkerMock);
	}
}
