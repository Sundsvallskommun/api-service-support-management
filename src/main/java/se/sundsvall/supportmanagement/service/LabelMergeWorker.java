package se.sundsvall.supportmanagement.service;

import java.util.Set;
import java.util.function.IntConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Carries out label merges accepted by {@link MetadataService#startLabelMerge}.
 * <p>
 * A merge walks every errand that references any of the source labels, substitutes the destination label id for
 * whichever source id each errand carried, and once nothing references a source label any more, deletes it. Mirrors
 * {@link LabelMoveWorker} in shape - paged, keyset-walked restowing with optimistic-lock retry, one transaction per
 * page - but there is no tree re-parenting step: the destination label already sits where it is going to stay, and
 * what changes is which errands point at it. Never throws: whatever goes wrong ends the job as failed, since the
 * thread this runs on has nobody to report to.
 */
@Component
public class LabelMergeWorker {

	private static final Logger LOG = LoggerFactory.getLogger(LabelMergeWorker.class);

	private static final String ABORTED_MESSAGE = "Label merge aborted: %s";
	private static final String ENDED_WITHOUT_RESULT = "Label merge ended without reaching a result of its own";
	private static final String SUMMARY = "Labels %s merged into %s, %d errand(s) restowed";
	private static final String AUDIT_MESSAGE = "Labels %s merged into %s by %s, %d errand(s) restowed";
	private static final String LABEL_GONE = "Label %s no longer exists";
	private static final int MAX_BATCH_ATTEMPTS = 3;

	private final ErrandsRepository errandsRepository;
	private final MetadataLabelRepository metadataLabelRepository;
	private final ErrandService errandService;
	private final JobService jobService;
	private final EventService eventService;
	private final RestowPager restowPager;

	LabelMergeWorker(
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
		this.restowPager = new RestowPager(LOG, properties.batchSize(), MAX_BATCH_ATTEMPTS);
	}

	/**
	 * Runs a label merge to its end.
	 *
	 * @param run the run to carry out.
	 */
	public void run(final LabelMergeRun run) {
		LOG.info("Label merge {} started for labels {} into {} in namespace {} for municipality {} by {}",
			run.jobId(), sanitizeForLogging(run.sourceLabelIds().toString()), sanitizeForLogging(run.targetLabelId()),
			sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), sanitizeForLogging(run.startedBy()));

		var ended = false;

		try {
			jobService.setRunning(run.jobId());
			merge(run);
			ended = true;
		} catch (final Exception e) {
			LOG.error("Label merge {} aborted for labels {} into {} in namespace {}", run.jobId(), sanitizeForLogging(run.sourceLabelIds().toString()),
				sanitizeForLogging(run.targetLabelId()), sanitizeForLogging(run.namespace()), e);
			jobService.fail(run.jobId(), ABORTED_MESSAGE.formatted(e.getMessage()));
			ended = true;
		} finally {
			// A thread taken down by something that is not an exception - an Error - would otherwise leave the job reading
			// as running for as long as it lives.
			if (!ended) {
				jobService.fail(run.jobId(), ENDED_WITHOUT_RESULT);
			}
		}

		LOG.info("Label merge {} ended", run.jobId());
	}

	private void merge(final LabelMergeRun run) {
		final var restowed = mergeAndRestow(run.jobId(), run.municipalityId(), run.targetLabelId(), run.sourceLabelIds(), run.startedBy(),
			processed -> jobService.updateProgress(run.jobId(), processed));

		jobService.complete(run.jobId(), SUMMARY.formatted(run.sourceLabelIds(), run.targetLabelId(), restowed));
	}

	/**
	 * Restows every errand referencing any of {@code sourceLabelIds} onto {@code targetLabelId}, then deletes the now
	 * unreferenced source labels, reporting cumulative progress through {@code progressReporter} as it goes.
	 * <p>
	 * Extracted out of {@link #merge(LabelMergeRun)} so that {@code LabelTreeRestructureWorker} can carry out one MERGE
	 * step of a larger restructure directly, on its own worker thread, reporting progress against its own composite job
	 * instead of a per-merge job - mirrors {@link LabelMoveWorker#moveAndRestow}, including taking {@code jobId}
	 * separately from that caller's own job, purely for log correlation in {@link RestowPager}.
	 *
	 * @return number of errands restowed.
	 */
	int mergeAndRestow(final String jobId, final String municipalityId, final String targetLabelId, final Set<String> sourceLabelIds, final String startedBy,
		final IntConsumer progressReporter) {
		if (!metadataLabelRepository.existsById(targetLabelId)) {
			throw new IllegalStateException(LABEL_GONE.formatted(targetLabelId));
		}
		sourceLabelIds.forEach(sourceId -> {
			if (!metadataLabelRepository.existsById(sourceId)) {
				throw new IllegalStateException(LABEL_GONE.formatted(sourceId));
			}
		});

		final var restowed = restowErrands(jobId, targetLabelId, sourceLabelIds, progressReporter);

		// Only reached once every errand that referenced a source label has been restowed onto the destination - no
		// source label is referenced by an errand any more by the time this deletes them.
		metadataLabelRepository.deleteAllById(sourceLabelIds);
		metadataLabelRepository.flush();

		eventService.createLabelMergeEvent(municipalityId, targetLabelId, startedBy,
			AUDIT_MESSAGE.formatted(sourceLabelIds, targetLabelId, startedBy, restowed));

		return restowed;
	}

	/**
	 * Restows every errand that references any of the source labels - directly, or through a descendant, since an
	 * errand's stored label set already carries the full ancestor chain. Delegates the actual paged walk to
	 * {@link RestowPager}, shared with {@link LabelMoveWorker}: a page at a time, read and persisted each in a
	 * transaction of its own.
	 */
	private int restowErrands(final String jobId, final String targetLabelId, final Set<String> sourceLabelIds, final IntConsumer progressReporter) {
		return restowPager.restow(
			(lastSeenId, pageable) -> errandsRepository.findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(sourceLabelIds, lastSeenId, pageable),
			page -> errandService.persistLabelMergeBatch(page, sourceLabelIds, targetLabelId),
			attempt -> "Label merge %s retrying a page for target %s after a concurrent edit lost the optimistic-lock race (attempt %d/%d)"
				.formatted(jobId, sanitizeForLogging(targetLabelId), attempt, MAX_BATCH_ATTEMPTS),
			progressReporter);
	}
}
