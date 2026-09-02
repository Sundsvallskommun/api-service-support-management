package se.sundsvall.supportmanagement.service.scheduler.job;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobSchedulerTest {

	private static final String JOB_NAME = "maintain_jobs";

	@Mock
	private JobWorker jobWorkerMock;

	@Mock
	private Dept44HealthUtility healthUtilityMock;

	@InjectMocks
	private JobScheduler scheduler;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(scheduler, "jobName", JOB_NAME);
	}

	@Test
	@DisplayName("Verification that a sweep finding nothing abandoned says nothing about the health of the service, leaving the scheduling aspect to report the run as the success it was")
	void maintainJobsWithNothingAbandoned() {
		when(jobWorkerMock.endAbandonedJobs()).thenReturn(Optional.empty());

		scheduler.maintainJobs();

		verify(jobWorkerMock).endAbandonedJobs();
		verifyNoInteractions(healthUtilityMock);
	}

	@Test
	@DisplayName("Verification that the account of what was abandoned is what the service is marked restricted with, so that whoever reads it sees which work stopped rather than only that something did")
	void maintainJobsWithAbandonedRuns() {
		final var account = "2 run(s) stopped being reported on and were ended here, leaving the work each was carrying out half done: "
			+ "ERRAND_PURGE job-1 in namespace NAMESPACE-1 for municipality 2281, last written to 2026-08-25T04:12:11+02:00.";
		when(jobWorkerMock.endAbandonedJobs()).thenReturn(Optional.of(account));

		scheduler.maintainJobs();

		verify(healthUtilityMock).setHealthIndicatorUnhealthy(JOB_NAME, account);
		verifyNoMoreInteractions(healthUtilityMock);
	}
}
