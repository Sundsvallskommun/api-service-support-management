package se.sundsvall.supportmanagement.service;

import java.time.Duration;
import java.util.ArrayList;
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

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.STOPPED;

@Service
public class JobService {

	private static final Logger LOG = LoggerFactory.getLogger(JobService.class);
	private static final int MAX_MESSAGE_LENGTH = 1024;
	private static final String JOB_NOT_FOUND = "Job with id '%s' not found in namespace '%s' for municipality with id '%s'";
	private static final String NOT_REPORTED_ON = "Job was not reported on for %s and is taken to have ended with the instance carrying it out";

	/**
	 * The states a job works in. Held in one place because the guard that keeps two runs of a kind out of the same
	 * namespace and the sweep that ends jobs nobody is reporting on must agree on what counts as under way: a state one
	 * of them treats as active and the other does not is either a namespace blocked for good or a run ended under a
	 * thread still working on it.
	 */
	private static final List<JobStatus> ACTIVE_STATUSES = List.of(JobStatus.PENDING, RUNNING);

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
			job.setMessage(toStoredMessage(message));
			jobRepository.save(job);
		}, () -> LOG.warn("complete called with unknown jobId '{}'", jobId));
	}

	/**
	 * Asks a job to stop. It is marked as stopped straight away, and the work itself ends once it notices - which is what
	 * lets a job be stopped from an instance other than the one carrying it out.
	 * <p>
	 * The kind of job is part of what is looked up rather than checked afterwards. Every kind of work shares this table,
	 * and each reaches it through a resource of its own, so an id that belongs to another kind of job is answered as not
	 * found: a caller asking to stop a purge must not be able to halt an unrelated job by sending its id instead.
	 *
	 * @param  namespace      namespace the job belongs to.
	 * @param  municipalityId id of the municipality the job belongs to.
	 * @param  jobId          id of the job to stop.
	 * @param  type           the kind of job the caller is stopping.
	 * @return                the job as it stands once asked to stop.
	 */
	@Transactional
	public JobResponse stop(final String namespace, final String municipalityId, final String jobId, final JobType type) {
		final var job = findOrThrow(namespace, municipalityId, jobId, type);

		// A job that has already reached a state it cannot leave keeps it, so that a late stop does not rewrite the
		// outcome of a job that was already done.
		if (ACTIVE_STATUSES.contains(job.getStatus())) {
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
			job.setMessage(toStoredMessage(message));
			jobRepository.save(job);
		}, () -> LOG.warn("fail called with unknown jobId '{}'", jobId));
	}

	/**
	 * What a job is allowed to keep as its message.
	 * <p>
	 * A reason is often built from the message of an exception, which can carry whatever a caller sent in, and what is
	 * stored here is read back through the API and written to a log by whoever reads it. Line breaks and control
	 * characters are taken out at this point, so that no producer has to remember to.
	 * <p>
	 * The length is bounded for the same reason: the column holds a sentence for a person to read, and a message that
	 * arrives carrying a whole stack trace should be cut rather than fill the row.
	 */
	private static String toStoredMessage(final String message) {
		return ofNullable(sanitizeForLogging(message))
			.map(sanitized -> sanitized.length() > MAX_MESSAGE_LENGTH ? sanitized.substring(0, MAX_MESSAGE_LENGTH) + "..." : sanitized)
			.orElse(null);
	}

	public boolean hasActiveJob(final String namespace, final String municipalityId) {
		return jobRepository.existsByNamespaceAndMunicipalityIdAndStatusIn(namespace, municipalityId, ACTIVE_STATUSES);
	}

	/**
	 * Whether a job of one kind is already under way, for work that only rules out another run of its own kind rather
	 * than every other job in the namespace.
	 */
	public boolean hasActiveJob(final String namespace, final String municipalityId, final JobType type) {
		return jobRepository.existsByNamespaceAndMunicipalityIdAndTypeAndStatusIn(namespace, municipalityId, type, ACTIVE_STATUSES);
	}

	/**
	 * Ends the jobs that stopped being reported on.
	 * <p>
	 * Work writes to its job as it goes, so a job that has not been written to for far longer than it takes to report is
	 * one whose instance is no longer there to write it: taken down mid run by a restart, an eviction, or something the
	 * thread could not report on its way out. Nothing else would ever move it. The row would read as running for as long
	 * as it lives, and the guard that keeps two runs of a kind out of the same namespace would go on refusing every run
	 * of that kind from then on.
	 * <p>
	 * A job ended here that in fact still has a run behind it costs nothing: what a run reads to know whether it is still
	 * wanted is the job, so it stops itself at the next batch rather than carrying on against a job that has ended.
	 *
	 * @param  staleAfter how long a job may go without being written to before it is taken to have ended with its
	 *                    instance.
	 * @return            the jobs that were ended, so that a caller can say which work was left half done rather than
	 *                    only how much of it there was.
	 */
	@Transactional
	public List<JobEntity> failStaleJobs(final Duration staleAfter) {
		final var quietSince = now(systemDefault()).minus(staleAfter);

		// Asked for in two parts rather than one: a job that was created by an instance which died before it ever
		// reported has no modified of its own, and is reached through the moment it was created instead.
		final var stale = new ArrayList<>(jobRepository.findByStatusInAndModifiedBefore(ACTIVE_STATUSES, quietSince));
		stale.addAll(jobRepository.findByStatusInAndModifiedIsNullAndCreatedBefore(ACTIVE_STATUSES, quietSince));

		stale.forEach(job -> {
			LOG.warn("Job {} of type {} in namespace {} for municipality {} was last written to at {} and is ended as failed",
				job.getId(), job.getType(), sanitizeForLogging(job.getNamespace()), sanitizeForLogging(job.getMunicipalityId()),
				ofNullable(job.getModified()).orElse(job.getCreated()));

			job.setStatus(FAILED);
			job.setMessage(toStoredMessage(NOT_REPORTED_ON.formatted(staleAfter)));
		});

		return jobRepository.saveAll(stale);
	}

	private JobEntity findOrThrow(final String namespace, final String municipalityId, final String jobId) {
		return jobRepository.findByIdAndNamespaceAndMunicipalityId(jobId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, JOB_NOT_FOUND.formatted(jobId, namespace, municipalityId)));
	}

	private JobEntity findOrThrow(final String namespace, final String municipalityId, final String jobId, final JobType type) {
		return jobRepository.findByIdAndNamespaceAndMunicipalityIdAndType(jobId, namespace, municipalityId, type)
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
