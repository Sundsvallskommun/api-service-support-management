package se.sundsvall.supportmanagement.service.job;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.config.ErrandPurgeProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.model.enums.JobStatus.STOPPED;

/**
 * The walk of the purge worker: how far it goes, what it counts and what it tells the job it reports against. Which
 * errands the batches hold is settled by a specification handed to the database, and is not asserted here.
 */
@ExtendWith(MockitoExtension.class)
class ErrandPurgeRunnerTest {

	private static final String JOB_ID = randomUUID().toString();
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final OffsetDateTime OLDER_THAN = OffsetDateTime.parse("2020-08-28T00:00:00+02:00");
	private static final String STARTED_BY = "joe01doe";
	private static final int BATCH_SIZE = 2;

	/**
	 * Long enough that no batch in these tests comes due for a report of its own, leaving only the reporting a batch
	 * does when it ends.
	 */
	private static final Duration PROGRESS_INTERVAL = Duration.ofMinutes(1);

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	private ErrandPurgeRunner runner;

	private ErrandPurgeRunner runner() {
		return runner(PROGRESS_INTERVAL);
	}

	private ErrandPurgeRunner runner(final Duration progressInterval) {
		if (runner == null) {
			runner = new ErrandPurgeRunner(errandsRepositoryMock, errandServiceMock, jobServiceMock, namespaceConfigServiceMock,
				new ErrandPurgeProperties(Period.ofYears(2), BATCH_SIZE, 2, progressInterval));
		}
		return runner;
	}

	@Test
	@DisplayName("Verification that the number a job reports progress against is the number of errands the run would reach")
	void countErrandsToPurge() {
		when(errandsRepositoryMock.count(anySpecification())).thenReturn(4711L);

		assertThat(runner().countErrandsToPurge(NAMESPACE, MUNICIPALITY_ID, OLDER_THAN)).isEqualTo(4711);
	}

