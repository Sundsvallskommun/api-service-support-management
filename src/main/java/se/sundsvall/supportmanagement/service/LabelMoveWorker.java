package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

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
	 * Re-computes the label set of all errands that reference the moved label, using the already
	 * re-parented MetadataLabelEntity tree. Access labels (leaves) are kept as-is; the full
	 * ancestor chain for each leaf is re-derived by walking getParent() on the live entity tree.
	 */
	public void migrateErrandsForMovedLabel(final String movedLabelId) {
		errandsRepository.findAllByLabelsMetadataLabelId(movedLabelId)
			.forEach(this::rebuildLabels);
	}

	void rebuildLabels(final ErrandEntity errand) {
		errand.setLabels(computeNewLabels(errand));
		errandService.persistLabelUpdate(errand);
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

		// Captured before the re-parent, since the descendants still carry this prefix in the database until their own
		// resourcePath is refreshed below.
		final var oldPath = labelToMove.getResourcePath();

		labelToMove.setParent(newParent);
		metadataLabelRepository.saveAndFlush(labelToMove);

		// The moved node's own resourcePath (and version) is refreshed by saveAndFlush above; its descendants are not
		// touched by that call, and are walked and refreshed explicitly here instead of relying on the entity's
		// @PreUpdate cascade, which depends on its lazily-loaded children collection being populated.
		final var descendants = metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(run.namespace(), run.municipalityId(), oldPath + "/");
		descendants.forEach(MetadataLabelEntity::refreshResourcePath);
		metadataLabelRepository.saveAll(descendants);
		metadataLabelRepository.flush();

		final var restowed = restowErrands(run);

		eventService.createLabelMoveEvent(run.municipalityId(), run.labelId(), AUDIT_MESSAGE.formatted(run.labelId(), run.newParentId(), run.startedBy(), restowed));

		jobService.complete(run.jobId(), SUMMARY.formatted(run.labelId(), run.newParentId(), restowed));
	}

	/**
	 * Restows every errand that references the moved label - directly, or through a descendant, since an errand's
	 * stored label set already carries the full ancestor chain and so already contains the moved label's id either way.
	 * <p>
	 * Read a page at a time and persisted a page at a time, each in a transaction of its own - the persist, including the
	 * label rebuild itself, is {@link ErrandService#persistLabelMigrationBatch}'s job, since the page fetched here is
	 * detached by the time that transaction opens and nothing on it beyond an eagerly-fetched collection is safe to
	 * touch outside the session that read it. Plain offset paging is safe here, unlike a walk that removes what it
	 * reaches: restowing an errand never changes whether it still matches the query that found it, so the result set
	 * does not shrink under the walk.
	 */
	private int restowErrands(final LabelMoveRun run) {
		var pageable = PageRequest.of(0, batchSize, Sort.by("id"));
		var processed = 0;

		while (true) {
			final var page = errandsRepository.findByLabelsMetadataLabelId(run.labelId(), pageable);
			if (page.isEmpty()) {
				break;
			}

			errandService.persistLabelMigrationBatch(page.getContent());
			processed += page.getNumberOfElements();
			jobService.updateProgress(run.jobId(), processed);

			if (!page.hasNext()) {
				break;
			}
			pageable = pageable.next();
		}

		return processed;
	}

	private List<ErrandLabelEmbeddable> computeNewLabels(final ErrandEntity errand) {
		var leafIds = errand.getAccessLabels().stream()
			.map(a -> a.getMetadataLabelId())
			.toList();

		var labelEntities = metadataLabelRepository.findAllById(leafIds);
		return buildAncestorChain(labelEntities);
	}

	private static List<ErrandLabelEmbeddable> buildAncestorChain(final List<MetadataLabelEntity> leaves) {
		var seen = new HashSet<String>();
		var result = new ArrayList<ErrandLabelEmbeddable>();

		for (var leaf : leaves) {
			walkAncestors(leaf, seen, result);
		}

		return result;
	}

	private static void walkAncestors(final MetadataLabelEntity start, final HashSet<String> seen, final List<ErrandLabelEmbeddable> result) {
		var current = start;
		var visited = new HashSet<String>();

		while (current != null) {
			var id = current.getId();
			if (id == null || !visited.add(id)) {
				break;
			}
			if (seen.add(id)) {
				result.add(ErrandLabelEmbeddable.create().withMetadataLabelId(id));
			}
			current = current.getParent();
		}
	}
}
