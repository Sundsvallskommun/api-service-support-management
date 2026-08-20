package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AttachmentHashSchedulerTest {

	@Mock
	private AttachmentHashWorker attachmentHashWorkerMock;

	@Test
	void computeAttachmentHashesWhenEnabled() {

		// Arrange
		final var scheduler = new AttachmentHashScheduler(attachmentHashWorkerMock, true);

		// Act
		scheduler.computeAttachmentHashes();

		// Verify
		verify(attachmentHashWorkerMock).computeHashForAttachmentsWithoutHash();
		verifyNoMoreInteractions(attachmentHashWorkerMock);
	}

	@Test
	void computeAttachmentHashesWhenDisabled() {

		// Arrange
		final var scheduler = new AttachmentHashScheduler(attachmentHashWorkerMock, false);

		// Act
		scheduler.computeAttachmentHashes();

		// Verify
		verifyNoInteractions(attachmentHashWorkerMock);
	}
}
