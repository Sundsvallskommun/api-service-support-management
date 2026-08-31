package se.sundsvall.supportmanagement.service.purge;

import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import se.sundsvall.supportmanagement.config.ErrandPurgeProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.IdProjection;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.JobService;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withIdAfter;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withLastTouchedBefore;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

/**
 * Walks a namespace and removes the errands that have passed their retention period.
 * <p>
 * Errands are read in batches but removed one at a time, each in a transaction of its own. A namespace can hold far
 * more errands than fit in one transaction, and an errand that cannot be removed must cost only itself: it is counted
 * as failed, its id is logged, and the walk carries on.
 * <p>
 * The batch query is keyed on the id of the last errand reached rather than on an offset, which is what lets the walk
 * pass an errand that failed instead of meeting it again on the next batch.
 * <p>
 * Progress is written to the job between batches rather than after every errand, and the same moment is used to read
 * whether the job has been asked to stop. A run therefore stops on a batch boundary, and it does so wherever the stop
 * was asked for, since the answer comes from the job table rather than from this instance.
 */
@Service
public class ErrandPurgeWorker {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandPurgeWorker.class);

	private static final String ID_ATTRIBUTE = "id";

	private static final String ABORTED_MESSAGE = "Purge aborted: %s";
	private static final String DRY_RUN_SUMMARY = "Dry run over %d errands, none of which were removed";
	private static final String RUN_SUMMARY = "Removed %d of %d errands reached, %d could not be removed";
	private static final String ENDED_WITHOUT_RESULT = "Purge ended without reaching a result of its own";
	private static final String ACCESS_CONTROL_SWITCHED_ON = "Purge ended after removing %d errands: access control was switched on for the namespace while it was running";

	private final ErrandsRepository errandsRepository;
	private final ErrandService errandService;
	private final JobService jobService;
	private final NamespaceConfigService namespaceConfigService;
	private final int batchSize;

	public ErrandPurgeWorker(
		final ErrandsRepository errandsRepository,
		final ErrandService errandService,
		final JobService jobService,
		final NamespaceConfigService namespaceConfigService,
		final ErrandPurgeProperties properties) {

		this.errandsRepository = errandsRepository;
		this.errandService = errandService;
		this.jobService = jobService;
		this.namespaceConfigService = namespaceConfigService;
		this.batchSize = properties.batchSize();
	}

	/**
	 * How many errands a run over this namespace would reach, which is what the job reports its progress against.
	 *
	 * @param  namespace      the namespace to purge within.
	 * @param  municipalityId the id of the municipality to purge within.
	 * @param  cutoff         errands last touched before this point in time are counted.
	 * @return                the number of errands that have passed their retention period.
	 */
	public int countErrandsToPurge(final String namespace, final String municipalityId, final OffsetDateTime cutoff) {
		return (int) Math.min(Integer.MAX_VALUE, errandsRepository.count(reachedBy(namespace, municipalityId, cutoff)));
	}

	/**
	 * Runs a purge to its end. Never throws: whatever goes wrong ends the job as failed, since the thread this runs on
	 * has nobody to report to.
	 *
	 * @param run the run to carry out.
	 */
	public void run(final PurgeRun run) {
		LOG.info("Purge {} started for namespace {} in municipality {} by {}, removing errands untouched since {}{}",
			run.jobId(), sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), sanitizeForLogging(run.startedBy()),
			run.settings().olderThan(), run.settings().dryRun() ? " (dry run, nothing is removed)" : "");

		final var counters = new Counters();
		var ended = false;

		try {
			jobService.setRunning(run.jobId());
			ended = walk(run, counters);
		} catch (final Exception e) {
			LOG.error("Purge {} aborted for namespace {} in municipality {}", run.jobId(), sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), e);
			jobService.fail(run.jobId(), ABORTED_MESSAGE.formatted(e.getMessage()));
			ended = true;
		} finally {
			// A thread taken down by something that is not an exception - an Error - would otherwise leave the job reading
			// as running for as long as the row lives.
			if (!ended) {
				jobService.fail(run.jobId(), ENDED_WITHOUT_RESULT);
			}
		}

		LOG.info("Purge {} ended - processed {}, deleted {}, failed {}", run.jobId(), counters.processed, counters.deleted, counters.failed);
	}

	/**
	 * Walks the namespace to the end of the work, and says whether it left the job in a state of its own. A walk that
	 * ends because the job was stopped leaves it as stopped, which is a state of its own even though this run did not
	 * write it.
	 */
	private boolean walk(final PurgeRun run, final Counters counters) {
		String cursor = null;

		while (true) {
			final var budget = remainingBudget(run, counters);
			if (budget <= 0) {
				jobService.complete(run.jobId(), summaryOf(run, counters));
				return true;
			}

			final var ids = nextBatch(run, cursor, (int) Math.min(batchSize, budget));

			if (ids.isEmpty()) {
				jobService.complete(run.jobId(), summaryOf(run, counters));
				return true;
			}

			for (final var id : ids) {
				// Advanced before the errand is handled, so that one which cannot be removed is passed rather than met
				// again on the next batch.
				cursor = id;
				handle(run, id, counters);
			}

			jobService.updateProgress(run.jobId(), counters.processed);

			// Asked between batches, and answered by the job rather than by this instance, so that a run can be stopped
			// from wherever the request happens to land.
			if (isStopped(run)) {
				LOG.info("Purge {} stopped after {} errands", run.jobId(), counters.processed);
				return true;
			}

			// Asked again for every batch rather than only when the run was accepted. A run lasts hours, and a namespace
			// that has been put under access control in the meantime must not keep having its errands removed by a run
			// that started before the guard went up.
			if (namespaceConfigService.isAccessControlActive(run.namespace(), run.municipalityId())) {
				LOG.warn("Purge {} of namespace {} in municipality {} ended after {} errands: access control was switched on while it was running",
					run.jobId(), sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), counters.processed);
				jobService.fail(run.jobId(), ACCESS_CONTROL_SWITCHED_ON.formatted(counters.deleted));
				return true;
			}
		}
	}

	private void handle(final PurgeRun run, final String id, final Counters counters) {
		counters.processed++;

		if (run.settings().dryRun()) {
			return;
		}

		try {
			// An errand that was already gone counts as reached but not as removed by this run, so that a run does not
			// take credit for a removal it did not make.
			if (errandService.purgeErrand(run.namespace(), run.municipalityId(), id)) {
				counters.deleted++;
			}
		} catch (final Exception e) {
			counters.failed++;
			LOG.warn("Purge {} could not remove errand {} in namespace {} for municipality {}: {}", run.jobId(),
				sanitizeForLogging(id), sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()),
				isNull(e.getMessage()) ? e.getClass().getName() : sanitizeForLogging(e.getMessage()));
		}
	}

	/**
	 * The ids of the next batch, in ascending id order and starting after the id the previous batch ended on.
	 * <p>
	 * Only the ids are read. An errand carries collections that are fetched with it, and a walk that loaded whole
	 * errands only to remove them one at a time in transactions of their own would pay for all of that twice.
	 */
	private List<String> nextBatch(final PurgeRun run, final String cursor, final int size) {
		final var specification = ofNullable(cursor)
			.map(id -> reachedBy(run.namespace(), run.municipalityId(), run.settings().olderThan()).and(withIdAfter(id)))
			.orElseGet(() -> reachedBy(run.namespace(), run.municipalityId(), run.settings().olderThan()));

		return errandsRepository.findBy(specification, query -> query
			.project(ID_ATTRIBUTE)
			.sortBy(Sort.by(ASC, ID_ATTRIBUTE))
			.limit(size)
			.as(IdProjection.class)
			.all())
			.stream()
			.map(IdProjection::getId)
			.toList();
	}

	/**
	 * The errands a run over this namespace reaches: the ones that have not been touched since the cutoff it was started
	 * with.
	 */
	private static Specification<ErrandEntity> reachedBy(final String namespace, final String municipalityId, final OffsetDateTime cutoff) {
		return withNamespace(namespace)
			.and(withMunicipalityId(municipalityId))
			.and(withLastTouchedBefore(cutoff));
	}

	/**
	 * How many more errands the run may handle before it reaches the limit it was started with. A run without a limit is
	 * given one that outlasts the service, so that the counting needs no special case.
	 */
	private static long remainingBudget(final PurgeRun run, final Counters counters) {
		return isNull(run.settings().maxErrands()) ? Long.MAX_VALUE : (long) run.settings().maxErrands() - counters.processed;
	}

	/**
	 * Whether the job has left the state a run works in. A job that is gone counts as stopped too: there is nothing left
	 * to report against, so there is no reason to keep removing errands on its behalf.
	 */
	private boolean isStopped(final PurgeRun run) {
		return !jobService.statusOf(run.jobId())
			.filter(RUNNING::equals)
			.isPresent();
	}

	private static String summaryOf(final PurgeRun run, final Counters counters) {
		return run.settings().dryRun()
			? DRY_RUN_SUMMARY.formatted(counters.processed)
			: RUN_SUMMARY.formatted(counters.deleted, counters.processed, counters.failed);
	}

	/**
	 * The tally of a single run. Written and read by the one thread carrying the run out, which is why it needs nothing
	 * to make it safe across threads: what the outside world reads is the job, not this.
	 */
	private static final class Counters {
		private int processed;
		private int deleted;
		private int failed;
	}
}
