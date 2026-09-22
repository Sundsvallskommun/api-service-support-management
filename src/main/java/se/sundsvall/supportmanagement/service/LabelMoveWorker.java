package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
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
	 * Restows every errand that references the moved label - directly, or through a descendant, since an errand's
	 * stored label set already carries the full ancestor chain and so already contains the moved label's id either way.
	 * <p>
	 * Read a page at a time and persisted a page at a time, each in a transaction of its own - the persist, including the
	 * label rebuild itself, is {@link ErrandService#persistLabelMigrationBatch}'s job, since the page fetched here is
	 * detached by the time that transaction opens and nothing on it beyond an eagerly-fetched collection is safe to
	 * touch outside the session that read it.
	 * <p>
	 * Paged by keyset (id > lastSeenId), not offset: an errand created, purged, or relabelled while this walk is under
	 * way would otherwise shift where a later page starts, and an errand landing on that boundary would be skipped and
	 * keep its stale ancestor chain. A page shorter than the requested size ends the walk, since keyset paging has no
	 * separate "has next" signal to ask for.
	 */
	private int restowErrands(final LabelMoveRun run) {
		var lastSeenId = "";
		var processed = 0;
		var page = fetchAndPersistPage(run, lastSeenId);

		while (!page.isEmpty()) {
			processed += page.size();
			jobService.updateProgress(run.jobId(), processed);
			lastSeenId = page.get(page.size() - 1).getId();

			// A page shorter than requested is necessarily the last one - skip the round-trip that would only confirm it.
			page = page.size() < batchSize ? List.of() : fetchAndPersistPage(run, lastSeenId);
		}

		return processed;
	}

	/**
	 * Reads one page and hands it to {@link ErrandService#persistLabelMigrationBatch}, retrying against a fresh read
	 * when a concurrent edit - a user PATCHing one of these errands between the read and the merge, which {@code
	 * ErrandEntity}'s {@code @Version} turns into a lock conflict rather than a silently lost update - loses the
	 * optimistic-lock race. A stale, already-detached page would just fail the same way again, so each attempt re-reads
	 * rather than retrying the same instances; an errand a concurrent edit has since unlabelled naturally drops out of
	 * the requery instead of being retried at all.
	 */
	private List<ErrandEntity> fetchAndPersistPage(final LabelMoveRun run, final String lastSeenId) {
		final var pageable = PageRequest.ofSize(batchSize);
		var attempt = 0;

		while (true) {
			attempt++;
			final var page = errandsRepository.findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(run.labelId(), lastSeenId, pageable);
			if (page.isEmpty()) {
				return page;
			}

			try {
				errandService.persistLabelMigrationBatch(page);
				return page;
			} catch (final ObjectOptimisticLockingFailureException e) {
				if (attempt == MAX_BATCH_ATTEMPTS) {
					throw e;
				}
				LOG.warn("Label move {} retrying a page after a concurrent edit lost the optimistic-lock race (attempt {}/{})",
					run.jobId(), attempt, MAX_BATCH_ATTEMPTS);
			}
		}
	}
}
