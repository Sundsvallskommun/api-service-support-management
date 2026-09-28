package se.sundsvall.supportmanagement.service.job;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.integration.db.JobRepository;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobType;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
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
	private static final String ACTIVE_JOB_IN_NAMESPACE = "A job is already running for namespace '%s' in municipality with id '%s'";

	/**
	 * The states a job works in. Held in one place because the guard that keeps two runs of a kind out of the same
	 * namespace and the sweep that ends jobs nobody is reporting on must agree on what counts as under way: a state one
	 * of them treats as active and the other does not is either a namespace blocked for good or a run ended under a
	 * thread still working on it.
	 */
	private static final List<JobStatus> ACTIVE_STATUSES = List.of(JobStatus.PENDING, RUNNING);

	private final JobRepository jobRepository;

	/**
	 * This same bean, reached through its own Spring proxy rather than through {@code this}. {@link #launch} calls
	 * {@link #fail} and {@link #get} through here so that their {@code @Transactional} requirements - {@code
	 * REQUIRES_NEW} and {@code readOnly} respectively - actually take effect: a plain {@code this} call bypasses the
	 * proxy those requirements live on, silently running with whatever transaction happens to be ambient (often none)
	 * instead. {@code @Lazy} because the proxy this needs is this very bean's own, not yet constructed while its own
	 * constructor runs.
	 */
	private final JobService self;

	JobService(final JobRepository jobRepository, @Lazy final JobService self) {
		this.jobRepository = jobRepository;
		this.self = self;
	}

	/**
	 * Creates a job's row. The only caller is {@link #launch}, which is deliberately not itself {@code @Transactional}
	 * (see its own javadoc for why) - so this needs none of its own either: {@code saveAndFlush} on the repository
	 * carries a transaction of its own regardless of what, if anything, is open in the caller.
	 * <p>
	 * Flushed rather than merely saved, so that a namespace-scoped DB constraint a caller relies on to close a
	 * check-then-act race against its own precheck (see {@code V1_60__add_active_job_guard.sql}) is violated
	 * here, inside this call's own transaction, rather than staying unflushed until some later point picks the
	 * failure up out of context.
	 */
	private String createJob(final String namespace, final String municipalityId, final JobType type, final int total, final String subjectId) {
		try {
			return jobRepository.saveAndFlush(JobEntity.create()
				.withNamespace(namespace)
				.withMunicipalityId(municipalityId)
				.withType(type)
				.withTotal(total)
				.withSubjectId(subjectId)).getId();
		} catch (final DataIntegrityViolationException e) {
			// The only unique constraint this table carries besides its primary key - a second request that raced the
			// precheck above and lost is answered the same way a sequential one already is.
			throw Problem.valueOf(CONFLICT, ACTIVE_JOB_IN_NAMESPACE.formatted(namespace, municipalityId));
		}
	}

	@Transactional(readOnly = true)
	public JobResponse get(final String namespace, final String municipalityId, final String jobId) {
		return toJobResponse(findOrThrow(namespace, municipalityId, jobId));
	}

	/**
	 * Creates a job and dispatches a run against it, in one place - so the transaction boundary the dispatch depends on
	 * lives here once instead of being reproduced, and possibly gotten subtly wrong, by every caller that starts a job
	 * of its own. That is exactly what went wrong the last time this was written out twice: one copy was correctly not
	 * {@code @Transactional} and the other was, and the second one dispatched a run that could not see the job it was
	 * meant to update.
	 * <p>
	 * Deliberately not itself {@code @Transactional}: wrapping this method would hold job creation and the rest of it in
	 * one transaction, so the row {@link #createJob} flushes would not actually be committed before the run is handed to
	 * {@code executor} - the run's own {@link #setRunning}, executing on a different thread in its own
	 * {@code REQUIRES_NEW} transaction, would find no job to update.
	 *
	 * @param  spec                 the job to create - namespace, kind, and everything else {@link #createJob} needs.
	 * @param  executor             the executor to dispatch the run on.
	 * @param  toRun                builds the run once the job's id is known - the run itself always needs it, and
	 *                              needs it first.
	 * @param  runner               carries the run to its end - a {@link JobRunner}'s own {@code run}.
	 * @param  couldNotStartMessage format string with one {@code %s} for the failure reason, used both to fail the job
	 *                              and, wrapped in a {@link Problem}, to answer the caller.
	 * @return                      the job the run reports against.
	 */
	public <R> JobResponse launch(
		final JobSpec spec,
		final AsyncTaskExecutor executor,
		final Function<String, R> toRun,
		final Consumer<R> runner,
		final String couldNotStartMessage) {

		final var jobId = createJob(spec.namespace(), spec.municipalityId(), spec.type(), spec.total(), spec.subjectId());

		try {
			executor.execute(() -> runner.accept(toRun.apply(jobId)));
		} catch (final Exception e) {
			// The job is already there and would otherwise sit waiting for a run that never comes.
			self.fail(jobId, couldNotStartMessage.formatted(e.getMessage()));

			throw e instanceof final ThrowableProblem problem ? problem : Problem.valueOf(INTERNAL_SERVER_ERROR, couldNotStartMessage.formatted(e.getMessage()));
		}

		return self.get(spec.namespace(), spec.municipalityId(), jobId);
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

	/**
	 * Whether a job of one kind is already under way, for work that only rules out another run of its own kind rather
	 * than every other job in the namespace.
	 */
	public boolean hasActiveJob(final String namespace, final String municipalityId, final JobType type) {
		return jobRepository.existsByNamespaceAndMunicipalityIdAndTypeAndStatusIn(namespace, municipalityId, type, ACTIVE_STATUSES);
	}

	/**
	 * Clears the way for a new run of one kind in one namespace, stealing a stale lease rather than leaving the
	 * namespace blocked for as long as {@code staleAfter} - the active-job row doubles as that lease: {@code modified}
	 * is its heartbeat, {@code staleAfter} the duration one may go quiet for, and the guard in
	 * {@code V1_60__add_active_job_guard.sql} (or its counterpart for another type) is what makes it exclusive.
	 * <p>
	 * Deliberately narrower than {@link #failStaleJobs(Duration)}: that sweep ends every kind of job in every namespace
	 * that has gone quiet, on its own schedule; this steals the lease for exactly the one namespace and kind a caller is
	 * about to start a new run against, on demand, so that a caller does not have to wait for the sweep's own cron to
	 * get there first. A namespace with a genuinely active job is still refused - only one that has gone quiet longer
	 * than {@code staleAfter} is failed and reclaimed.
	 * <p>
	 * Committed by the time this call returns (its own transaction, not the caller's): the row a stale lease is
	 * reclaimed from must be failed - and that failure durable - before the caller's own {@link #launch} (via
	 * {@link #createJob}) can succeed against the same unique constraint that refused it a moment ago.
	 *
	 * @param  namespace      the namespace to clear a lease in.
	 * @param  municipalityId the id of the municipality the namespace belongs to.
	 * @param  type           the kind of job to clear a lease for.
	 * @param  staleAfter     how long a job may go without being written to before its lease is taken to be abandoned.
	 * @return                {@code true} if a new run may proceed - no job was active, or a stale one was just failed
	 *                        and reclaimed; {@code false} if a job is still genuinely active and the caller must wait.
	 */
	@Transactional
	public boolean stealStaleLease(final String namespace, final String municipalityId, final JobType type, final Duration staleAfter) {
		final var active = jobRepository.findFirstByNamespaceAndMunicipalityIdAndTypeAndStatusIn(namespace, municipalityId, type, ACTIVE_STATUSES);
		if (active.isEmpty()) {
			return true;
		}

		final var job = active.get();
		final var quietSince = now(systemDefault()).minus(staleAfter);
		final var lastWrite = ofNullable(job.getModified()).orElse(job.getCreated());
		if (!lastWrite.isBefore(quietSince)) {
			return false;
		}

		LOG.warn("Job {} of type {} in namespace {} for municipality {} was last written to at {} and is ended as failed to reclaim its lease for a new run",
			job.getId(), job.getType(), sanitizeForLogging(job.getNamespace()), sanitizeForLogging(job.getMunicipalityId()), lastWrite);
		job.setStatus(FAILED);
		job.setMessage(toStoredMessage(NOT_REPORTED_ON.formatted(staleAfter)));
		jobRepository.saveAndFlush(job);
		return true;
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
