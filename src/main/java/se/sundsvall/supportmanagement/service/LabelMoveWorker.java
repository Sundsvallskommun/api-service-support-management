package se.sundsvall.supportmanagement.service;

import java.util.function.IntConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;

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
public class LabelMoveWorker extends JobRunner<LabelMoveRun> {

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
	private final RestowPager restowPager;

	LabelMoveWorker(
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
		this.restowPager = new RestowPager(LOG, properties.batchSize(), MAX_BATCH_ATTEMPTS);
	}

	@Override
	protected String jobId(final LabelMoveRun run) {
		return run.jobId();
	}

	@Override
	protected void work(final LabelMoveRun run) {
		move(run);
	}

	@Override
	protected void logStarted(final LabelMoveRun run) {
		LOG.info("Label move {} started for label {} to parent {} in namespace {} for municipality {} by {}",
			run.jobId(), sanitizeForLogging(run.labelId()), sanitizeForLogging(run.newParentId()),
			sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), sanitizeForLogging(run.startedBy()));
	}

	@Override
	protected void logAborted(final LabelMoveRun run, final Exception e) {
		LOG.error("Label move {} aborted for label {} in namespace {}", run.jobId(), sanitizeForLogging(run.labelId()), sanitizeForLogging(run.namespace()), e);
	}

	@Override
	protected String abortedMessage(final Exception e) {
		return ABORTED_MESSAGE.formatted(e.getMessage());
	}

	@Override
	protected void logEnded(final LabelMoveRun run) {
		LOG.info("Label move {} ended", run.jobId());
	}

	@Override
	protected String endedWithoutResultMessage() {
		return ENDED_WITHOUT_RESULT;
	}

	private void move(final LabelMoveRun run) {
		final var restowed = moveAndRestow(run.jobId(), run.municipalityId(), run.labelId(), run.newParentId(), null, null, run.startedBy(),
			processed -> jobService.updateProgress(run.jobId(), processed));

		jobService.complete(run.jobId(), SUMMARY.formatted(run.labelId(), run.newParentId(), restowed));
	}

	/**
	 * Reparents {@code labelId} under {@code newParentId}, optionally also setting a new {@code resourceName} and/or
	 * {@code displayName} at the same time (a combined move+rename, as a label-tree restructure's MOVE step allows), and
	 * restows every affected errand, reporting cumulative progress through {@code progressReporter} as it goes.
	 * <p>
	 * Extracted out of {@link #move(LabelMoveRun)} so that {@code LabelTreeRestructureWorker} can carry out one MOVE
	 * step of a larger restructure directly - on its own worker thread, not dispatched through the executor again -
	 * reporting progress against its own composite job instead of a per-move job, and without this method itself
	 * touching {@link JobService#complete}/{@code fail}, which only the caller that owns the job's lifecycle may do.
	 * {@code jobId} is taken separately from that caller's own job rather than read off a {@link LabelMoveRun} - purely
	 * for log correlation in {@link #fetchAndPersistPage}, so a restructure's MOVE step logs against the restructure's
	 * own job rather than a move job that, called this way, never exists.
	 *
	 * @param  newResourceName optional new resourceName to set in the same update, or {@code null} to keep it.
	 * @param  newDisplayName  optional new displayName to set in the same update, or {@code null} to keep it.
	 * @return                 number of errands restowed.
	 */
	int moveAndRestow(final String jobId, final String municipalityId, final String labelId, final String newParentId, final String newResourceName, final String newDisplayName,
		final String startedBy, final IntConsumer progressReporter) {
		final var labelToMove = metadataLabelRepository.findById(labelId)
			.orElseThrow(() -> new IllegalStateException(LABEL_GONE.formatted(labelId)));
		final var newParent = newParentId != null
			? metadataLabelRepository.findById(newParentId).orElseThrow(() -> new IllegalStateException(NEW_PARENT_GONE.formatted(newParentId)))
			: null;

		labelToMove.setParent(newParent);
		if (newResourceName != null) {
			labelToMove.setResourceName(newResourceName);
		}
		if (newDisplayName != null) {
			labelToMove.setDisplayName(newDisplayName);
		}
		metadataLabelRepository.saveAndFlush(labelToMove);
		// The @PreUpdate cascade on labelToMove (onUpdate -> updateChildrenPathsRecursively) recomputes resourcePath for
		// the moved node and, recursively, for every descendant reachable through its metadataLabels collection, all
		// within this single saveAndFlush's own session - the whole subtree is already correct and persisted by the
		// time this call returns. A second pass re-querying by the old resourcePath prefix would find nothing (the
		// cascade above already moved every descendant off it), and refreshing whatever it did find by walking a lazy
		// getParent() chain would run on entities already detached from that query's own, separate transaction.

		final var restowed = restowErrands(jobId, labelId, progressReporter);

		eventService.createLabelMoveEvent(municipalityId, labelId, startedBy, AUDIT_MESSAGE.formatted(labelId, newParentId, startedBy, restowed));

		return restowed;
	}

	/**
	 * Restows every errand that references the moved label - directly, or through a descendant, since an errand's
	 * stored label set already carries the full ancestor chain and so already contains the moved label's id either way.
	 * <p>
	 * Delegates the actual paged walk to {@link RestowPager}, shared with {@link LabelMergeWorker}: read a page at a
	 * time and persisted a page at a time, each in a transaction of its own - the persist, including the label rebuild
	 * itself, is {@link ErrandService#persistLabelMigrationBatch}'s job, since the page fetched here is detached by the
	 * time that transaction opens and nothing on it beyond an eagerly-fetched collection is safe to touch outside the
	 * session that read it. Paged by keyset (id > lastSeenId), not offset: an errand created, purged, or relabelled
	 * while this walk is under way would otherwise shift where a later page starts, and an errand landing on that
	 * boundary would be skipped and keep its stale ancestor chain.
	 */
	private int restowErrands(final String jobId, final String labelId, final IntConsumer progressReporter) {
		return restowPager.restow(
			(lastSeenId, pageable) -> errandsRepository.findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(labelId, lastSeenId, pageable),
			errandService::persistLabelMigrationBatch,
			attempt -> "Label move %s retrying a page for label %s after a concurrent edit lost the optimistic-lock race (attempt %d/%d)"
				.formatted(jobId, sanitizeForLogging(labelId), attempt, MAX_BATCH_ATTEMPTS),
			progressReporter);
	}
}
