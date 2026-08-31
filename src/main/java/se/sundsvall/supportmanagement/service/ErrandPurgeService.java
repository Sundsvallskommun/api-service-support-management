package se.sundsvall.supportmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeRequest;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.purge.ErrandPurgeWorker;
import se.sundsvall.supportmanagement.service.purge.PurgeRun;
import se.sundsvall.supportmanagement.service.purge.PurgeSettings;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.ERRAND_PURGE;

/**
 * Entry point for purging errands that have passed their retention period.
 * <p>
 * A run is answered with a job and carried out on a thread of its own, since walking a namespace one errand at a time
 * can take hours and no caller should be held open for that. Progress is kept in the job table, which the service
 * shares with every other long running piece of work, so a run survives a restart of the instance that started it and
 * can be followed and stopped from any instance.
 */
@Service
public class ErrandPurgeService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandPurgeService.class);

	private static final String ALREADY_RUNNING = "A purge is already running for namespace '%s' in municipality with id '%s'";
	private static final String ACCESS_CONTROL_ACTIVE = "Errands in namespace '%s' for municipality with id '%s' are under access control and cannot be purged";
	private static final String COULD_NOT_START = "Purge could not be started: %s";
	private static final String UNKNOWN_CALLER = "unknown";

	private final ErrandPurgeWorker worker;
	private final JobService jobService;
	private final NamespaceConfigService namespaceConfigService;
	private final AsyncTaskExecutor taskExecutor;

	public ErrandPurgeService(
		final ErrandPurgeWorker worker,
		final JobService jobService,
		final NamespaceConfigService namespaceConfigService,
		@Qualifier("errandPurgeTaskExecutor") final AsyncTaskExecutor taskExecutor) {

		this.worker = worker;
		this.jobService = jobService;
		this.namespaceConfigService = namespaceConfigService;
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Starts a purge run for the sent in namespace and municipality.
	 * <p>
	 * The errands to remove are counted before the run is accepted, both to answer the caller with how much there is to
	 * do and to give the job a total to report progress against. That count is the one thing a caller waits for.
	 *
	 * @param  namespace      namespace to purge within.
	 * @param  municipalityId id of the municipality to purge within.
	 * @param  request        cutoff and run settings.
	 * @return                the job the run reports against.
	 */
	public JobResponse startPurge(final String namespace, final String municipalityId, final ErrandPurgeRequest request) {
		// Two runs walking the same namespace would do each other's work twice over. The check is not a lock: two
		// requests arriving at the same moment can both pass it, which costs duplicated work rather than lost or
		// wrongly removed errands, since an errand already gone is not removed twice.
		if (jobService.hasActiveJob(namespace, municipalityId, ERRAND_PURGE)) {
			throw Problem.valueOf(CONFLICT, ALREADY_RUNNING.formatted(namespace, municipalityId));
		}

		// Settled before a single errand is removed rather than partway through the walk. A purge carries no caller to
		// authorize, so it must not become the way around a namespace that restricts who may reach its errands. Worth a
		// line of its own in the log: an attempt to empty a protected namespace is what an audit would come looking for.
		if (namespaceConfigService.isAccessControlActive(namespace, municipalityId)) {
			LOG.info("Purge of namespace {} in municipality {} requested by {} was refused: access control is active",
				sanitizeForLogging(namespace), sanitizeForLogging(municipalityId), sanitizeForLogging(startedBy()));

			throw Problem.valueOf(CONFLICT, ACCESS_CONTROL_ACTIVE.formatted(namespace, municipalityId));
		}

		final var settings = new PurgeSettings(request.getOlderThan(), TRUE.equals(request.getDryRun()), request.getMaxErrands());
		final var total = worker.countErrandsToPurge(namespace, municipalityId, settings.olderThan());
		final var jobId = jobService.create(namespace, municipalityId, ERRAND_PURGE, total);

		try {
			taskExecutor.execute(() -> worker.run(new PurgeRun(jobId, namespace, municipalityId, startedBy(), settings)));
		} catch (final Exception e) {
			// The job is already there and would otherwise sit waiting for a run that never comes.
			jobService.fail(jobId, COULD_NOT_START.formatted(e.getMessage()));

			throw e instanceof final ThrowableProblem problem ? problem : Problem.valueOf(INTERNAL_SERVER_ERROR, COULD_NOT_START.formatted(e.getMessage()));
		}

		return jobService.get(namespace, municipalityId, jobId);
	}

	/**
	 * Asks a purge run to stop. The run finishes the errand it is on before it does.
	 *
	 * @param  namespace      namespace the run belongs to.
	 * @param  municipalityId id of the municipality the run belongs to.
	 * @param  jobId          id of the job the run reports against.
	 * @return                the job as it stands once asked to stop.
	 */
	public JobResponse stopPurge(final String namespace, final String municipalityId, final String jobId) {
		LOG.info("Purge {} asked to stop for namespace {} in municipality {} by {}", sanitizeForLogging(jobId),
			sanitizeForLogging(namespace), sanitizeForLogging(municipalityId), sanitizeForLogging(startedBy()));

		return jobService.stop(namespace, municipalityId, jobId);
	}

	/**
	 * The caller a run is recorded against. Read here, on the request thread, since the thread carrying out the run has
	 * no identifier of its own to read.
	 */
	private static String startedBy() {
		return ofNullable(Identifier.get())
			.map(Identifier::getValue)
			.orElse(UNKNOWN_CALLER);
	}
}
