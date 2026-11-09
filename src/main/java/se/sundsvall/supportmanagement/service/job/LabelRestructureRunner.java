package se.sundsvall.supportmanagement.service.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;
import se.sundsvall.supportmanagement.config.LabelRestructureProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.LabelTreeSnapshot;
import se.sundsvall.supportmanagement.service.MetadataService;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.model.enums.JobStatus.RUNNING;

/**
 * Carries out label-tree restructures accepted by {@link MetadataService#startLabelTreeRestructure} - the single
 * runner behind every label-tree mutation now that the standalone {@code /move} and {@code /merge} endpoints are
 * gone. A plain move or merge is simply a one-step restructure; this class makes no distinction between the two and
 * an arbitrarily larger one.
 * <p>
 * Applies the run's steps strictly in order, directly against the database as each is reached - so a step
 * referencing a label an earlier {@code ADD} step in the same run just created resolves it by the real id that
 * step's own {@code save} produced, exactly as it would if a caller made the equivalent sequence of separate API
 * calls by hand.
 * <p>
 * Carried out as a single transaction: every step's mutation, the one restow pass over the union of errands any
 * MOVE/MERGE step affects, and the deletion of any label a MERGE step folds away, or none of it. An errand touched
 * by more than one MOVE/MERGE step is restowed once, not once per step: every affected errand id was already
 * resolved as one deduplicated set by {@link MetadataService#startLabelTreeRestructure} before this run was
 * dispatched, and a {@code sourceId -> targetId} map accumulated from every MERGE step (resolved through its full
 * chain by {@link ErrandService#persistLabelRestructureMigration}, so "merge A into B, then merge B into C" still
 * substitutes A straight to C) is applied to each affected errand's leaf labels before its ancestor chain is
 * re-derived - restowing once, after every step has already mutated the tree, rather than once per step.
 * <p>
 * Every step was already validated once, synchronously, by {@link MetadataService#startLabelTreeRestructure} before
 * this run was dispatched; each step's own mutation re-validates once more anyway, immediately before writing,
 * against the database rather than the request-time snapshot - the same execution-time re-check a plain move always
 * ran, now run uniformly for every step type (see {@link MetadataService#revalidateAndReparent}/
 * {@link MetadataService#mergeInto}/{@link MetadataService#addLabel}/{@link MetadataService#renameLabel}/
 * {@link MetadataService#deleteLabel}). A failure there - a concurrent edit landing between validation and this run,
 * a lock-acquisition timeout, a stop request - rolls the whole transaction back: nothing committed by an earlier
 * step in the same run survives either, and a retry is a fresh attempt, never a resume.
 * <p>
 * Never throws past {@link #run}: whatever goes wrong ends the job as failed, since the thread this runs on has
 * nobody else to report to.
 */
@Component
public class LabelRestructureRunner extends JobRunner<LabelRestructureRun> {

	private static final Logger LOG = LoggerFactory.getLogger(LabelRestructureRunner.class);

	private static final String SUMMARY = "%d step(s) applied, %s";
	private static final String ONLY_RESTOWED = "%d errand(s) restowed";
	private static final String RESTOWED_AND_UNCHANGED = "%d errand(s) restowed, %d kept their labels";
	private static final String MERGE_KEPT_SOURCES = "labels %s were merged away but errands still wear them";
	private static final String MOVE_AUDIT_MESSAGE = "Label %s moved under %s by %s";
	private static final String MERGE_AUDIT_MESSAGE = "Labels %s merged into %s by %s";
	private static final String ROLLED_BACK_MESSAGE = "Rolled back - no changes applied. Reached %d of %d errand(s) before: %s";
	private static final String LOCK_TIMEOUT_REASON = "could not acquire locks - one or more errands are being edited; retry when the namespace is idle";
	private static final String STOPPED_REASON = "stopped by request";
	private static final String STEP_FAILED_REASON = "step %d (%s) failed: %s";
	private static final String ENDED_WITHOUT_RESULT = "Label tree restructure ended without reaching a result of its own";
	private static final String LABEL_GONE = "Label at path '%s' no longer exists";
	private static final String PARENT_GONE = "Parent at path '%s' no longer exists";

