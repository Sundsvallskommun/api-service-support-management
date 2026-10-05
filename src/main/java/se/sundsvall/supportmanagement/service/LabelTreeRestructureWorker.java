package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Carries out label-tree restructures accepted by {@link MetadataService#startLabelTreeRestructure}.
 * <p>
 * Applies the run's steps strictly in order, directly against the database as each is reached - so a step referencing
 * a label an earlier {@code ADD} step in the same run just created resolves it by the real id that step's own
 * {@code save} produced, exactly as it would if a caller made the equivalent sequence of separate API calls by hand.
 * MOVE/MERGE steps delegate their actual reparent-and-restow / merge-and-restow work to
 * {@link LabelMoveWorker#moveAndRestow}/{@link LabelMergeWorker#mergeAndRestow}, reporting progress against this
 * run's own job rather than one of their own.
 * <p>
 * Every step was already validated once, synchronously, by {@link MetadataService#startLabelTreeRestructure} before
 * this run was even dispatched - a failure reaching here means the tree changed between that check and this run
 * (a concurrent edit, or an operator error resolved a database issue between the two), and per this feature's design,
 * is treated the same way {@link LabelMoveWorker}/{@link LabelMergeWorker} treat any other run-time failure: the run
 * stops where it is, whatever earlier steps already committed is left as applied, and the job is failed with a
 * message naming which step failed - never rolled back, since restow batches already committed page by page cannot
 * be undone by a caller downstream of them anyway. Someone reviewing the tree can safely resubmit a request
 * containing only the remaining steps: ADD is idempotent (skips a path already present), and every other step type
 * simply fails fast again if its own precondition still doesn't hold.
 */
@Component
public class LabelTreeRestructureWorker extends JobRunner<LabelRestructureRun> {

	private static final Logger LOG = LoggerFactory.getLogger(LabelTreeRestructureWorker.class);

	private static final String ABORTED_MESSAGE = "Label tree restructure aborted: %s";
	private static final String ENDED_WITHOUT_RESULT = "Label tree restructure ended without reaching a result of its own";
	private static final String STEP_FAILED = "Step %d (%s) failed: %s";
	private static final String SUMMARY = "%d step(s) applied, %d errand(s) restowed";
	private static final String LABEL_GONE = "Label at path '%s' no longer exists";
	private static final String PARENT_GONE = "Parent at path '%s' no longer exists";
	private static final String HAS_CHILDREN = "Label at path '%s' still has children";
	private static final String REFERENCED_BY_ERRANDS = "Label at path '%s' is referenced by one or more errands";

	private final MetadataLabelRepository metadataLabelRepository;
	private final ErrandsRepository errandsRepository;
	private final LabelMoveWorker labelMoveWorker;
	private final LabelMergeWorker labelMergeWorker;
	private final JobService jobService;
	private final TransactionTemplate transactionTemplate;

	LabelTreeRestructureWorker(
		final MetadataLabelRepository metadataLabelRepository,
		final ErrandsRepository errandsRepository,
		final LabelMoveWorker labelMoveWorker,
		final LabelMergeWorker labelMergeWorker,
		final JobService jobService,
		final PlatformTransactionManager transactionManager) {
		super(jobService);
		this.metadataLabelRepository = metadataLabelRepository;
		this.errandsRepository = errandsRepository;
		this.labelMoveWorker = labelMoveWorker;
		this.labelMergeWorker = labelMergeWorker;
		this.jobService = jobService;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Override
	protected String jobId(final LabelRestructureRun run) {
		return run.jobId();
	}

	@Override
	protected void work(final LabelRestructureRun run) {
		restructure(run);
	}

	@Override
	protected void logStarted(final LabelRestructureRun run) {
		LOG.info("Label tree restructure {} started with {} step(s) in namespace {} for municipality {} by {}",
			run.jobId(), run.steps().size(), sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), sanitizeForLogging(run.startedBy()));
	}

	@Override
	protected void logAborted(final LabelRestructureRun run, final Exception e) {
		LOG.error("Label tree restructure {} aborted in namespace {}", run.jobId(), sanitizeForLogging(run.namespace()), e);
	}

	@Override
	protected String abortedMessage(final Exception e) {
		return ABORTED_MESSAGE.formatted(e.getMessage());
	}

	@Override
	protected void logEnded(final LabelRestructureRun run) {
		LOG.info("Label tree restructure {} ended", run.jobId());
	}

	@Override
	protected String endedWithoutResultMessage() {
		return ENDED_WITHOUT_RESULT;
	}

	private void restructure(final LabelRestructureRun run) {
		// A single-element holder rather than a local var, so applyStep's per-step progress-reporter lambdas can add to
		// it without needing restructure's own stack frame to still be the one calling jobService.updateProgress.
		final var totalRestowed = new int[] {
			0
		};

		final var steps = run.steps();
		for (var index = 0; index < steps.size(); index++) {
			final var step = steps.get(index);
			try {
				applyStep(run, step, totalRestowed);
			} catch (final Exception e) {
				throw new IllegalStateException(STEP_FAILED.formatted(index, step.getType(), e.getMessage()), e);
			}
		}

		jobService.complete(run.jobId(), SUMMARY.formatted(steps.size(), totalRestowed[0]));
	}

	private void applyStep(final LabelRestructureRun run, final LabelRestructureStep step, final int[] totalRestowed) {
		final var namespace = run.namespace();
		final var municipalityId = run.municipalityId();

		switch (step.getType()) {
			case ADD -> applyAdd(namespace, municipalityId, step);
			case RENAME -> applyRename(namespace, municipalityId, step);
			case DELETE -> applyDelete(namespace, municipalityId, step);
			case MOVE -> {
				final var before = totalRestowed[0];
				final var restowed = applyMove(run.jobId(), namespace, municipalityId, step, run.startedBy(), processed -> jobService.updateProgress(run.jobId(), before + processed));
				totalRestowed[0] = before + restowed;
			}
			case MERGE -> {
				final var before = totalRestowed[0];
				final var restowed = applyMerge(run.jobId(), namespace, municipalityId, step, run.startedBy(), processed -> jobService.updateProgress(run.jobId(), before + processed));
				totalRestowed[0] = before + restowed;
			}
		}
	}

	/**
	 * A no-op if the path already exists - safe to resubmit a request whose earlier attempt already added this label,
	 * mirroring {@code MetadataService.simulateAdd}'s dry-run treatment of the same case.
	 * <p>
	 * The parent lookup and the save are kept in one transaction of their own, rather than each running in the separate,
	 * short-lived transaction a plain repository call opens on its own: {@code MetadataLabelEntity}'s {@code @PrePersist}
	 * walks the new label's whole ancestor chain to compute {@code resourcePath}, following {@code parent.getParent()}
	 * as many levels up as the tree goes. A parent fetched in a transaction of its own would be detached by the time
	 * that walk reaches it, and its own {@code parent} field - lazy regardless of how the parent itself was queried -
	 * would have no session left to resolve against past the first hop.
	 */
	private void applyAdd(final String namespace, final String municipalityId, final LabelRestructureStep step) {
		final var path = LabelTreeSnapshot.join(step.getPath());
		final var parentPath = LabelTreeSnapshot.join(LabelTreeSnapshot.allButLast(step.getPath()));

		transactionTemplate.executeWithoutResult(status -> {
			if (metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(namespace, municipalityId, path).isPresent()) {
				return;
			}

			final var parent = parentPath.isEmpty() ? null : findOrThrow(namespace, municipalityId, parentPath, PARENT_GONE);

			metadataLabelRepository.save(MetadataLabelEntity.create()
				.withNamespace(namespace)
				.withMunicipalityId(municipalityId)
				.withClassification(step.getClassification())
				.withDisplayName(step.getDisplayName())
				.withResourceName(LabelTreeSnapshot.lastSegment(step.getPath()))
				.withParent(parent));
		});
	}

	private void applyRename(final String namespace, final String municipalityId, final LabelRestructureStep step) {
		final var path = LabelTreeSnapshot.join(step.getPath());
		final var entity = findOrThrow(namespace, municipalityId, path, LABEL_GONE);
		entity.setDisplayName(step.getDisplayName());
		metadataLabelRepository.save(entity);
	}

	private void applyDelete(final String namespace, final String municipalityId, final LabelRestructureStep step) {
		final var path = LabelTreeSnapshot.join(step.getPath());
		final var entity = findOrThrow(namespace, municipalityId, path, LABEL_GONE);

		if (!metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(namespace, municipalityId, path + "/").isEmpty()) {
			throw new IllegalStateException(HAS_CHILDREN.formatted(path));
		}
		if (errandsRepository.existsByLabelsMetadataLabelIdIn(Set.of(entity.getId()))) {
			throw new IllegalStateException(REFERENCED_BY_ERRANDS.formatted(path));
		}

		metadataLabelRepository.deleteById(entity.getId());
	}

	private int applyMove(final String jobId, final String namespace, final String municipalityId, final LabelRestructureStep step, final String startedBy, final IntConsumer progressReporter) {
		final var sourceId = findOrThrow(namespace, municipalityId, LabelTreeSnapshot.join(step.getPath()), LABEL_GONE).getId();

		final var destinationSegments = ofNullable(step.getDestinationParentPath()).orElse(List.of());
		final var destinationParentId = destinationSegments.isEmpty()
			? null
			: findOrThrow(namespace, municipalityId, LabelTreeSnapshot.join(destinationSegments), PARENT_GONE).getId();

		return labelMoveWorker.moveAndRestow(jobId, municipalityId, sourceId, destinationParentId, step.getNewResourceName(), step.getDisplayName(), startedBy, progressReporter);
	}

	private int applyMerge(final String jobId, final String namespace, final String municipalityId, final LabelRestructureStep step, final String startedBy, final IntConsumer progressReporter) {
		final var targetId = findOrThrow(namespace, municipalityId, LabelTreeSnapshot.join(step.getPath()), LABEL_GONE).getId();

		final var sourceIds = step.getSourcePaths().stream()
			.map(sourcePath -> findOrThrow(namespace, municipalityId, LabelTreeSnapshot.join(sourcePath), LABEL_GONE).getId())
			.collect(toSet());

		return labelMergeWorker.mergeAndRestow(jobId, namespace, municipalityId, targetId, sourceIds, startedBy, progressReporter);
	}

	private MetadataLabelEntity findOrThrow(final String namespace, final String municipalityId, final String path, final String messageTemplate) {
		return metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(namespace, municipalityId, path)
			.orElseThrow(() -> new IllegalStateException(messageTemplate.formatted(path)));
	}
}
