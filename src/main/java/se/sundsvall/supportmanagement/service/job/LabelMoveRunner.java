package se.sundsvall.supportmanagement.service.job;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.JobProperties;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.MetadataService;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;

/**
 * Carries out label moves accepted by {@link MetadataService#startLabelMove}.
 * <p>
 * A move re-parents one label, refreshes {@code resourcePath} for its whole subtree, and then walks every errand that
 * references the moved label (or any of its descendants — captured for free since an errand's stored label set already
 * carries the full ancestor chain) restowing its label set page by page. Never throws: whatever goes wrong ends the job
 * as failed, since the thread this runs on has nobody to report to.
 */
@Component
public class LabelMoveRunner extends JobRunner<LabelMoveRun> {

	private static final Logger LOG = LoggerFactory.getLogger(LabelMoveRunner.class);

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

	LabelMoveRunner(
		final ErrandsRepository errandsRepository,
		final MetadataLabelRepository metadataLabelRepository,
		final ErrandService errandService,
		final JobService jobService,
		final EventService eventService,
		final LabelMoveProperties properties) {
		super(jobService);
		this.errandsRepository = errandsRepository;
		this.metadataLabelRepository = metadataLabelRepository;
		this.errandService = errandService;
		this.jobService = jobService;
		this.eventService = eventService;
		this.batchSize = properties.batchSize();
		this.progressIntervalNanos = properties.progressInterval().toNanos();
	}

	@Override
	protected String jobId(final LabelMoveRun run) {
		return run.jobId();
	}

	@Override
	protected void work(final LabelMoveRun run) {
		LOG.info("Label move {} started for label {} to parent {} in namespace {} for municipality {} by {}",
			run.jobId(), sanitizeForLogging(run.labelId()), sanitizeForLogging(run.newParentId()),
			sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), sanitizeForLogging(run.startedBy()));

		move(run);

		LOG.info("Label move {} ended", run.jobId());
	}

	@Override
	protected String reportAborted(final LabelMoveRun run, final Exception e) {
		LOG.error("Label move {} aborted for label {} in namespace {}", run.jobId(), sanitizeForLogging(run.labelId()), sanitizeForLogging(run.namespace()), e);
		return ABORTED_MESSAGE.formatted(e.getMessage());
	}

	@Override
	protected String endedWithoutResultMessage() {
		return ENDED_WITHOUT_RESULT;
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

		// Checked once more here, in addition to restowErrands' own per-page check, since a stop landing on the very
		// last page would otherwise fall through to the audit event and complete() below - both of which must not fire
		// for a run whose lease has since been reclaimed and handed to a second one.
		if (isStopped(run)) {
			LOG.info("Label move {} stopped after restowing {} errand(s)", run.jobId(), restowed);
			return;
		}

		eventService.createLabelMoveEvent(run.municipalityId(), run.labelId(), run.startedBy(), AUDIT_MESSAGE.formatted(run.labelId(), run.newParentId(), run.startedBy(), restowed));

		jobService.complete(run.jobId(), SUMMARY.formatted(run.labelId(), run.newParentId(), restowed));
	}

	/**
	 * Restows every errand in {@link LabelMoveRun#errandIds()} - the frozen set resolved once when the move was
	 * accepted, which is also what the job's own {@code total} was set from. Read a page at a time for I/O efficiency,
	 * but persisted one errand at a time, each in a transaction of its own - mirrors {@link ErrandPurgeRunner#walk},
	 * and for the same reason: progress is
	 * reported not only at the page boundary but also from inside a page whenever {@code progressIntervalNanos} has
	 * elapsed, so a page that is merely slow keeps saying so instead of going quiet long enough for
	 * {@link JobProperties#staleAfter()} to take the job for abandoned mid-flight.
	 * <p>
	 * A plain paged walk over a fixed id list, not keyset paging: the set cannot change shape mid-walk the way a
	 * label-id-scoped query's result could, since it was already resolved before this runs.
	 * <p>
	 * Also checked at the page boundary, again mirroring {@link ErrandPurgeRunner#walk}: a run merely slow enough for
	 * {@link JobProperties#staleAfter()} to have elapsed has its lease reclaimed by {@code stealStaleLease} (or ended
	 * outright by {@code failStaleJobs}) without anyone telling this thread to stop. Left unchecked, it would carry on
	 * restowing pages nobody is waiting on any more while a second run - the one the reclaimed lease was handed to -
	 * restows the very same errands.
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

			if (isStopped(run)) {
				LOG.info("Label move {} stopped after restowing {} errand(s)", run.jobId(), processed);
				return processed;
			}
		}

		return processed;
	}

	/**
	 * Whether the job has left the state a run works in. A job that is gone counts as stopped too: there is nothing left
	 * to report against, so there is no reason to keep restowing errands on its behalf.
	 */
	private boolean isStopped(final LabelMoveRun run) {
		return !jobService.statusOf(run.jobId())
			.filter(RUNNING::equals)
			.isPresent();
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