	private final MetadataLabelRepository metadataLabelRepository;
	private final ErrandsRepository errandsRepository;
	private final ErrandService errandService;
	private final MetadataService metadataService;
	private final JobService jobService;
	private final EventService eventService;
	private final LabelRestructureRunner self;
	private final int batchSize;
	private final long progressIntervalNanos;

	/**
	 * {@code self}: reached through this instance's own Spring proxy rather than through {@code this}, so that
	 * {@link #executeRestructure}'s {@code @Transactional} actually takes effect - mirrors the former
	 * {@code LabelMoveRunner}'s own {@code self} field (see its own javadoc for why). {@code @Lazy} because the proxy
	 * this needs is this very bean's own, not yet constructed while its own constructor runs.
	 */
	LabelRestructureRunner(
		final MetadataLabelRepository metadataLabelRepository,
		final ErrandsRepository errandsRepository,
		final ErrandService errandService,
		final MetadataService metadataService,
		final JobService jobService,
		final EventService eventService,
		final LabelRestructureProperties properties,
		@Lazy final LabelRestructureRunner self) {
		super(jobService);
		this.metadataLabelRepository = metadataLabelRepository;
		this.errandsRepository = errandsRepository;
		this.errandService = errandService;
		this.metadataService = metadataService;
		this.jobService = jobService;
		this.eventService = eventService;
		this.batchSize = properties.batchSize();
		this.progressIntervalNanos = properties.progressInterval().toNanos();
		this.self = self;
	}

	@Override
	protected String jobId(final LabelRestructureRun run) {
		return run.jobId();
	}

	@Override
	protected void work(final LabelRestructureRun run) {
		LOG.info("Label tree restructure {} started with {} step(s) in namespace {} for municipality {} by {}",
			run.jobId(), run.steps().size(), sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), sanitizeForLogging(displayValue(run.startedBy())));

		restructure(run);