	@Test
	@DisplayName("Verification that a namespace with nothing old enough completes without touching an errand")
	void runWithNothingToPurge() {
		batches(emptyList());

		runner().run(run(false, null));

		verify(jobServiceMock).setRunning(JOB_ID);
		verify(jobServiceMock).complete(JOB_ID, "Removed 0 of 0 errands reached, 0 could not be removed");
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	@DisplayName("Verification that the walk carries on past an errand it could not remove, and reports what it managed")
	void runWalksEveryBatch() {
		batches(ids("a", "b"), ids("c"), emptyList());
		stillRunning();
		when(errandServiceMock.purgeErrand(NAMESPACE, MUNICIPALITY_ID, "a")).thenReturn(true);
		when(errandServiceMock.purgeErrand(NAMESPACE, MUNICIPALITY_ID, "b")).thenThrow(new RuntimeException("Constraint violation"));
		when(errandServiceMock.purgeErrand(NAMESPACE, MUNICIPALITY_ID, "c")).thenReturn(true);

		runner().run(run(false, null));

		verify(jobServiceMock).updateProgress(JOB_ID, 2);
		verify(jobServiceMock).updateProgress(JOB_ID, 3);
		verify(jobServiceMock).complete(JOB_ID, "Removed 2 of 3 errands reached, 1 could not be removed");
	}

	@Test
	@DisplayName("Verification that a batch outlasting the interval a run reports on is reported from inside, so that a slow run is not taken for one whose instance is gone")
	void runReportsProgressFromInsideALongBatch() {
		batches(ids("a", "b"), emptyList());
		stillRunning();
		when(errandServiceMock.purgeErrand(any(), any(), any())).thenReturn(true);

		// Nothing may elapse before a report is due, so every errand comes due and the first is reported on well before
		// the batch holding it has ended.
		runner(Duration.ZERO).run(run(false, null));

		verify(jobServiceMock).updateProgress(JOB_ID, 1);
		// Once as the last errand of the batch comes due and once as the batch ends. The two coincide only because the
		// interval is zero here, and the second write says what the first one did.
		verify(jobServiceMock, times(2)).updateProgress(JOB_ID, 2);
		verify(jobServiceMock).complete(JOB_ID, "Removed 2 of 2 errands reached, 0 could not be removed");
	}

	@Test
	@DisplayName("Verification that a dry run counts every errand it reaches and removes none of them")
	void runAsDryRun() {
		batches(ids("a", "b"), emptyList());
		stillRunning();

		runner().run(run(true, null));

		verify(jobServiceMock).complete(JOB_ID, "Dry run over 2 errands, none of which were removed");
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	@DisplayName("Verification that an errand gone before the run reached it counts as reached but not as removed by this run")
	void runWhenErrandIsAlreadyGone() {
		batches(ids("a"), emptyList());
		stillRunning();
		when(errandServiceMock.purgeErrand(NAMESPACE, MUNICIPALITY_ID, "a")).thenReturn(false);

		runner().run(run(false, null));

		verify(jobServiceMock).complete(JOB_ID, "Removed 0 of 1 errands reached, 0 could not be removed");
	}

	@Test
	@DisplayName("Verification that a run stops once it reaches the limit it was started with, without reading a further batch")
	void runStopsWhenTheLimitIsReached() {
		batches(ids("a", "b"));
		stillRunning();
		when(errandServiceMock.purgeErrand(any(), any(), any())).thenReturn(true);

		runner().run(run(false, 2));

		verify(errandsRepositoryMock).findIdsAfter(any(), any(), anyInt());
		verify(jobServiceMock).complete(JOB_ID, "Removed 2 of 2 errands reached, 0 could not be removed");
	}

	@Test
	@DisplayName("Verification that a run asked to stop finishes the batch it is on and stops there, leaving the job as it was stopped")
	void runStopsWhenTheJobSaysSo() {
		batches(ids("a", "b"), ids("c", "d"));
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(STOPPED));
		when(errandServiceMock.purgeErrand(any(), any(), any())).thenReturn(true);

		runner().run(run(false, null));

		verify(errandsRepositoryMock).findIdsAfter(any(), any(), anyInt());
		verify(jobServiceMock).updateProgress(JOB_ID, 2);
		verify(jobServiceMock, never()).complete(any(), any());
		verify(jobServiceMock, never()).fail(any(), any());
	}

	@Test
	@DisplayName("Verification that a database that cannot be read ends the job as failed with the reason, rather than throwing on a thread with nobody to catch it")
	void runWhenReadingErrandsFails() {
		doThrow(new RuntimeException("Database is unreachable")).when(errandsRepositoryMock).findIdsAfter(any(), any(), anyInt());

		runner().run(run(false, null));

		verify(jobServiceMock).fail(JOB_ID, "Purge aborted: Database is unreachable");
	}

	@Test
	@DisplayName("Verification that a run leaving the job as running is ended, so that nothing is left reading as under way for as long as the row lives")
	void runThatLeavesTheJobRunning() {
		final var purgeRunner = runner();
		final var purgeRun = run(false, null);
		doThrow(new StackOverflowError()).when(errandsRepositoryMock).findIdsAfter(any(), any(), anyInt());

		// Only the run itself is left inside the lambda, so the assertion cannot be met by anything else throwing.
		assertThatThrownBy(() -> purgeRunner.run(purgeRun)).isInstanceOf(StackOverflowError.class);

		verify(jobServiceMock).fail(JOB_ID, "Purge ended without reaching a result of its own");
	}

	@Test
	@DisplayName("Verification that a namespace put under access control while a run is under way stops the run, rather than having it keep removing errands past a guard raised after it started")
	void runWhenAccessControlIsSwitchedOnMidRun() {
		batches(ids("a", "b"), ids("c", "d"));
		stillRunning();
		when(errandServiceMock.purgeErrand(any(), any(), any())).thenReturn(true);
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		runner().run(run(false, null));

		verify(errandsRepositoryMock).findIdsAfter(any(), any(), anyInt());
		verify(jobServiceMock).fail(JOB_ID, "Purge ended after removing 2 errands: access control was switched on for the namespace while it was running");
		verify(jobServiceMock, never()).complete(any(), any());
	}

	/**
	 * The job as a run under way sees it, so the walk carries on to its own end.
	 */
	private void stillRunning() {
		when(jobServiceMock.statusOf(JOB_ID)).thenReturn(Optional.of(RUNNING));
	}

	@SafeVarargs
	private void batches(final List<String>... batches) {
		var stubbing = doReturn(batches[0]);
		for (var index = 1; index < batches.length; index++) {
			stubbing = stubbing.doReturn(batches[index]);
		}
		stubbing.when(errandsRepositoryMock).findIdsAfter(any(), any(), anyInt());
	}

	private static List<String> ids(final String... ids) {
		return List.of(ids);
	}

	private static PurgeRun run(final boolean dryRun, final Integer maxErrands) {
		return new PurgeRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID, STARTED_BY, new PurgeSettings(OLDER_THAN, dryRun, maxErrands));
	}

	/**
	 * Matches any specification, typed so that it picks one of the several counts of the repository.
	 */
	private static Specification<ErrandEntity> anySpecification() {
		return ArgumentMatchers.any();
	}
}
