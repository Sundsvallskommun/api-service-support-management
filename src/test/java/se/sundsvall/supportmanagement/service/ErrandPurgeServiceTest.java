package se.sundsvall.supportmanagement.service;

import java.time.OffsetDateTime;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeRequest;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.job.ErrandPurgeRunner;
import se.sundsvall.supportmanagement.service.job.JobService;
import se.sundsvall.supportmanagement.service.job.JobSpec;
import se.sundsvall.supportmanagement.service.job.PurgeRun;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.ERRAND_PURGE;

@ExtendWith(MockitoExtension.class)
class ErrandPurgeServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String JOB_ID = randomUUID().toString();
	private static final OffsetDateTime OLDER_THAN = OffsetDateTime.parse("2020-08-28T00:00:00+02:00");
	private static final int TOTAL = 1000;
	private static final String COULD_NOT_START = "Purge could not be started: %s";

	/**
	 * Accepts what it is handed and never runs it, which leaves the run pending for as long as the test needs it to.
	 */
	private static final AsyncTaskExecutor NEVER_RUNS = _ -> {
		// Deliberately does nothing.
	};

	@Mock
	private ErrandPurgeRunner runnerMock;

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
		verify(runnerMock).countErrandsToPurge(NAMESPACE, MUNICIPALITY_ID, OLDER_THAN);
		verify(jobServiceMock).launch(eq(new JobSpec(NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE, TOTAL, null)), any(), any(), any(), eq(COULD_NOT_START));
	}

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName("Verification that the run built for the launch carries the job it reports against and the settings it was started with, and that the runner argument reaches the actual ErrandPurgeRunner")
	void startPurgeHandsTheRunToTheRunner() {
		final var service = service(NEVER_RUNS);
		acceptsRuns();
		final var identifier = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe");
		Identifier.set(identifier);

		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(false, 500));

		final var toRunCaptor = ArgumentCaptor.forClass(Function.class);
		final var runnerCaptor = ArgumentCaptor.forClass(Consumer.class);
		verify(jobServiceMock).launch(eq(new JobSpec(NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE, TOTAL, null)), any(), toRunCaptor.capture(), runnerCaptor.capture(), eq(COULD_NOT_START));

		final var run = (PurgeRun) toRunCaptor.getValue().apply(JOB_ID);
		assertThat(run.jobId()).isEqualTo(JOB_ID);
		assertThat(run.namespace()).isEqualTo(NAMESPACE);
		assertThat(run.municipalityId()).isEqualTo(MUNICIPALITY_ID);
		// The whole identifier (type and value), not just the value - see startedBy()'s own doc comment for why.
		assertThat(run.startedBy()).isEqualTo(identifier.toHeaderValue());
		assertThat(run.settings().olderThan()).isEqualTo(OLDER_THAN);
		assertThat(run.settings().dryRun()).isFalse();
		assertThat(run.settings().maxErrands()).isEqualTo(500);

		// The captured runner argument is runnerMock::run bound to the very mock under test - invoking it here is what
		// proves that binding, since jobServiceMock.launch is stubbed and never calls it on its own.
		((Consumer<PurgeRun>) runnerCaptor.getValue()).accept(run);
		verify(runnerMock).run(run);
	}

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName("Verification that the caller is read on the request thread, which is the only thread carrying one")
	void startPurgeReadsTheCallerBeforeHandingTheRunOver() {
		final var service = service(NEVER_RUNS);
		acceptsRuns();
		final var identifier = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe");
		Identifier.set(identifier);

		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(false, null));

		final var toRunCaptor = ArgumentCaptor.forClass(Function.class);
		verify(jobServiceMock).launch(eq(new JobSpec(NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE, TOTAL, null)), any(), toRunCaptor.capture(), any(), eq(COULD_NOT_START));

		// The request is over and its thread carries no identifier any more. What the run is recorded as was already
		// settled when the request built this function - applying it here, after the identifier is gone, is what proves
		// it was captured rather than left to be read again from the (now empty) thread local.
		Identifier.remove();
		final var run = (PurgeRun) toRunCaptor.getValue().apply(JOB_ID);

		assertThat(run.startedBy()).isEqualTo(identifier.toHeaderValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName("Verification that a run started without an identifier is recorded as such rather than as nobody")
	void startPurgeWithoutAnIdentifier() {
		final var service = service(NEVER_RUNS);
		acceptsRuns();

		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		final var toRunCaptor = ArgumentCaptor.forClass(Function.class);
		verify(jobServiceMock).launch(eq(new JobSpec(NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE, TOTAL, null)), any(), toRunCaptor.capture(), any(), eq(COULD_NOT_START));

		final var run = (PurgeRun) toRunCaptor.getValue().apply(JOB_ID);
		assertThat(run.startedBy()).isEqualTo("unknown");
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

		verify(jobServiceMock, never()).launch(any(), any(), any(), any(), any());
		verifyNoInteractions(runnerMock);
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

		verify(jobServiceMock, never()).launch(any(), any(), any(), any(), any());
		verifyNoInteractions(runnerMock);
	}

	@Test
	@DisplayName("Verification that a stop reaches the job, which is what lets a run be stopped from another instance than the one carrying it out")
	void stopPurge() {
		final var service = service(NEVER_RUNS);
		final var stopped = JobResponse.create().withJobId(JOB_ID);
		when(jobServiceMock.stop(NAMESPACE, MUNICIPALITY_ID, JOB_ID, ERRAND_PURGE)).thenReturn(stopped);

		assertThat(service.stopPurge(NAMESPACE, MUNICIPALITY_ID, JOB_ID)).isSameAs(stopped);

		verify(jobServiceMock).stop(NAMESPACE, MUNICIPALITY_ID, JOB_ID, ERRAND_PURGE);
	}

	/**
	 * What the job side answers for a run that gets as far as being accepted.
	 */
	private void acceptsRuns() {
		when(runnerMock.countErrandsToPurge(NAMESPACE, MUNICIPALITY_ID, OLDER_THAN)).thenReturn(TOTAL);
		when(jobServiceMock.launch(eq(new JobSpec(NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE, TOTAL, null)), any(), any(), any(), eq(COULD_NOT_START)))
			.thenReturn(JobResponse.create()
				.withJobId(JOB_ID)
				.withStatus(RUNNING));
	}

	private ErrandPurgeService service(final AsyncTaskExecutor taskExecutor) {
		return new ErrandPurgeService(runnerMock, jobServiceMock, namespaceConfigServiceMock, taskExecutor);
	}

	private static ErrandPurgeRequest request(final boolean dryRun, final Integer maxErrands) {
		return ErrandPurgeRequest.create()
			.withOlderThan(OLDER_THAN)
			.withDryRun(dryRun)
			.withMaxErrands(maxErrands);
	}
}