		LOG.info("Label tree restructure {} ended", run.jobId());
	}

	@Override
	protected String reportAborted(final LabelRestructureRun run, final Exception e) {
		// A user-requested stop is not a failure - it is logged at its own level rather than as an error.
		if (e instanceof final LabelRestructureAbortedException aborted && aborted.getCause() instanceof StoppedException) {
			LOG.info("Label tree restructure {} stopped in namespace {}: {}", run.jobId(), sanitizeForLogging(run.namespace()), e.getMessage());
		} else {
			LOG.error("Label tree restructure {} aborted in namespace {}", run.jobId(), sanitizeForLogging(run.namespace()), e);
		}
		// Already fully formed at the throw site inside executeRestructure, where processed/total and the failure
		// reason (including which step failed) are in scope - nothing left to add here.
		return e.getMessage();
	}

	@Override
	protected String endedWithoutResultMessage() {
		return ENDED_WITHOUT_RESULT;
	}

	/**
	 * Carries the restructure to its end.
	 * <p>
	 * Audit events and {@link JobService#complete} are deliberately called from here, once
	 * {@code self.executeRestructure} has already returned, rather than from inside it - mirrors the former
	 * {@code LabelMoveRunner.move}: a {@code @Transactional} method's transaction commits as the call returns
	 * through its own proxy, so anything called from inside the method body itself would still run before that
	 * commit has actually happened. Calling them here instead means neither ever reports success before the
	 * mutations it is reporting on are durably committed.
	 */
	private void restructure(final LabelRestructureRun run) {
		final var outcome = self.executeRestructure(run);

		outcome.auditEntries().forEach(entry -> entry.emit(eventService, run.municipalityId(), run.startedBy()));

		jobService.complete(run.jobId(), SUMMARY.formatted(run.steps().size(), outcome.describe()));
	}

	/**
	 * Applies every step and restows every affected errand in one transaction - see the class doc for why. Public
	 * rather than package-private only so that {@link #self} reaches this through the proxy {@code @Transactional}
	 * lives on; nothing outside this class calls it.
	 * <p>
	 * Every affected errand is locked - {@link ErrandsRepository#findAllByIdForUpdate}, ordered by id - immediately
	 * before it is restowed, a chunk ({@link LabelRestructureProperties#batchSize()}) at a time, in the same
	 * ascending order the frozen id list itself is sorted into once, up front. Progress is written as the walk
	 * goes - at the existing time-based heartbeat and at every chunk boundary - through
	 * {@link JobService#updateProgress}, which commits in a transaction of its own ({@code REQUIRES_NEW}) and so
	 * survives this transaction's own eventual commit or rollback either way.
	 *
	 * An errand whose rebuilt labels a guard refuses keeps the labels it had and is counted apart from those restowed.
	 * A source label a MERGE step folded away that an errand still wears afterward - kept on it by a guard, or left on
	 * an errand that has no access labels to rebuild its labels from - stops the run, as the steps were applied against
	 * a tree in which the sources were gone.
	 *
	 * @return how many errands were restowed and how many kept their labels, and the audit events to emit once this has
	 *         committed - always the full affected set on success, since any failure partway through rolls the whole
	 *         transaction back rather than leaving a partial result.
	 */
	@Transactional
	public RestructureOutcome executeRestructure(final LabelRestructureRun run) {
		var processed = 0;
		var unchanged = 0;
		var total = run.errandIds().size();

		try {
			final var labelSubstitutions = new HashMap<String, String>();
			final var mergedAwaySourceIds = new HashSet<String>();
			final var auditEntries = new ArrayList<AuditEntry>();

			final var steps = run.steps();
			for (var index = 0; index < steps.size(); index++) {
				final var step = steps.get(index);
				try {
					applyStep(run.namespace(), run.municipalityId(), step, labelSubstitutions, mergedAwaySourceIds, auditEntries);
				} catch (final RuntimeException e) {
					throw new IllegalStateException(STEP_FAILED_REASON.formatted(index, step.getType(), e.getMessage()), e);
				}
			}

			var ids = run.errandIds().stream().sorted().toList();
			var lastReport = System.nanoTime();

			for (var start = 0; start < ids.size(); start += batchSize) {
				var chunk = ids.subList(start, Math.min(start + batchSize, ids.size()));

				for (var errand : errandsRepository.findAllByIdForUpdate(chunk)) {
					if (!errandService.persistLabelRestructureMigration(errand, labelSubstitutions, run.startedByAdAccount())) {
						unchanged++;
					}
					processed++;

					if (System.nanoTime() - lastReport >= progressIntervalNanos) {
						jobService.updateProgress(run.jobId(), processed);
						lastReport = System.nanoTime();
					}
				}

				jobService.updateProgress(run.jobId(), processed);
				lastReport = System.nanoTime();

				if (isStopped(run)) {
					throw new StoppedException();
				}
			}

			if (isStopped(run)) {
				throw new StoppedException();
			}

			// Only reached once every affected errand has already been restowed off whichever source labels this run
			// merged away. An errand that kept its labels may still wear a source label, which must then not be deleted.
			if (!mergedAwaySourceIds.isEmpty()) {
				final var keptLabelIds = labelsStillWorn(mergedAwaySourceIds);
				if (!keptLabelIds.isEmpty()) {
					throw new IllegalStateException(MERGE_KEPT_SOURCES.formatted(keptLabelIds));
				}
				metadataService.deleteMergedLabels(mergedAwaySourceIds);
			}

			return new RestructureOutcome(processed - unchanged, unchanged, auditEntries);
		} catch (final RuntimeException e) {
			jobService.updateProgress(run.jobId(), processed);
			throw new LabelRestructureAbortedException(ROLLED_BACK_MESSAGE.formatted(processed, total, reasonFor(e)), e);
		}
	}

	private void applyStep(final String namespace, final String municipalityId, final LabelRestructureStep step,
		final Map<String, String> labelSubstitutions, final Set<String> mergedAwaySourceIds, final List<AuditEntry> auditEntries) {

		switch (step.getType()) {
			case ADD -> metadataService.addLabel(namespace, municipalityId, step.getPath(), step.getDisplayName(), step.getClassification());
			case RENAME -> metadataService.renameLabel(namespace, municipalityId, step.getPath(), step.getDisplayName());
			case DELETE -> metadataService.deleteLabel(namespace, municipalityId, step.getPath());
			case MOVE -> applyMove(namespace, municipalityId, step, auditEntries);
			case MERGE -> applyMerge(namespace, municipalityId, step, labelSubstitutions, mergedAwaySourceIds, auditEntries);
		}
	}

	private void applyMove(final String namespace, final String municipalityId, final LabelRestructureStep step, final List<AuditEntry> auditEntries) {
		final var labelId = findOrThrow(namespace, municipalityId, step.getPath(), LABEL_GONE).getId();

		final var destinationSegments = ofNullable(step.getDestinationParentPath()).orElse(List.of());
		final var newParentId = destinationSegments.isEmpty() ? null : findOrThrow(namespace, municipalityId, destinationSegments, PARENT_GONE).getId();

		metadataService.revalidateAndReparent(namespace, municipalityId, labelId, newParentId, step.getNewResourceName(), step.getDisplayName());

		auditEntries.add(new MoveAudit(labelId, newParentId));
	}

	private void applyMerge(final String namespace, final String municipalityId, final LabelRestructureStep step,
		final Map<String, String> labelSubstitutions, final Set<String> mergedAwaySourceIds, final List<AuditEntry> auditEntries) {

		final var targetId = findOrThrow(namespace, municipalityId, step.getPath(), LABEL_GONE).getId();
		final var sourceIds = step.getSourcePaths().stream()
			.map(sourcePath -> findOrThrow(namespace, municipalityId, sourcePath, LABEL_GONE).getId())
			.collect(toSet());

		metadataService.mergeInto(namespace, municipalityId, targetId, sourceIds);

		sourceIds.forEach(sourceId -> labelSubstitutions.put(sourceId, targetId));
		mergedAwaySourceIds.addAll(sourceIds);

		auditEntries.add(new MergeAudit(sourceIds, targetId));
	}

	/**
	 * The source labels an errand still wears once the errands have been restowed, sorted so the message naming them is
	 * stable.
	 */
	private List<String> labelsStillWorn(final Set<String> sourceLabelIds) {
		return sourceLabelIds.stream()
			.filter(sourceId -> errandsRepository.existsByLabelsMetadataLabelIdIn(Set.of(sourceId)))
			.sorted()
			.toList();
	}

	private MetadataLabelEntity findOrThrow(final String namespace, final String municipalityId, final List<String> path, final String messageTemplate) {
		final var resolvedPath = LabelTreeSnapshot.join(path);
		return metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(namespace, municipalityId, resolvedPath)
			.orElseThrow(() -> new IllegalStateException(messageTemplate.formatted(resolvedPath)));
	}

	/**
	 * Whether the job has left the state a run works in. A job that is gone counts as stopped too: there is nothing
	 * left to report against, so there is no reason to keep restowing errands on its behalf.
	 * <p>
	 * {@link JobService#statusOf} reads this with a transaction of its own ({@code REQUIRES_NEW}) rather than
	 * joining {@link #executeRestructure}'s, specifically so that a stop or a lease steal committed by some other
	 * request after this run's own long transaction began is actually visible here - joining would instead reuse
	 * that long transaction's own snapshot and never see it.
	 */
	private boolean isStopped(final LabelRestructureRun run) {
		return !jobService.statusOf(run.jobId())
			.filter(RUNNING::equals)
			.isPresent();
	}

	/**
	 * The reason a failure is reported with. Distinguishes the two cases an operator reading the job's own message
	 * benefits from telling apart from an opaque failure: a stop they themselves asked for, and a lock-acquisition
	 * timeout, which means nothing is actually broken - the namespace was merely busy - and a retry once it is idle
	 * is expected to succeed.
	 */
	private static String reasonFor(final RuntimeException e) {
		if (e instanceof StoppedException) {
			return STOPPED_REASON;
		}
		if (e instanceof CannotAcquireLockException) {
			return LOCK_TIMEOUT_REASON;
		}
		return e.getMessage();
	}

	/**
	 * {@code startedBy} as a person should read it: just the value, not {@link Identifier#toHeaderValue()}'s encoded
	 * {@code "<value>; type=<type>"} form that {@link MetadataService#startedBy()} actually carries through
	 * {@link LabelRestructureRun} so that {@link EventService#createLabelMoveEvent}/
	 * {@link EventService#createLabelMergeEvent} can rebuild the original type.
	 */
	private static String displayValue(final String startedBy) {
		var parsed = Identifier.parse(startedBy);
		return parsed != null ? parsed.getValue() : startedBy;
	}

	/**
	 * What {@link #executeRestructure} hands back to {@link #restructure} once committed: how many errands were
	 * restowed and how many kept the labels they had, and the audit events each MOVE/MERGE step queued - collected
	 * during the transaction but only ever emitted afterward, so neither ever announces a step that could still roll
	 * back.
	 */
	private record RestructureOutcome(int restowed, int unchanged, List<AuditEntry> auditEntries) {

		/**
		 * The outcome in words, for the summary of the job. The errands that kept their labels are named only when there
		 * are any.
		 */
		String describe() {
			return unchanged == 0 ? ONLY_RESTOWED.formatted(restowed) : RESTOWED_AND_UNCHANGED.formatted(restowed, unchanged);
		}
	}

	/**
	 * One MOVE or MERGE step's audit event, queued during {@link #executeRestructure} and emitted only once that has
	 * committed - ADD/RENAME/DELETE steps are not audited, mirroring the former {@code LabelTreeRestructureWorker}.
	 */
	private sealed interface AuditEntry permits MoveAudit, MergeAudit {
		void emit(EventService eventService, String municipalityId, String startedBy);
	}

	private record MoveAudit(String labelId, String newParentId)
		implements
		AuditEntry {
		@Override
		public void emit(final EventService eventService, final String municipalityId, final String startedBy) {
			eventService.createLabelMoveEvent(municipalityId, labelId, startedBy,
				MOVE_AUDIT_MESSAGE.formatted(labelId, newParentId, displayValue(startedBy)));
		}
	}

	private record MergeAudit(Set<String> sourceLabelIds, String targetLabelId)
		implements
		AuditEntry {
		@Override
		public void emit(final EventService eventService, final String municipalityId, final String startedBy) {
			eventService.createLabelMergeEvent(municipalityId, targetLabelId, startedBy,
				MERGE_AUDIT_MESSAGE.formatted(sourceLabelIds, targetLabelId, displayValue(startedBy)));
		}
	}

	/**
	 * Thrown from inside {@link #executeRestructure} to roll the transaction back when the job has been asked to
	 * stop - a plain {@code return} there would commit instead. Caught by the same {@code catch} every other failure
	 * is, so it is reported through the same path; {@link #reportAborted} tells it apart only to log it at a level
	 * that does not read as an error.
	 */
	private static final class StoppedException extends RuntimeException {
		StoppedException() {
			super(STOPPED_REASON);
		}
	}

	/**
	 * Wraps whatever caused {@link #executeRestructure} to roll back, carrying the fully-formed message
	 * {@link #reportAborted} reports the job as failed with - built at the throw site, where {@code processed} and
	 * {@code total} are in scope, rather than reconstructed later from the bare cause.
	 */
	private static final class LabelRestructureAbortedException extends RuntimeException {
		LabelRestructureAbortedException(final String message, final Throwable cause) {
			super(message, cause);
		}
	}
}
