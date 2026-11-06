package se.sundsvall.supportmanagement.service.scheduler.supensions;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuspensionSchedulerTest {

	private static final String JOB_NAME = "jobName";

	@Mock
	private SuspensionWorker suspensionWorkerMock;

	@Mock
	private Dept44HealthUtility healthUtilityMock;

	private SuspensionScheduler suspensionScheduler;

	@BeforeEach
	void setUp() {
		suspensionScheduler = new SuspensionScheduler(suspensionWorkerMock, healthUtilityMock, JOB_NAME);
	}

	@Test
	void processExpiredSuspensions() {
		when(suspensionWorkerMock.getErrandIdsWithEndedSuspension()).thenReturn(List.of("id-1", "id-2"));

		suspensionScheduler.processExpiredSuspensions();

		verify(suspensionWorkerMock).announceSuspensionEnd("id-1");
		verify(suspensionWorkerMock).announceSuspensionEnd("id-2");
		verifyNoInteractions(healthUtilityMock);
	}

	@Test
	void processExpiredSuspensionsContinuesAfterAFailure() {
		when(suspensionWorkerMock.getErrandIdsWithEndedSuspension()).thenReturn(List.of("id-1", "id-2"));
		doThrow(new IllegalStateException("failure")).when(suspensionWorkerMock).announceSuspensionEnd("id-1");

		suspensionScheduler.processExpiredSuspensions();

		verify(suspensionWorkerMock).announceSuspensionEnd("id-2");
		verify(healthUtilityMock).setHealthIndicatorUnhealthy(JOB_NAME, "Error announcing the end of a suspension: failure");
		verifyNoMoreInteractions(healthUtilityMock);
	}
}
