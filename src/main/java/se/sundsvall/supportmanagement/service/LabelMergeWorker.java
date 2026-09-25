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
	private final int batchSize;

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
		this.batchSize = properties.batchSize();
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
		if (!metadataLabelRepository.existsById(run.targetLabelId())) {
			throw new IllegalStateException(LABEL_GONE.formatted(run.targetLabelId()));
		}
		run.sourceLabelIds().forEach(sourceId -> {
			if (!metadataLabelRepository.existsById(sourceId)) {
				throw new IllegalStateException(LABEL_GONE.formatted(sourceId));
			}
		});

		final var restowed = restowErrands(run);

		// Only reached once every errand that referenced a source label has been restowed onto the destination - no
		// source label is referenced by an errand any more by the time this deletes them.
		metadataLabelRepository.deleteAllById(run.sourceLabelIds());
		metadataLabelRepository.flush();

		eventService.createLabelMergeEvent(run.municipalityId(), run.targetLabelId(), run.startedBy(),
			AUDIT_MESSAGE.formatted(run.sourceLabelIds(), run.targetLabelId(), run.startedBy(), restowed));

		jobService.complete(run.jobId(), SUMMARY.formatted(run.sourceLabelIds(), run.targetLabelId(), restowed));
	}

	/**
	 * Restows every errand that references any of the source labels - directly, or through a descendant, since an
	 * errand's stored label set already carries the full ancestor chain. Read a page at a time and persisted a page at
	 * a time, each in a transaction of its own, exactly as {@link LabelMoveWorker#restowErrands} does and for the same
	 * reasons.
	 */
	private int restowErrands(final LabelMergeRun run) {
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
	 * Reads one page and hands it to {@link ErrandService#persistLabelMergeBatch}, retrying against a fresh read when a
	 * concurrent edit loses the optimistic-lock race - mirrors {@link LabelMoveWorker#fetchAndPersistPage}.
	 */
	private List<ErrandEntity> fetchAndPersistPage(final LabelMergeRun run, final String lastSeenId) {
		final var pageable = PageRequest.ofSize(batchSize);
		var attempt = 0;

		while (true) {
			attempt++;
			final var page = errandsRepository.findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(run.sourceLabelIds(), lastSeenId, pageable);
			if (page.isEmpty()) {
				return page;
			}

			try {
				errandService.persistLabelMergeBatch(page, run.sourceLabelIds(), run.targetLabelId());
				return page;
			} catch (final ObjectOptimisticLockingFailureException e) {
				if (attempt == MAX_BATCH_ATTEMPTS) {
					throw e;
				}
				LOG.warn("Label merge {} retrying a page after a concurrent edit lost the optimistic-lock race (attempt {}/{})",
					run.jobId(), attempt, MAX_BATCH_ATTEMPTS);
			}
		}
	}
}
