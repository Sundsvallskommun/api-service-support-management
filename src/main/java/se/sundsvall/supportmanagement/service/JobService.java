package se.sundsvall.supportmanagement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.integration.db.JobRepository;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobType;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;

@Service
public class JobService {

	private static final String JOB_NOT_FOUND = "Job with id '%s' not found in namespace '%s' for municipality with id '%s'";

	private final JobRepository jobRepository;

	JobService(final JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	@Transactional
	public String create(final String namespace, final String municipalityId, final JobType type, final int total) {
		return jobRepository.save(JobEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withType(type)
			.withTotal(total)).getId();
	}

	@Transactional(readOnly = true)
	public JobResponse get(final String namespace, final String municipalityId, final String jobId) {
		return toJobResponse(findOrThrow(namespace, municipalityId, jobId));
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void setRunning(final String jobId) {
		jobRepository.findById(jobId).ifPresent(job -> {
			job.setStatus(RUNNING);
			jobRepository.save(job);
		});
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void updateProgress(final String jobId, final int processed) {
		jobRepository.findById(jobId).ifPresent(job -> {
			job.setProcessed(processed);
			job.setProgress(job.getTotal() == null || job.getTotal() == 0 ? 100 : (processed * 100 / job.getTotal()));
			jobRepository.save(job);
		});
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void complete(final String jobId) {
		jobRepository.findById(jobId).ifPresent(job -> {
			job.setStatus(COMPLETED);
			job.setProgress(100);
			jobRepository.save(job);
		});
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void fail(final String jobId, final String message) {
		jobRepository.findById(jobId).ifPresent(job -> {
			job.setStatus(FAILED);
			job.setMessage(message);
			jobRepository.save(job);
		});
	}

	public boolean hasActiveJob(final String namespace, final String municipalityId) {
		return jobRepository.existsByNamespaceAndMunicipalityIdAndStatusIn(namespace, municipalityId, java.util.List.of(JobStatus.PENDING, RUNNING));
	}

	private JobEntity findOrThrow(final String namespace, final String municipalityId, final String jobId) {
		return jobRepository.findByIdAndNamespaceAndMunicipalityId(jobId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, JOB_NOT_FOUND.formatted(jobId, namespace, municipalityId)));
	}

	private static JobResponse toJobResponse(final JobEntity entity) {
		return JobResponse.create()
			.withJobId(entity.getId())
			.withType(entity.getType())
			.withStatus(entity.getStatus())
			.withProgress(entity.getProgress())
			.withTotal(entity.getTotal())
			.withProcessed(entity.getProcessed())
			.withMessage(entity.getMessage())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}
}
