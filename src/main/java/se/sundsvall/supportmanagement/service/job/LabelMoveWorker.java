package se.sundsvall.supportmanagement.service.job;

import java.util.function.IntConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Internal helper {@link LabelTreeRestructureWorker} calls directly to carry out one MOVE step of a larger
 * restructure, on its own worker thread, reporting progress against that caller's own composite job rather than one
 * of this class's own - the public {@code /move} endpoint's own job is carried out by {@link LabelMoveRunner}
 * instead, which this class predates. Kept around specifically because {@link #moveAndRestow} supports what that
 * endpoint's own runner does not: a combined move+rename in one step, and progress reported through a caller-supplied
 * {@link IntConsumer} rather than always against this class's own job.
 * <p>
 * A move re-parents one label, refreshes {@code resourcePath} for its whole subtree, and then walks every errand that
 * references the moved label (or any of its descendants — captured for free since an errand's stored label set already
 * carries the full ancestor chain) restowing its label set page by page.
 */
@Component
public class LabelMoveWorker {

	private static final Logger LOG = LoggerFactory.getLogger(LabelMoveWorker.class);

	private static final String AUDIT_MESSAGE = "Label %s moved under %s by %s, %d errand(s) restowed";
	private static final String LABEL_GONE = "Label %s no longer exists";
	private static final String NEW_PARENT_GONE = "New parent %s no longer exists";
	private static final int MAX_BATCH_ATTEMPTS = 3;

	private final ErrandsRepository errandsRepository;
	private final MetadataLabelRepository metadataLabelRepository;
	private final ErrandService errandService;
	private final EventService eventService;
	private final RestowPager restowPager;

	LabelMoveWorker(
		final ErrandsRepository errandsRepository,
		final MetadataLabelRepository metadataLabelRepository,
		final ErrandService errandService,
		final EventService eventService,
		final LabelMoveProperties properties) {
		this.errandsRepository = errandsRepository;
		this.metadataLabelRepository = metadataLabelRepository;
		this.errandService = errandService;
		this.eventService = eventService;
		this.restowPager = new RestowPager(LOG, properties.batchSize(), MAX_BATCH_ATTEMPTS);
	}

	/**
	 * Reparents {@code labelId} under {@code newParentId}, optionally also setting a new {@code resourceName} and/or
	 * {@code displayName} at the same time (a combined move+rename, as a label-tree restructure's MOVE step allows), and
	 * restows every affected errand, reporting cumulative progress through {@code progressReporter} as it goes.
	 * <p>
	 * Called directly by {@code LabelTreeRestructureWorker} so it can carry out one MOVE step of a larger restructure
	 * on its own worker thread, reporting progress against its own composite job instead of a per-move job, and
	 * without this method itself touching {@code JobService#complete}/{@code fail}, which only the caller that owns
	 * the job's lifecycle may do. {@code jobId} is taken separately from that caller's own job rather than carried on
	 * a run of this class's own - purely for log correlation in {@link #restowErrands}, so a restructure's MOVE step
	 * logs against the restructure's own job rather than a move job that, called this way, never exists.
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
