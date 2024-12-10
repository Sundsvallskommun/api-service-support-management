package se.sundsvall.supportmanagement.service.scheduler.supensions;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuspensionSchedulerTest {

	@Mock
	private SuspensionWorker suspensionWorkerMock;

	@InjectMocks
	private SuspensionScheduler suspensionScheduler;

	@Test
	void processExpiredSuspensions() {

		// Act
		suspensionScheduler.processExpiredSuspensions();

		// Verify
		verify(suspensionWorkerMock).processExpiredSuspensions();
		verifyNoMoreInteractions(suspensionWorkerMock);
	}

}
