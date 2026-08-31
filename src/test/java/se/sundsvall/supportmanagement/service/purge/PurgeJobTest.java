package se.sundsvall.supportmanagement.service.purge;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.COMPLETED;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.FAILED;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.RUNNING;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.STOPPED;

class PurgeJobTest {

	private static final String JOB_ID = randomUUID().toString();
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final OffsetDateTime OLDER_THAN = OffsetDateTime.parse("2020-08-28T00:00:00+02:00");
	private static final String STARTED_BY = "joe01doe";
	private static final OffsetDateTime STARTED = OffsetDateTime.parse("2026-08-28T10:00:00+02:00");
	private static final OffsetDateTime ENDED = OffsetDateTime.parse("2026-08-28T12:00:00+02:00");

	@Test
	void newJobIsRunningAndEmpty() {
		final var status = job(null).toStatus();

		assertThat(status.getJobId()).isEqualTo(JOB_ID);
		assertThat(status.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(status.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(status.getOlderThan()).isEqualTo(OLDER_THAN);
		assertThat(status.isDryRun()).isFalse();
		assertThat(status.getState()).isEqualTo(RUNNING);
		assertThat(status.getStarted()).isEqualTo(STARTED);
		assertThat(status.getFinished()).isNull();
		assertThat(status.getProcessed()).isZero();
		assertThat(status.getDeleted()).isZero();
		assertThat(status.getFailed()).isZero();
		assertThat(status.getMessage()).isNull();
	}

	@Test
	@DisplayName("Verification that an errand reached but not removed raises the processed count alone")
	void recordCountedCountsAsReachedOnly() {
		final var job = job(null);

		job.recordCounted();
		job.recordCounted();

		assertThat(job.toStatus().getProcessed()).isEqualTo(2);
		assertThat(job.toStatus().getDeleted()).isZero();
		assertThat(job.toStatus().getFailed()).isZero();
	}

	@Test
	void countersAddUp() {
		final var job = job(null);

		job.recordDeleted();
		job.recordDeleted();
		job.recordFailed();
		job.recordCounted();

		final var status = job.toStatus();
		assertThat(status.getProcessed()).isEqualTo(4);
		assertThat(status.getDeleted()).isEqualTo(2);
		assertThat(status.getFailed()).isEqualTo(1);
	}

	@Test
	@DisplayName("Verification that a job asked to stop keeps running until the run itself notices")
	void requestStopDoesNotChangeState() {
		final var job = job(null);

		job.requestStop();

		assertThat(job.isStopRequested()).isTrue();
		assertThat(job.getState()).isEqualTo(RUNNING);
		assertThat(job.getFinished()).isNull();
	}

	@Test
	void finishSetsStateMessageAndTimestamp() {
		final var job = job(null);

		job.finish(FAILED, "Purge aborted: database is unreachable", ENDED);

		final var status = job.toStatus();
		assertThat(status.getState()).isEqualTo(FAILED);
		assertThat(status.getMessage()).isEqualTo("Purge aborted: database is unreachable");
		assertThat(status.getFinished()).isEqualTo(ENDED);
	}

	@Test
	@DisplayName("Verification that a job that has already ended is not overwritten by a later attempt to end it")
	void finishOnlyTakesEffectOnce() {
		final var job = job(null);

		job.finish(COMPLETED, null, ENDED);
		job.finish(FAILED, "too late", ENDED.plusHours(1));

		final var status = job.toStatus();
		assertThat(status.getState()).isEqualTo(COMPLETED);
		assertThat(status.getMessage()).isNull();
		assertThat(status.getFinished()).isEqualTo(ENDED);
	}

	@Test
	@DisplayName("Verification that a run without a limit never runs out of budget")
	void unlimitedRunHasBudgetLeft() {
		final var job = job(null);

		job.recordDeleted();

		assertThat(job.remainingBudget()).isPositive();
	}

	@Test
	void budgetShrinksWithEveryErrandReached() {
		final var job = job(2);

		assertThat(job.remainingBudget()).isEqualTo(2);

		job.recordDeleted();
		assertThat(job.remainingBudget()).isEqualTo(1);

		job.recordFailed();
		assertThat(job.remainingBudget()).isZero();
	}

	@Test
	void stoppedJobReportsItsCounters() {
		final var job = job(10);

		job.recordDeleted();
		job.finish(STOPPED, null, ENDED);

		final var status = job.toStatus();
		assertThat(status.getState()).isEqualTo(STOPPED);
		assertThat(status.getProcessed()).isEqualTo(1);
		assertThat(status.getDeleted()).isEqualTo(1);
		assertThat(status.getMessage()).isNull();
	}

	private static PurgeJob job(final Integer maxErrands) {
		return new PurgeJob(JOB_ID, NAMESPACE, MUNICIPALITY_ID, OLDER_THAN, false, maxErrands, STARTED_BY, STARTED);
	}
}
