package se.sundsvall.supportmanagement.service.purge;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import se.sundsvall.supportmanagement.config.ErrandPurgeProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.service.ErrandService;

import static java.time.OffsetDateTime.now;
import static java.util.Objects.isNull;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.COMPLETED;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.FAILED;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.STOPPED;

/**
 * Walks a namespace and removes the errands that have passed their retention period.
 * <p>
 * Errands are read in batches but removed one at a time, each in a transaction of its own. A namespace can hold far
 * more errands than fit in one transaction, and an errand that cannot be removed must cost only itself: it is counted
 * as failed, its id is logged, and the walk carries on.
 * <p>
 * The batch query is keyed on the id of the last errand reached rather than on an offset, which is what lets the walk
 * pass an errand that failed instead of meeting it again on the next batch.
 */
@Service
public class ErrandPurgeWorker {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandPurgeWorker.class);

	/**
	 * Sorts before every id, so the first batch starts at the beginning of the namespace.
	 */
	private static final String START_OF_NAMESPACE = "";

	private static final String ABORTED_MESSAGE = "Purge aborted: %s";
	private static final String STOPPED_ON_REQUEST = "Stopped on request";
	private static final String LIMIT_REACHED = "Stopped after reaching the limit of errands set for the run";
	private static final String ENDED_WITHOUT_RESULT = "Purge ended without reaching a result of its own";

	private final ErrandsRepository errandsRepository;
	private final ErrandService errandService;
	private final Clock clock;
	private final int batchSize;

	public ErrandPurgeWorker(
		final ErrandsRepository errandsRepository,
		final ErrandService errandService,
		final Clock clock,
		final ErrandPurgeProperties properties) {

		this.errandsRepository = errandsRepository;
		this.errandService = errandService;
		this.clock = clock;
		this.batchSize = properties.batchSize();
	}

	/**
	 * Runs a purge to its end. Never throws: whatever goes wrong ends the run as failed, since the thread this runs on
	 * has nobody to report to.
	 *
	 * @param job the run to carry out.
	 */
	public void run(final PurgeJob job) {
		LOG.info("Purge {} started for namespace {} in municipality {} by {}, removing errands untouched since {}{}",
			job.getJobId(), sanitizeForLogging(job.getNamespace()), sanitizeForLogging(job.getMunicipalityId()), sanitizeForLogging(job.getStartedBy()), job.getOlderThan(),
			job.isDryRun() ? " (dry run, nothing is removed)" : "");

		try {
			walk(job);
		} catch (final Exception e) {
			LOG.error("Purge {} aborted for namespace {} in municipality {}", job.getJobId(), sanitizeForLogging(job.getNamespace()), sanitizeForLogging(job.getMunicipalityId()), e);
			job.finish(FAILED, ABORTED_MESSAGE.formatted(e.getMessage()), now(clock));
		} finally {
			// A thread taken down by something that is not an exception - an Error - would otherwise leave the run reading
			// as RUNNING for as long as the instance lives, since only a finished run is ever evicted from the registry.
			// finish takes effect once, so this changes nothing for a run that reached a state of its own.
			job.finish(FAILED, ENDED_WITHOUT_RESULT, now(clock));
		}

		final var status = job.toStatus();
		LOG.info("Purge {} ended as {} - processed {}, deleted {}, failed {}", job.getJobId(), status.getState(), status.getProcessed(), status.getDeleted(), status.getFailed());
	}

	private void walk(final PurgeJob job) {
		var cursor = START_OF_NAMESPACE;

		while (true) {
			if (job.isStopRequested()) {
				job.finish(STOPPED, STOPPED_ON_REQUEST, now(clock));
				return;
			}

			final var budget = job.remainingBudget();
			if (budget <= 0) {
				job.finish(STOPPED, LIMIT_REACHED, now(clock));
				return;
			}

			final var ids = errandsRepository.findIdsToPurge(job.getNamespace(), job.getMunicipalityId(), job.getOlderThan(), cursor,
				PageRequest.of(0, (int) Math.min(batchSize, budget)));

			if (ids.isEmpty()) {
				job.finish(COMPLETED, null, now(clock));
				return;
			}

			for (final var id : ids) {
				if (job.isStopRequested()) {
					job.finish(STOPPED, STOPPED_ON_REQUEST, now(clock));
					return;
				}

				// Advanced before the errand is handled, so that one which cannot be removed is passed rather than met
				// again on the next batch.
				cursor = id;
				handle(job, id);
			}
		}
	}

	private void handle(final PurgeJob job, final String id) {
		if (job.isDryRun()) {
			job.recordCounted();
			return;
		}

		try {
			// An errand that was already gone counts as reached but not as removed by this run, so that a run does not
			// take credit for a removal it did not make.
			if (errandService.purgeErrand(job.getNamespace(), job.getMunicipalityId(), id)) {
				job.recordDeleted();
			} else {
				job.recordCounted();
			}
		} catch (final Exception e) {
			job.recordFailed();
			LOG.warn("Purge {} could not remove errand {} in namespace {} for municipality {}: {}", job.getJobId(),
				sanitizeForLogging(id), sanitizeForLogging(job.getNamespace()), sanitizeForLogging(job.getMunicipalityId()),
				isNull(e.getMessage()) ? e.getClass().getName() : sanitizeForLogging(e.getMessage()));
		}
	}
}
