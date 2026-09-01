package se.sundsvall.supportmanagement.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.JobRepository;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.PENDING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.ERRAND_PURGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.MOVE_LABEL;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String JOB_ID = "job-id";

	@Mock
	private JobRepository jobRepositoryMock;

	@InjectMocks
	private JobService jobService;

	@Test
	void create() {
		final var entity = JobEntity.create().withId(JOB_ID);
		when(jobRepositoryMock.save(any())).thenReturn(entity);

		final var result = jobService.create(NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL, 100);

		assertThat(result).isEqualTo(JOB_ID);
		final var captor = ArgumentCaptor.forClass(JobEntity.class);
		verify(jobRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(captor.getValue().getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(captor.getValue().getType()).isEqualTo(MOVE_LABEL);
		assertThat(captor.getValue().getTotal()).isEqualTo(100);
	}

	@Test
	void get() {
		final var entity = jobEntity(RUNNING);
		when(jobRepositoryMock.findByIdAndNamespaceAndMunicipalityId(JOB_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));

		final var response = jobService.get(NAMESPACE, MUNICIPALITY_ID, JOB_ID);

		assertThat(response.getJobId()).isEqualTo(JOB_ID);
		assertThat(response.getStatus()).isEqualTo(RUNNING);
		assertThat(response.getType()).isEqualTo(MOVE_LABEL);
		assertThat(response.getProgress()).isEqualTo(50);
		assertThat(response.getTotal()).isEqualTo(100);
		assertThat(response.getProcessed()).isEqualTo(50);
	}

	@Test
	void getThrowsWhenNotFound() {
		when(jobRepositoryMock.findByIdAndNamespaceAndMunicipalityId(JOB_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> jobService.get(NAMESPACE, MUNICIPALITY_ID, JOB_ID))
			.hasMessageContaining(JOB_ID)
			.hasMessageContaining(NAMESPACE)
			.hasMessageContaining(MUNICIPALITY_ID);
	}

	@Test
	void setRunning() {
		final var entity = jobEntity(PENDING);
		when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(entity));

		jobService.setRunning(JOB_ID);

		final var captor = ArgumentCaptor.forClass(JobEntity.class);
		verify(jobRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(RUNNING);
	}

	@Test
	void updateProgress() {
		final var entity = jobEntity(RUNNING);
		when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(entity));

		jobService.updateProgress(JOB_ID, 75);

		final var captor = ArgumentCaptor.forClass(JobEntity.class);
		verify(jobRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getProcessed()).isEqualTo(75);
		assertThat(captor.getValue().getProgress()).isEqualTo(75);
	}

	@Test
	void complete() {
		final var entity = jobEntity(RUNNING);
		when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(entity));

		jobService.complete(JOB_ID);

		final var captor = ArgumentCaptor.forClass(JobEntity.class);
		verify(jobRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(COMPLETED);
		assertThat(captor.getValue().getProgress()).isEqualTo(100);
	}

	@Test
	void fail() {
		final var entity = jobEntity(RUNNING);
		when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(entity));

		jobService.fail(JOB_ID, "something went wrong");

		final var captor = ArgumentCaptor.forClass(JobEntity.class);
		verify(jobRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(FAILED);
		assertThat(captor.getValue().getMessage()).isEqualTo("something went wrong");
	}

	@Test
	void hasActiveJobReturnsTrueWhenPendingJobExists() {
		when(jobRepositoryMock.existsByNamespaceAndMunicipalityIdAndStatusIn(eq(NAMESPACE), eq(MUNICIPALITY_ID), any())).thenReturn(true);

		assertThat(jobService.hasActiveJob(NAMESPACE, MUNICIPALITY_ID)).isTrue();
	}

	@Test
	void hasActiveJobReturnsFalseWhenNoActiveJob() {
		when(jobRepositoryMock.existsByNamespaceAndMunicipalityIdAndStatusIn(eq(NAMESPACE), eq(MUNICIPALITY_ID), any())).thenReturn(false);

		assertThat(jobService.hasActiveJob(NAMESPACE, MUNICIPALITY_ID)).isFalse();
	}

	@Test
	@DisplayName("Verification that a job of one kind being under way says nothing about another kind, so that two unrelated jobs do not rule each other out")
	void hasActiveJobOfType() {
		when(jobRepositoryMock.existsByNamespaceAndMunicipalityIdAndTypeAndStatusIn(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_PURGE), any())).thenReturn(true);

		assertThat(jobService.hasActiveJob(NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE)).isTrue();
	}

	@Test
	@DisplayName("Verification that completing a job with a summary keeps the summary with it")
	void completeWithMessage() {
		final var entity = jobEntity(RUNNING);
		when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(entity));

		jobService.complete(JOB_ID, "Removed 248 of 250 errands reached, 2 could not be removed");

		assertThat(entity.getStatus()).isEqualTo(COMPLETED);
		assertThat(entity.getProgress()).isEqualTo(100);
		assertThat(entity.getMessage()).isEqualTo("Removed 248 of 250 errands reached, 2 could not be removed");
		verify(jobRepositoryMock).save(entity);
	}

	@Test
	@DisplayName("Verification that a job under way is marked as stopped, which is what the work itself reads to know it is no longer wanted")
	void stop() {
		final var entity = jobEntity(RUNNING);
		when(jobRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndType(JOB_ID, NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL)).thenReturn(Optional.of(entity));

		final var response = jobService.stop(NAMESPACE, MUNICIPALITY_ID, JOB_ID, MOVE_LABEL);

		assertThat(response.getStatus()).isEqualTo(STOPPED);
		assertThat(entity.getStatus()).isEqualTo(STOPPED);
		verify(jobRepositoryMock).save(entity);
	}

	@Test
	@DisplayName("Verification that a job which has already ended keeps the outcome it reached, rather than having it rewritten by a late stop")
	void stopJobThatHasAlreadyEnded() {
		final var entity = jobEntity(COMPLETED);
		when(jobRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndType(JOB_ID, NAMESPACE, MUNICIPALITY_ID, MOVE_LABEL)).thenReturn(Optional.of(entity));

		final var response = jobService.stop(NAMESPACE, MUNICIPALITY_ID, JOB_ID, MOVE_LABEL);

		assertThat(response.getStatus()).isEqualTo(COMPLETED);
		verify(jobRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that a job of another kind is not reached by a stop asking for this one, so that work sharing the table is not halted through a resource it does not belong to")
	void stopJobOfAnotherType() {
		when(jobRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndType(JOB_ID, NAMESPACE, MUNICIPALITY_ID, ERRAND_PURGE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> jobService.stop(NAMESPACE, MUNICIPALITY_ID, JOB_ID, ERRAND_PURGE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessageContaining(JOB_ID)
			.hasMessageContaining(NAMESPACE)
			.hasMessageContaining(MUNICIPALITY_ID);

		verify(jobRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that what a job keeps as its message carries no line breaks, since a reason is often built from an exception carrying whatever a caller sent in")
	void failSanitizesTheMessage() {
		final var entity = jobEntity(RUNNING);
		when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(entity));

		jobService.fail(JOB_ID, "Purge aborted: broken\r\n2026-08-31 INFO Everything is fine");

		assertThat(entity.getMessage()).isEqualTo("Purge aborted: broken  2026-08-31 INFO Everything is fine");
	}

	@Test
	@DisplayName("Verification that a message arriving with a whole stack trace in it is cut rather than left to fill the row")
	void failBoundsTheMessageLength() {
		final var entity = jobEntity(RUNNING);
		when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(entity));

		jobService.fail(JOB_ID, "x".repeat(5000));

		assertThat(entity.getMessage()).hasSize(1027).endsWith("...");
	}

	@Test
	void statusOf() {
		when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(jobEntity(RUNNING)));

		assertThat(jobService.statusOf(JOB_ID)).contains(RUNNING);
	}

	@Test
	void statusOfUnknownJob() {
		when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.empty());

		assertThat(jobService.statusOf(JOB_ID)).isEmpty();
	}

	@Test
	@DisplayName("Verification that a job nothing is reporting on any more is ended, since nothing else would ever move it and a namespace holding one refuses every run of that kind from then on")
	void failStaleJobs() {
		final var reportedOn = jobEntity(RUNNING).withModified(now(systemDefault()).minusDays(2));
		final var neverReportedOn = jobEntity(PENDING).withCreated(now(systemDefault()).minusDays(2));

		when(jobRepositoryMock.findByStatusInAndModifiedBefore(eq(List.of(PENDING, RUNNING)), any(OffsetDateTime.class)))
			.thenReturn(List.of(reportedOn));
		when(jobRepositoryMock.findByStatusInAndModifiedIsNullAndCreatedBefore(eq(List.of(PENDING, RUNNING)), any(OffsetDateTime.class)))
			.thenReturn(List.of(neverReportedOn));
		when(jobRepositoryMock.saveAll(List.of(reportedOn, neverReportedOn))).thenReturn(List.of(reportedOn, neverReportedOn));

		// Handed back rather than counted, so that whoever reports on this can say which work was left half done
		assertThat(jobService.failStaleJobs(Duration.ofHours(12))).containsExactly(reportedOn, neverReportedOn);

		assertThat(reportedOn.getStatus()).isEqualTo(FAILED);
		assertThat(reportedOn.getMessage()).isEqualTo("Job was not reported on for PT12H and is taken to have ended with the instance carrying it out");
		assertThat(neverReportedOn.getStatus()).isEqualTo(FAILED);
		verify(jobRepositoryMock).saveAll(List.of(reportedOn, neverReportedOn));
	}

	@Test
	@DisplayName("Verification that a job created by an instance which died before it ever reported is reached too, since it has no modified of its own to be found by")
	void failStaleJobsAsksForBothTheReportedOnAndTheNeverReportedOn() {
		final var staleAfter = Duration.ofHours(12);
		final var quietSince = ArgumentCaptor.forClass(OffsetDateTime.class);

		jobService.failStaleJobs(staleAfter);

		verify(jobRepositoryMock).findByStatusInAndModifiedBefore(eq(List.of(PENDING, RUNNING)), quietSince.capture());
		verify(jobRepositoryMock).findByStatusInAndModifiedIsNullAndCreatedBefore(eq(List.of(PENDING, RUNNING)), quietSince.capture());

		// Both are asked about the same moment, so that a job is not stale by one reckoning and under way by the other
		assertThat(quietSince.getAllValues()).hasSize(2);
		assertThat(quietSince.getAllValues().getFirst())
			.isEqualTo(quietSince.getAllValues().getLast())
			.isCloseTo(now(systemDefault()).minus(staleAfter), within(5, SECONDS));
	}

	@Test
	@DisplayName("Verification that a job still being reported on is left alone, however long the run behind it has been going")
	void failStaleJobsLeavesJobsUnderWayAlone() {
		when(jobRepositoryMock.findByStatusInAndModifiedBefore(any(), any())).thenReturn(emptyList());
		when(jobRepositoryMock.findByStatusInAndModifiedIsNullAndCreatedBefore(any(), any())).thenReturn(emptyList());

		assertThat(jobService.failStaleJobs(Duration.ofHours(12))).isEmpty();

		verify(jobRepositoryMock).saveAll(emptyList());
	}

	@Test
	@DisplayName("Verification that a job is never removed, whatever state it reached and however long ago: what a run leaves behind is the lasting record that it happened")
	void nothingRemovesJobs() {
		jobService.failStaleJobs(Duration.ofHours(12));

		verify(jobRepositoryMock, never()).delete(any());
		verify(jobRepositoryMock, never()).deleteAll(any());
		verify(jobRepositoryMock, never()).deleteById(any());
	}

	private static JobEntity jobEntity(final JobStatus status) {
		return JobEntity.create()
			.withId(JOB_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withType(MOVE_LABEL)
			.withStatus(status)
			.withProgress(50)
			.withTotal(100)
			.withProcessed(50);
	}
}
