package se.sundsvall.supportmanagement.service.scheduler.job;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.config.JobProperties;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.service.JobService;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.ERRAND_PURGE;

@ExtendWith(MockitoExtension.class)
class JobWorkerTest {

	private static final Duration STALE_AFTER = Duration.ofDays(3);
	private static final OffsetDateTime LAST_WRITTEN_TO = OffsetDateTime.parse("2026-08-25T04:12:11+02:00");

	@Mock
	private JobService jobServiceMock;

	@Test
	@DisplayName("Verification that what is handed back names the work that stopped rather than counting it, since a service marked restricted is only actionable if it says which run stopped and where")
	void endAbandonedJobs() {
		final var worker = worker();
		when(jobServiceMock.failStaleJobs(STALE_AFTER)).thenReturn(List.of(abandonedJob("job-1")));

		assertThat(worker.endAbandonedJobs())
			.contains("1 run(s) stopped being reported on and were ended here, leaving the work each was carrying out half done: "
				+ "ERRAND_PURGE job-1 in namespace NAMESPACE-1 for municipality 2281, last written to 2026-08-25T04:12:11+02:00.");

		verify(jobServiceMock).failStaleJobs(STALE_AFTER);
	}

	@Test
	@DisplayName("Verification that a job which was never reported on is accounted for by when it was created, since that is the only moment it has")
	void endAbandonedJobsForAJobNeverReportedOn() {
		final var worker = worker();
		final var neverReportedOn = abandonedJob("job-1").withModified(null).withCreated(LAST_WRITTEN_TO);
		when(jobServiceMock.failStaleJobs(STALE_AFTER)).thenReturn(List.of(neverReportedOn));

		assertThat(worker.endAbandonedJobs().orElseThrow())
			.contains("last written to 2026-08-25T04:12:11+02:00");
	}

	@Test
	@DisplayName("Verification that a sweep finding a great many abandoned runs names a few and counts the rest, so that a health page stays a page")
	void endAbandonedJobsWithMoreThanTheAccountNames() {
		final var worker = worker();
		when(jobServiceMock.failStaleJobs(STALE_AFTER)).thenReturn(IntStream.rangeClosed(1, 8)
			.mapToObj(number -> abandonedJob("job-" + number))
			.toList());

		assertThat(worker.endAbandonedJobs().orElseThrow())
			.startsWith("8 run(s) stopped being reported on")
			.contains("job-5")
			.doesNotContain("job-6")
			.endsWith("and 3 more.");
	}

	@Test
	@DisplayName("Verification that a sweep finding nothing says nothing, leaving the service to be reported as the healthy one it is")
	void endAbandonedJobsWithNothingAbandoned() {
		final var worker = worker();
		when(jobServiceMock.failStaleJobs(STALE_AFTER)).thenReturn(emptyList());

		assertThat(worker.endAbandonedJobs()).isEmpty();
	}

	@Test
	@DisplayName("Verification that nothing is removed from the job table, however old a job is: a purge leaves one row behind and that row is the lasting record of it")
	void endAbandonedJobsRemovesNothing() {
		final var worker = worker();
		when(jobServiceMock.failStaleJobs(STALE_AFTER)).thenReturn(emptyList());

		worker.endAbandonedJobs();

		verify(jobServiceMock).failStaleJobs(STALE_AFTER);
		verifyNoMoreInteractions(jobServiceMock);
	}

	private JobWorker worker() {
		return new JobWorker(jobServiceMock, new JobProperties(STALE_AFTER));
	}

	private static JobEntity abandonedJob(final String id) {
		return JobEntity.create()
			.withId(id)
			.withType(ERRAND_PURGE)
			.withStatus(RUNNING)
			.withNamespace("NAMESPACE-1")
			.withMunicipalityId("2281")
			.withCreated(LAST_WRITTEN_TO.minusDays(1))
			.withModified(LAST_WRITTEN_TO);
	}
}
