package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeRequest;
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeStatus;
import se.sundsvall.supportmanagement.config.ErrandPurgeProperties;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.purge.ErrandPurgeWorker;
import se.sundsvall.supportmanagement.service.purge.PurgeJob;

import static java.lang.Boolean.TRUE;
import static java.time.OffsetDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Entry point for purging errands that have passed their retention period.
 * <p>
 * A run is answered with a job id and carried out on a thread of its own, since walking a namespace one errand at a
 * time can take hours and no caller should be held open for that.
 * <p>
 * Progress is kept in memory, which is a deliberate limitation. The registry lives no longer than the instance holding
 * it, so a restart loses the ability to follow a run that was under way, and a status request reaching a different
 * instance than the one doing the work finds no job. What has already been removed stays removed either way, since the
 * deletions are committed one errand at a time. The status resource is a convenience; the removals are the outcome that
 * matters.
 */
@Service
public class ErrandPurgeService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandPurgeService.class);

	private static final String ALREADY_RUNNING = "A purge is already running for namespace '%s' in municipality with id '%s'";
	private static final String JOB_NOT_FOUND = "A purge job with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String ACCESS_CONTROL_ACTIVE = "Errands in namespace '%s' for municipality with id '%s' are under access control and cannot be purged";
	private static final String COULD_NOT_START = "Purge could not be started: %s";
	private static final String UNKNOWN_CALLER = "unknown";

	private static final String SCOPE_FORMAT = "%s|%s";

	private final Map<String, PurgeJob> jobs = new ConcurrentHashMap<>();

	/**
	 * The job holding a namespace, which is what keeps two runs from walking the same errands at once. Claimed before the
	 * job is registered and released the moment the run ends.
	 */
	private final Map<String, String> jobIdByScope = new ConcurrentHashMap<>();

	private final ErrandPurgeWorker worker;
	private final NamespaceConfigService namespaceConfigService;
	private final AsyncTaskExecutor taskExecutor;
	private final Clock clock;
	private final Duration jobRetention;

	public ErrandPurgeService(
		final ErrandPurgeWorker worker,
		final NamespaceConfigService namespaceConfigService,
		@Qualifier("errandPurgeTaskExecutor") final AsyncTaskExecutor taskExecutor,
		final Clock clock,
		final ErrandPurgeProperties properties) {

		this.worker = worker;
		this.namespaceConfigService = namespaceConfigService;
		this.taskExecutor = taskExecutor;
		this.clock = clock;
		this.jobRetention = properties.jobRetention();
	}

	/**
	 * Starts a purge run for the sent in namespace and municipality.
	 *
	 * @param  namespace      namespace to purge within.
	 * @param  municipalityId id of the municipality to purge within.
	 * @param  request        cutoff and run settings.
	 * @return                the state of the started run.
	 */
	public ErrandPurgeStatus startPurge(final String namespace, final String municipalityId, final ErrandPurgeRequest request) {
		evictExpiredJobs();

		final var scope = scopeOf(namespace, municipalityId);
		final var job = new PurgeJob(randomUUID().toString(), namespace, municipalityId, request.getOlderThan(),
			TRUE.equals(request.getDryRun()), request.getMaxErrands(), startedBy(), now(clock));

		if (nonNull(jobIdByScope.putIfAbsent(scope, job.getJobId()))) {
			throw Problem.valueOf(CONFLICT, ALREADY_RUNNING.formatted(namespace, municipalityId));
		}

		// Everything between claiming the namespace and handing the run to a thread has to release the claim if it goes
		// wrong. A refusal is answered as such rather than as an accepted run that immediately failed, since a caller who
		// is told 202 has no reason to look at the state it was given.
		try {
			// Settled before a single errand is removed rather than partway through the walk. A purge carries no caller to
			// authorize, so it must not become the way around a namespace that restricts who may reach its errands.
			if (namespaceConfigService.isAccessControlActive(namespace, municipalityId)) {
				throw Problem.valueOf(CONFLICT, ACCESS_CONTROL_ACTIVE.formatted(namespace, municipalityId));
			}

			jobs.put(job.getJobId(), job);

			taskExecutor.execute(() -> {
				try {
					worker.run(job);
				} finally {
					jobIdByScope.remove(scope, job.getJobId());
				}
			});
		} catch (final Exception e) {
			jobIdByScope.remove(scope, job.getJobId());
			jobs.remove(job.getJobId());

			LOG.info("Purge {} was not started for namespace {} in municipality {} requested by {}: {}", job.getJobId(),
				sanitizeForLogging(namespace), sanitizeForLogging(municipalityId), sanitizeForLogging(job.getStartedBy()), e.getMessage());

			throw e instanceof final ThrowableProblem problem ? problem : Problem.valueOf(INTERNAL_SERVER_ERROR, COULD_NOT_START.formatted(e.getMessage()));
		}

		return job.toStatus();
	}

	/**
	 * Reads the state of a purge run.
	 *
	 * @param  namespace      namespace the run belongs to.
	 * @param  municipalityId id of the municipality the run belongs to.
	 * @param  jobId          id of the run.
	 * @return                the state of the run.
	 */
	public ErrandPurgeStatus readPurgeStatus(final String namespace, final String municipalityId, final String jobId) {
		return jobOf(namespace, municipalityId, jobId).toStatus();
	}

	/**
	 * Asks a purge run to stop. The run finishes the errand it is on before it does.
	 *
	 * @param  namespace      namespace the run belongs to.
	 * @param  municipalityId id of the municipality the run belongs to.
	 * @param  jobId          id of the run.
	 * @return                the state of the run at the time the stop was requested.
	 */
	public ErrandPurgeStatus stopPurge(final String namespace, final String municipalityId, final String jobId) {
		final var job = jobOf(namespace, municipalityId, jobId);
		job.requestStop();

		LOG.info("Purge {} asked to stop for namespace {} in municipality {}", jobId, sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));

		return job.toStatus();
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

	/**
	 * A job belonging to another namespace answers as though it did not exist, so that the resource does not confirm an
	 * id to a caller who cannot reach the run it names.
	 */
	private PurgeJob jobOf(final String namespace, final String municipalityId, final String jobId) {
		return ofNullable(jobs.get(jobId))
			.filter(job -> job.getNamespace().equals(namespace) && job.getMunicipalityId().equals(municipalityId))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, JOB_NOT_FOUND.formatted(jobId, namespace, municipalityId)));
	}

	/**
	 * Drops finished runs that have been readable for longer than the retention window, so that the registry does not
	 * keep growing for as long as the instance lives.
	 * <p>
	 * Done when a run is started, which is the only thing that adds to the registry, and never when one is read - a poll
	 * must not be able to remove the very run it asks about.
	 */
	private void evictExpiredJobs() {
		final var cutoff = now(clock).minus(jobRetention);

		jobs.values().removeIf(job -> ofNullable(job.getFinished())
			.map(finished -> finished.isBefore(cutoff))
			.orElse(false));
	}

	private static String scopeOf(final String namespace, final String municipalityId) {
		return SCOPE_FORMAT.formatted(namespace, municipalityId);
	}
}
