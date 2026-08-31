package se.sundsvall.supportmanagement.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.JobRepository;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.PENDING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
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
