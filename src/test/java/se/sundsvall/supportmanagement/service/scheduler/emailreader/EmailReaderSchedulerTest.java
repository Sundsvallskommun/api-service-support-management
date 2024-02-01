package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailReaderSchedulerTest {

	@Mock
	private EmailReaderWorker emailReaderWorkerMock;

	@InjectMocks
	private EmailReaderScheduler emailReaderScheduler;

	@Test
	void getAndProcessEmails() {
		// Act
		emailReaderScheduler.getAndProcessEmails();
		// Verify
		verify(emailReaderWorkerMock).getAndProcessEmails();
		verifyNoMoreInteractions(emailReaderWorkerMock);
	}

}
