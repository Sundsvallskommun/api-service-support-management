package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Carries out label moves accepted by {@link MetadataService#startLabelMove}.
 * <p>
 * A move re-parents one label, refreshes {@code resourcePath} for its whole subtree, and then walks every errand that
 * references the moved label (or any of its descendants — captured for free since an errand's stored label set already
 * carries the full ancestor chain) restowing its label set page by page. Never throws: whatever goes wrong ends the job
 * as failed, since the thread this runs on has nobody to report to.
 */
@Component
public class LabelMoveWorker {

	private static final Logger LOG = LoggerFactory.getLogger(LabelMoveWorker.class);

	private static final String ABORTED_MESSAGE = "Label move aborted: %s";
	private static final String ENDED_WITHOUT_RESULT = "Label move ended without reaching a result of its own";
	private static final String SUMMARY = "Label %s moved under %s, %d errand(s) restowed";
	private static final String AUDIT_MESSAGE = "Label %s moved under %s by %s, %d errand(s) restowed";
	private static final String LABEL_GONE = "Label %s no longer exists";
	private static final String NEW_PARENT_GONE = "New parent %s no longer exists";
	private static final int MAX_BATCH_ATTEMPTS = 3;

	private final ErrandsRepository errandsRepository;
	private final MetadataLabelRepository metadataLabelRepository;
	private final ErrandService errandService;
	private final JobService jobService;
	private final EventService eventService;
	private final int batchSize;
	private final long progressIntervalNanos;

	LabelMoveWorker(
		final ErrandsRepository errandsRepository,
		final MetadataLabelRepository metadataLabelRepository,
		final ErrandService errandService,
		final JobService jobService,
		final EventService eventService,
		final LabelMoveProperties properties) {
		this.errandsRepository = errandsRepository;
		this.metadataLabelRepository = metadataLabelRepository;
		this.errandService = errandService;
		this.jobService = jobService;
		this.eventService = eventService;
		this.batchSize = properties.batchSize();
		this.progressIntervalNanos = properties.progressInterval().toNanos();
	}

	/**
	 * Runs a label move to its end.
	 *
	 * @param run the run to carry out.
	 */
	public void run(final LabelMoveRun run) {
		LOG.info("Label move {} started for label {} to parent {} in namespace {} for municipality {} by {}",
			run.jobId(), sanitizeForLogging(run.labelId()), sanitizeForLogging(run.newParentId()),
			sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), sanitizeForLogging(run.startedBy()));

		var ended = false;

		try {
			jobService.setRunning(run.jobId());
			move(run);
			ended = true;
		} catch (final Exception e) {
			LOG.error("Label move {} aborted for label {} in namespace {}", run.jobId(), sanitizeForLogging(run.labelId()), sanitizeForLogging(run.namespace()), e);
			jobService.fail(run.jobId(), ABORTED_MESSAGE.formatted(e.getMessage()));
			ended = true;
		} finally {
			// A thread taken down by something that is not an exception - an Error - would otherwise leave the job reading
			// as running for as long as it lives.
			if (!ended) {
				jobService.fail(run.jobId(), ENDED_WITHOUT_RESULT);
			}
		}

		LOG.info("Label move {} ended", run.jobId());
	}

	private void move(final LabelMoveRun run) {
		final var labelToMove = metadataLabelRepository.findById(run.labelId())
			.orElseThrow(() -> new IllegalStateException(LABEL_GONE.formatted(run.labelId())));
		final var newParent = run.newParentId() != null
			? metadataLabelRepository.findById(run.newParentId()).orElseThrow(() -> new IllegalStateException(NEW_PARENT_GONE.formatted(run.newParentId())))
			: null;

		labelToMove.setParent(newParent);
		metadataLabelRepository.saveAndFlush(labelToMove);
		// The @PreUpdate cascade on labelToMove (onUpdate -> updateChildrenPathsRecursively) recomputes resourcePath for
		// the moved node and, recursively, for every descendant reachable through its metadataLabels collection, all
		// within this single saveAndFlush's own session - the whole subtree is already correct and persisted by the
		// time this call returns. A second pass re-querying by the old resourcePath prefix would find nothing (the
		// cascade above already moved every descendant off it), and refreshing whatever it did find by walking a lazy
		// getParent() chain would run on entities already detached from that query's own, separate transaction.

		final var restowed = restowErrands(run);

		eventService.createLabelMoveEvent(run.municipalityId(), run.labelId(), run.startedBy(), AUDIT_MESSAGE.formatted(run.labelId(), run.newParentId(), run.startedBy(), restowed));

		jobService.complete(run.jobId(), SUMMARY.formatted(run.labelId(), run.newParentId(), restowed));
	}

	/**
	 * Restows every errand in {@link LabelMoveRun#errandIds()} - the frozen set resolved once when the move was
	 * accepted, which is also what the job's own {@code total} was set from. Read a page at a time for I/O efficiency,
	 * but persisted one errand at a time, each in a transaction of its own - mirrors {@link
	 * se.sundsvall.supportmanagement.service.purge.ErrandPurgeWorker#walk}, and for the same reason: progress is
	 * reported not only at the page boundary but also from inside a page whenever {@code progressIntervalNanos} has
	 * elapsed, so a page that is merely slow keeps saying so instead of going quiet long enough for
	 * {@link JobProperties#staleAfter()} to take the job for abandoned mid-flight.
	 * <p>
	 * A plain paged walk over a fixed id list, not keyset paging: the set cannot change shape mid-walk the way a
	 * label-id-scoped query's result could, since it was already resolved before this runs.
	 */
	private int restowErrands(final LabelMoveRun run) {
		var ids = run.errandIds();
		var processed = 0;
		var lastReport = System.nanoTime();

		for (var start = 0; start < ids.size(); start += batchSize) {
			var pageIds = ids.subList(start, Math.min(start + batchSize, ids.size()));

			for (var errand : errandsRepository.findAllById(pageIds)) {
				persistWithRetry(run, errand);
				processed++;

				if (System.nanoTime() - lastReport >= progressIntervalNanos) {
					jobService.updateProgress(run.jobId(), processed);
					lastReport = System.nanoTime();
				}
			}

			jobService.updateProgress(run.jobId(), processed);
			lastReport = System.nanoTime();
		}

		return processed;
	}

	/**
	 * Persists one errand's restow, retrying against a fresh read when a concurrent edit - a user PATCHing this errand
	 * between the read and the merge, which {@code ErrandEntity}'s {@code @Version} turns into a lock conflict rather
	 * than a silently lost update - loses the optimistic-lock race. A stale, already-detached instance would just fail
	 * the same way again, so each retry re-reads rather than reusing it. An errand that has disappeared by the time of
	 * a retry (purged, most likely) needs no restow at all - it is simply left out rather than treated as a failure of
	 * this one errand.
	 */
	private void persistWithRetry(final LabelMoveRun run, final ErrandEntity firstRead) {
		var errand = firstRead;
		var attempt = 0;

		while (errand != null) {
			attempt++;
			try {
				errandService.persistLabelMigrationBatch(List.of(errand));
				return;
			} catch (final ObjectOptimisticLockingFailureException e) {
				if (attempt == MAX_BATCH_ATTEMPTS) {
					throw e;
				}
				LOG.warn("Label move {} retrying errand {} after a concurrent edit lost the optimistic-lock race (attempt {}/{})",
					run.jobId(), sanitizeForLogging(errand.getId()), attempt, MAX_BATCH_ATTEMPTS);
				errand = errandsRepository.findAllById(List.of(errand.getId())).stream().findFirst().orElse(null);
			}
		}
	}
}
