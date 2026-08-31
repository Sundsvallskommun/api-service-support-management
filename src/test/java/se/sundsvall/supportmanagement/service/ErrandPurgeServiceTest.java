package se.sundsvall.supportmanagement.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeRequest;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.purge.ErrandPurgeWorker;
import se.sundsvall.supportmanagement.service.purge.PurgeRun;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.ERRAND_PURGE;

@ExtendWith(MockitoExtension.class)
class ErrandPurgeServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String JOB_ID = randomUUID().toString();
	private static final OffsetDateTime OLDER_THAN = OffsetDateTime.parse("2020-08-28T00:00:00+02:00");
	private static final int TOTAL = 1000;

	/**
	 * Accepts what it is handed and never runs it, which leaves the run pending for as long as the test needs it to.
	 */
	private static final AsyncTaskExecutor NEVER_RUNS = _ -> {
		// Deliberately does nothing.
	};

	@Mock
	private ErrandPurgeWorker workerMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	@Test
	void startPurge() {
		final var service = service(NEVER_RUNS);
		acceptsRuns();

		final var response = service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, 500));

		assertThat(response.getJobId()).isEqualTo(JOB_ID);
		assertThat(response.getStatus()).isEqualTo(RUNNING);
		verify(workerMock).countErrandsToPurge(NAMESPACE, MUNICIPALITY_ID, OLDER_THAN);
		verify(jobServiceMock).create(NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE, TOTAL);
		verify(jobServiceMock).get(NAMESPACE, MUNICIPALITY_ID, JOB_ID);
	}

	@Test
	@DisplayName("Verification that the run is handed the job it reports against and the settings it was started with")
	void startPurgeHandsTheRunToTheWorker() {
		final var handled = new ArrayList<PurgeRun>();
		final var service = service(Runnable::run);
		acceptsRuns();
		doAnswer(invocation -> handled.add(invocation.getArgument(0))).when(workerMock).run(any(PurgeRun.class));
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe"));

		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(false, 500));

		assertThat(handled).hasSize(1);
		assertThat(handled.getFirst().jobId()).isEqualTo(JOB_ID);
		assertThat(handled.getFirst().namespace()).isEqualTo(NAMESPACE);
		assertThat(handled.getFirst().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(handled.getFirst().startedBy()).isEqualTo("joe01doe");
		assertThat(handled.getFirst().settings().olderThan()).isEqualTo(OLDER_THAN);
		assertThat(handled.getFirst().settings().dryRun()).isFalse();
		assertThat(handled.getFirst().settings().maxErrands()).isEqualTo(500);
	}

	@Test
	@DisplayName("Verification that a run started without an identifier is recorded as such rather than as nobody")
	void startPurgeWithoutAnIdentifier() {
		final var handled = new ArrayList<PurgeRun>();
		final var service = service(Runnable::run);
		acceptsRuns();
		doAnswer(invocation -> handled.add(invocation.getArgument(0))).when(workerMock).run(any(PurgeRun.class));

		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		assertThat(handled.getFirst().startedBy()).isEqualTo("unknown");
	}

	@Test
	@DisplayName("Verification that a namespace already being purged is refused rather than walked by two runs at once")
	void startPurgeWhileOneIsAlreadyRunning() {
		final var service = service(NEVER_RUNS);
		when(jobServiceMock.hasActiveJob(NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE)).thenReturn(true);

		assertThatThrownBy(() -> service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessageContaining("A purge is already running for namespace 'namespace' in municipality with id '2281'");

		verify(jobServiceMock, never()).create(any(), any(), any(), anyInt());
		verifyNoInteractions(workerMock);
	}

	@Test
	@DisplayName("Verification that a namespace with access control is refused outright rather than being purged past its own guard")
	void startPurgeWhenAccessControlIsActive() {
		final var service = service(NEVER_RUNS);
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessageContaining("Errands in namespace 'namespace' for municipality with id '2281' are under access control and cannot be purged");

		verify(jobServiceMock, never()).create(any(), any(), any(), anyInt());
		verifyNoInteractions(workerMock);
	}

	@Test
	@DisplayName("Verification that a run which cannot be given a thread ends the job it was given, rather than leaving it waiting for work that never comes")
	void startPurgeWhenNoThreadCanBeGiven() {
		final var service = service(_ -> {
			throw new TaskRejectedException("No thread available");
		});
		when(workerMock.countErrandsToPurge(NAMESPACE, MUNICIPALITY_ID, OLDER_THAN)).thenReturn(TOTAL);
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE, TOTAL)).thenReturn(JOB_ID);

		assertThatThrownBy(() -> service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR)
			.hasMessageContaining("Purge could not be started: No thread available");

		verify(jobServiceMock).fail(JOB_ID, "Purge could not be started: No thread available");
	}

	@Test
	@DisplayName("Verification that a stop reaches the job, which is what lets a run be stopped from another instance than the one carrying it out")
	void stopPurge() {
		final var service = service(NEVER_RUNS);
		final var stopped = JobResponse.create().withJobId(JOB_ID);
		when(jobServiceMock.stop(NAMESPACE, MUNICIPALITY_ID, JOB_ID)).thenReturn(stopped);

		assertThat(service.stopPurge(NAMESPACE, MUNICIPALITY_ID, JOB_ID)).isSameAs(stopped);

		verify(jobServiceMock).stop(NAMESPACE, MUNICIPALITY_ID, JOB_ID);
	}

	/**
	 * What the job side answers for a run that gets as far as being accepted.
	 */
	private void acceptsRuns() {
		when(workerMock.countErrandsToPurge(NAMESPACE, MUNICIPALITY_ID, OLDER_THAN)).thenReturn(TOTAL);
		when(jobServiceMock.create(NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE, TOTAL)).thenReturn(JOB_ID);
		when(jobServiceMock.get(NAMESPACE, MUNICIPALITY_ID, JOB_ID)).thenReturn(JobResponse.create()
			.withJobId(JOB_ID)
			.withStatus(RUNNING));
	}

	private ErrandPurgeService service(final AsyncTaskExecutor taskExecutor) {
		return new ErrandPurgeService(workerMock, jobServiceMock, namespaceConfigServiceMock, taskExecutor);
	}

	private static ErrandPurgeRequest request(final boolean dryRun, final Integer maxErrands) {
		return ErrandPurgeRequest.create()
			.withOlderThan(OLDER_THAN)
			.withDryRun(dryRun)
			.withMaxErrands(maxErrands);
	}
}
