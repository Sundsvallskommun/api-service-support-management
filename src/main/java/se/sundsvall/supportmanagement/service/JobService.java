package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;

@Service
public class JobService {

	private static final Logger LOG = LoggerFactory.getLogger(JobService.class);
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
		jobRepository.findById(jobId).ifPresentOrElse(job -> {
			job.setStatus(RUNNING);
			jobRepository.save(job);
		}, () -> LOG.warn("setRunning called with unknown jobId '{}'", jobId));
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void updateProgress(final String jobId, final int processed) {
		jobRepository.findById(jobId).ifPresentOrElse(job -> {
			final var total = job.getTotal();
			final var rawProgress = (total == null || total == 0) ? 100 : (processed * 100 / total);
			job.setProcessed(processed);
			job.setProgress(Math.min(100, Math.max(0, rawProgress)));
			jobRepository.save(job);
		}, () -> LOG.warn("updateProgress called with unknown jobId '{}'", jobId));
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void complete(final String jobId) {
		jobRepository.findById(jobId).ifPresentOrElse(job -> {
			job.setStatus(COMPLETED);
			job.setProgress(100);
			jobRepository.save(job);
		}, () -> LOG.warn("complete called with unknown jobId '{}'", jobId));
	}

	/**
	 * Ends a job that reached its end with something worth saying about the outcome, such as how much of what it walked
	 * it actually removed.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	public void complete(final String jobId, final String message) {
		jobRepository.findById(jobId).ifPresentOrElse(job -> {
			job.setStatus(COMPLETED);
			job.setProgress(100);
			job.setMessage(message);
			jobRepository.save(job);
		}, () -> LOG.warn("complete called with unknown jobId '{}'", jobId));
	}

	/**
	 * Asks a job to stop. It is marked as stopped straight away, and the work itself ends once it notices - which is what
	 * lets a job be stopped from an instance other than the one carrying it out.
	 *
	 * @param  namespace      namespace the job belongs to.
	 * @param  municipalityId id of the municipality the job belongs to.
	 * @param  jobId          id of the job to stop.
	 * @return                the job as it stands once asked to stop.
	 */
	@Transactional
	public JobResponse stop(final String namespace, final String municipalityId, final String jobId) {
		final var job = findOrThrow(namespace, municipalityId, jobId);

		// A job that has already reached a state it cannot leave keeps it, so that a late stop does not rewrite the
		// outcome of a job that was already done.
		if (List.of(JobStatus.PENDING, RUNNING).contains(job.getStatus())) {
			job.setStatus(STOPPED);
			jobRepository.save(job);
		}

		return toJobResponse(job);
	}

	/**
	 * The state a job is in, for work that needs to know whether it is still wanted. Empty for a job that is not there.
	 */
	@Transactional(readOnly = true)
	public Optional<JobStatus> statusOf(final String jobId) {
		return jobRepository.findById(jobId).map(JobEntity::getStatus);
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void fail(final String jobId, final String message) {
		jobRepository.findById(jobId).ifPresentOrElse(job -> {
			job.setStatus(FAILED);
			job.setMessage(message);
			jobRepository.save(job);
		}, () -> LOG.warn("fail called with unknown jobId '{}'", jobId));
	}

	public boolean hasActiveJob(final String namespace, final String municipalityId) {
		return jobRepository.existsByNamespaceAndMunicipalityIdAndStatusIn(namespace, municipalityId, List.of(JobStatus.PENDING, RUNNING));
	}

	/**
	 * Whether a job of one kind is already under way, for work that only rules out another run of its own kind rather
	 * than every other job in the namespace.
	 */
	public boolean hasActiveJob(final String namespace, final String municipalityId, final JobType type) {
		return jobRepository.existsByNamespaceAndMunicipalityIdAndTypeAndStatusIn(namespace, municipalityId, type, List.of(JobStatus.PENDING, RUNNING));
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
