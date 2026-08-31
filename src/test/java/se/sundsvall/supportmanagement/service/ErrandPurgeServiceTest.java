package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import se.sundsvall.supportmanagement.config.ErrandPurgeProperties;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.purge.ErrandPurgeWorker;
import se.sundsvall.supportmanagement.service.purge.PurgeJob;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.COMPLETED;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.RUNNING;

@ExtendWith(MockitoExtension.class)
class ErrandPurgeServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String OTHER_NAMESPACE = "otherNamespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final OffsetDateTime OLDER_THAN = OffsetDateTime.parse("2020-08-28T00:00:00+02:00");
	private static final Duration JOB_RETENTION = Duration.ofHours(24);

	@Mock
	private ErrandPurgeWorker workerMock;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	/**
	 * Runs what it is handed on the calling thread, so that a run is over by the time startPurge returns.
	 */
	private static final AsyncTaskExecutor DIRECT = Runnable::run;

	/**
	 * Accepts what it is handed and never runs it, which leaves the run holding its namespace for as long as the test
	 * needs it to.
	 */
	private static final AsyncTaskExecutor NEVER_RUNS = _ -> {
		// Deliberately does nothing.
	};

	private MutableClock clock;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-08-28T10:00:00Z"));
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	@Test
	void startPurge() {
		final var service = service(NEVER_RUNS);

		final var status = service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, 1000));

		assertThat(status.getJobId()).isNotBlank();
		assertThat(status.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(status.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(status.getOlderThan()).isEqualTo(OLDER_THAN);
		assertThat(status.isDryRun()).isTrue();
		assertThat(status.getState()).isEqualTo(RUNNING);
		assertThat(status.getStarted()).isEqualTo(OffsetDateTime.now(clock));
		assertThat(status.getFinished()).isNull();
		assertThat(status.getProcessed()).isZero();
		assertThat(status.getMessage()).isNull();
	}

	@Test
	@DisplayName("Verification that the run is handed the cutoff and the settings it was started with")
	void startPurgeHandsTheRunToTheWorker() {
		final var service = service(DIRECT);
		final var handled = new ArrayList<PurgeJob>();
		doAnswer(invocation -> handled.add(invocation.getArgument(0))).when(workerMock).run(any());

		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(false, 500));

		assertThat(handled).hasSize(1);
		assertThat(handled.getFirst().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(handled.getFirst().getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(handled.getFirst().getOlderThan()).isEqualTo(OLDER_THAN);
		assertThat(handled.getFirst().isDryRun()).isFalse();
		assertThat(handled.getFirst().remainingBudget()).isEqualTo(500);
	}

	@Test
	@DisplayName("Verification that a second run on a namespace that already has one is refused rather than started alongside it")
	void startPurgeWhileOneIsAlreadyRunning() {
		final var service = service(NEVER_RUNS);
		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		assertThatThrownBy(() -> service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessageContaining("A purge is already running for namespace 'namespace' in municipality with id '2281'");
	}

	@Test
	@DisplayName("Verification that a run on another namespace is unaffected by one already under way")
	void startPurgeOnAnotherNamespaceWhileOneIsRunning() {
		final var service = service(NEVER_RUNS);
		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		final var status = service.startPurge(OTHER_NAMESPACE, MUNICIPALITY_ID, request(true, null));

		assertThat(status.getState()).isEqualTo(RUNNING);
		assertThat(status.getNamespace()).isEqualTo(OTHER_NAMESPACE);
	}

	@Test
	@DisplayName("Verification that the namespace is released once the run ends, so that a further run may be started")
	void startPurgeAfterAnEarlierRunHasEnded() {
		final var service = service(DIRECT);
		completeRunsImmediately();

		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));
		final var second = service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		assertThat(second.getState()).isEqualTo(COMPLETED);
		verify(workerMock, times(2)).run(any());
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

		verifyNoInteractions(workerMock);
	}

	@Test
	@DisplayName("Verification that a refused run does not leave the namespace claimed behind it")
	void startPurgeAfterOneWasRefused() {
		final var service = service(NEVER_RUNS);
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true, false);

		assertThatThrownBy(() -> service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)))
			.isInstanceOf(ThrowableProblem.class);
		final var second = service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		assertThat(second.getState()).isEqualTo(RUNNING);
	}

	@Test
	@DisplayName("Verification that a namespace whose access control cannot be read is left free for a later run, rather than held by a run that never started")
	void startPurgeWhenAccessControlCannotBeRead() {
		final var service = service(NEVER_RUNS);
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID))
			.thenThrow(new IllegalStateException("Database is down"))
			.thenReturn(false);

		assertThatThrownBy(() -> service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR)
			.hasMessageContaining("Purge could not be started: Database is down");

		assertThat(service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)).getState()).isEqualTo(RUNNING);
	}

	@Test
	@DisplayName("Verification that a run which cannot be given a thread is refused instead of reported as under way")
	void startPurgeWhenNoThreadCanBeGiven() {
		final var service = service(_ -> {
			throw new TaskRejectedException("No thread available");
		});

		assertThatThrownBy(() -> service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR)
			.hasMessageContaining("Purge could not be started: No thread available");

		verifyNoInteractions(workerMock);
	}

	@Test
	@DisplayName("Verification that a rejected run leaves nothing behind, so that the namespace can be purged once threads are free again")
	void startPurgeAfterOneWasRejected() {
		final var rejecting = new AtomicBoolean(true);
		final var service = service(runnable -> {
			if (rejecting.get()) {
				throw new TaskRejectedException("No thread available");
			}
		});

		assertThatThrownBy(() -> service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)))
			.isInstanceOf(ThrowableProblem.class);
		rejecting.set(false);

		assertThat(service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)).getState()).isEqualTo(RUNNING);
	}

	@Test
	@DisplayName("Verification that the caller asking for a run is recorded on it, since the thread carrying it out has no identifier to read")
	void startPurgeRecordsTheCaller() {
		final var service = service(NEVER_RUNS);
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe"));

		assertThat(service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)).getStartedBy()).isEqualTo("joe01doe");
	}

	@Test
	@DisplayName("Verification that a run started without an identifier says so, rather than naming nobody at all")
	void startPurgeWithoutAnIdentifier() {
		final var service = service(NEVER_RUNS);

		assertThat(service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null)).getStartedBy()).isEqualTo("unknown");
	}

	@Test
	void readPurgeStatus() {
		final var service = service(NEVER_RUNS);
		final var started = service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		final var status = service.readPurgeStatus(NAMESPACE, MUNICIPALITY_ID, started.getJobId());

		assertThat(status.getJobId()).isEqualTo(started.getJobId());
		assertThat(status.getState()).isEqualTo(RUNNING);
	}

	@Test
	void readPurgeStatusForUnknownJob() {
		final var service = service(NEVER_RUNS);
		final var jobId = randomUUID().toString();

		assertThatThrownBy(() -> service.readPurgeStatus(NAMESPACE, MUNICIPALITY_ID, jobId))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessageContaining("A purge job with id '%s' could not be found in namespace 'namespace' for municipality with id '2281'".formatted(jobId));
	}

	@Test
	@DisplayName("Verification that a run belonging to another namespace answers as though it did not exist, rather than confirming the id")
	void readPurgeStatusFromAnotherNamespace() {
		final var service = service(NEVER_RUNS);
		final var started = service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		assertThatThrownBy(() -> service.readPurgeStatus(OTHER_NAMESPACE, MUNICIPALITY_ID, started.getJobId()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	@DisplayName("Verification that a run asked to stop keeps its state until it notices, since it is still running until it does")
	void stopPurge() {
		final var service = service(NEVER_RUNS);
		final var started = service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		final var status = service.stopPurge(NAMESPACE, MUNICIPALITY_ID, started.getJobId());

		assertThat(status.getJobId()).isEqualTo(started.getJobId());
		assertThat(status.getState()).isEqualTo(RUNNING);
		assertThat(status.getFinished()).isNull();
	}

	@Test
	void stopPurgeForUnknownJob() {
		final var service = service(NEVER_RUNS);
		final var jobId = randomUUID().toString();

		assertThatThrownBy(() -> service.stopPurge(NAMESPACE, MUNICIPALITY_ID, jobId))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	@DisplayName("Verification that the stop reaches the run itself, and not merely the status the caller is answered with")
	void stopPurgeReachesTheRunItself() {
		final var pending = new ArrayList<Runnable>();
		final var service = service(pending::add);
		final var stopSeenByRun = new AtomicBoolean();
		doAnswer(invocation -> {
			stopSeenByRun.set(((PurgeJob) invocation.getArgument(0)).isStopRequested());
			return null;
		}).when(workerMock).run(any());

		final var started = service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));
		service.stopPurge(NAMESPACE, MUNICIPALITY_ID, started.getJobId());
		pending.forEach(Runnable::run);

		assertThat(stopSeenByRun).isTrue();
	}

	@Test
	@DisplayName("Verification that a finished run stops being readable once it has been so for longer than the retention window")
	void finishedJobsAreEvictedOnceTheyAgeOut() {
		final var service = service(DIRECT);
		completeRunsImmediately();

		final var first = service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));
		clock.advance(JOB_RETENTION.plusMinutes(1));
		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		assertThatThrownBy(() -> service.readPurgeStatus(NAMESPACE, MUNICIPALITY_ID, first.getJobId()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	@DisplayName("Verification that a run finished within the retention window is still readable")
	void recentlyFinishedJobsSurvive() {
		final var service = service(DIRECT);
		completeRunsImmediately();

		final var first = service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));
		clock.advance(JOB_RETENTION.minusMinutes(1));
		service.startPurge(NAMESPACE, MUNICIPALITY_ID, request(true, null));

		assertThat(service.readPurgeStatus(NAMESPACE, MUNICIPALITY_ID, first.getJobId()).getState()).isEqualTo(COMPLETED);
	}

	private void completeRunsImmediately() {
		doAnswer(invocation -> {
			final PurgeJob job = invocation.getArgument(0);
			job.finish(COMPLETED, null, OffsetDateTime.now(clock));
			return null;
		}).when(workerMock).run(any());
	}

	private ErrandPurgeService service(final AsyncTaskExecutor taskExecutor) {
		return new ErrandPurgeService(workerMock, namespaceConfigServiceMock, taskExecutor, clock,
			new ErrandPurgeProperties(Period.ofYears(2), 250, JOB_RETENTION, 2));
	}

	private static ErrandPurgeRequest request(final boolean dryRun, final Integer maxErrands) {
		return ErrandPurgeRequest.create()
			.withOlderThan(OLDER_THAN)
			.withDryRun(dryRun)
			.withMaxErrands(maxErrands);
	}

	/**
	 * A clock the test moves forward by hand, so that ageing a finished run out of the registry does not mean waiting a
	 * day for it.
	 */
	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(final Instant instant) {
			this.instant = instant;
		}

		private void advance(final Duration amount) {
			this.instant = this.instant.plus(amount);
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("UTC");
		}

		@Override
		public Clock withZone(final ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}

}
