package se.sundsvall.supportmanagement.service.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.config.LabelMoveProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.MetadataService;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus.RUNNING;

/**
 * Carries out label moves accepted by {@link MetadataService#startLabelMove}.
 * <p>
 * A move re-parents one label, refreshes {@code resourcePath} for its whole subtree, and restows every errand that
 * references the moved label or any of its descendants - {@link MetadataService#startLabelMove} collects the moved
 * label together with its whole descendant set and resolves the affected errand ids from that combined set before
 * this ever runs, rather than this runner discovering descendants on its own.
 * <p>
 * Carried out as a single transaction: the re-parent and every errand's restow, or none of it. Every affected errand
 * is locked with {@code SELECT ... FOR UPDATE} - ordered by id, matching the ascending order the frozen id list is
 * walked in chunk by chunk, so lock acquisition stays globally ordered and cannot deadlock against another ordered
 * multi-row operation - before it is touched, so a concurrent edit cannot land on it for the rest of this run. A
 * concurrent {@code @Version} conflict is therefore structurally impossible, and there is nothing left to retry.
 * <p>
 * A failure partway through - a lock-acquisition timeout, a stop request, anything else - rolls the whole
 * transaction back, the re-parent included: the label tree is left exactly as it was, and nothing is left half done
 * for a later run to pick up. A retry after that is a fresh attempt, not a resume, and is never a no-op, since
 * nothing from a rolled-back attempt was ever applied - {@link MetadataService#startLabelMove} rejects a genuine
 * no-op on that basis.
 * <p>
 * Never throws past {@link #run}: whatever goes wrong ends the job as failed, since the thread this runs on has
 * nobody else to report to.
 */
@Component
public class LabelMoveRunner extends JobRunner<LabelMoveRun> {

	private static final Logger LOG = LoggerFactory.getLogger(LabelMoveRunner.class);

	private static final String SUMMARY = "Label %s moved under %s, %d errand(s) restowed";
	private static final String AUDIT_MESSAGE = "Label %s moved under %s by %s, %d errand(s) restowed";
	private static final String ROLLED_BACK_MESSAGE = "Rolled back - no changes applied. Reached %d of %d errand(s) before: %s";
	private static final String LOCK_TIMEOUT_REASON = "could not acquire locks - one or more errands are being edited; retry when the namespace is idle";
	private static final String STOPPED_REASON = "stopped by request";
	private static final String ENDED_WITHOUT_RESULT = "Label move ended without reaching a result of its own";

	private final ErrandsRepository errandsRepository;
	private final ErrandService errandService;
	private final MetadataService metadataService;
	private final JobService jobService;
	private final EventService eventService;
	private final LabelMoveRunner self;
	private final int batchSize;
	private final long progressIntervalNanos;

	/**
	 * {@code self}: reached through this instance's own Spring proxy rather than through {@code this}, the same reason
	 * {@code JobService} reaches {@code fail}/{@code get} through its own {@code self} field (see its own javadoc) -
	 * so that {@link #executeMove}'s {@code @Transactional} actually takes effect: {@link #move} calls it directly on
	 * {@code this}, and a call from inside the same instance bypasses the proxy every annotation like that lives on.
	 * {@code @Lazy} because the proxy this needs is this very bean's own, not yet constructed while its own
	 * constructor runs.
	 */
	LabelMoveRunner(
		final ErrandsRepository errandsRepository,
		final ErrandService errandService,
		final MetadataService metadataService,
		final JobService jobService,
		final EventService eventService,
		final LabelMoveProperties properties,
		@Lazy final LabelMoveRunner self) {
		super(jobService);
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
	protected String jobId(final LabelMoveRun run) {
		return run.jobId();
	}

	@Override
	protected void work(final LabelMoveRun run) {
		LOG.info("Label move {} started for label {} to parent {} in namespace {} for municipality {} by {}",
			run.jobId(), sanitizeForLogging(run.labelId()), sanitizeForLogging(run.newParentId()),
			sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), sanitizeForLogging(displayValue(run.startedBy())));

		move(run);

		LOG.info("Label move {} ended", run.jobId());
	}

	@Override
	protected String reportAborted(final LabelMoveRun run, final Exception e) {
		// A user-requested stop is not a failure - it is logged at its own level rather than as an error, same as the
		// clean early return the old per-errand design used to make for this same case.
		if (e instanceof final LabelMoveAbortedException aborted && aborted.getCause() instanceof StoppedException) {
			LOG.info("Label move {} stopped for label {} in namespace {}: {}", run.jobId(), sanitizeForLogging(run.labelId()), sanitizeForLogging(run.namespace()), e.getMessage());
		} else {
			LOG.error("Label move {} aborted for label {} in namespace {}", run.jobId(), sanitizeForLogging(run.labelId()), sanitizeForLogging(run.namespace()), e);
		}
		// Already fully formed at the throw site inside executeMove, where processed/total and the failure reason are
		// in scope - nothing left to add here.
		return e.getMessage();
	}

	@Override
	protected String endedWithoutResultMessage() {
		return ENDED_WITHOUT_RESULT;
	}

	/**
	 * Carries the move to its end.
	 * <p>
	 * The audit event and {@link JobService#complete} are deliberately called from here, once {@code self.executeMove}
	 * has already returned, rather than from inside it: a {@code @Transactional} method's transaction commits as the
	 * call returns through its own proxy, so anything called from inside the method body itself would still run
	 * before that commit has actually happened. Calling them here instead means neither ever reports success before
	 * the restow it is reporting on is durably committed.
	 */
	private void move(final LabelMoveRun run) {
		var processed = self.executeMove(run);

		eventService.createLabelMoveEvent(run.municipalityId(), run.labelId(), run.startedBy(),
			AUDIT_MESSAGE.formatted(run.labelId(), run.newParentId(), displayValue(run.startedBy()), processed));

		jobService.complete(run.jobId(), SUMMARY.formatted(run.labelId(), run.newParentId(), processed));
	}

	/**
	 * Re-parents the label and restows every affected errand in one transaction - see the class doc for why. Public
	 * rather than package-private only so that {@link #self} reaches this through the proxy that {@code @Transactional}
	 * lives on; nothing outside this class calls it.
	 * <p>
	 * Every affected errand is locked - {@link ErrandsRepository#findAllByIdForUpdate}, ordered by id - immediately
	 * before it is restowed, a chunk ({@link LabelMoveProperties#batchSize()}) at a time, in the same ascending order
	 * the frozen id list itself is sorted into once, up front.
	 * <p>
	 * Progress is written as the walk goes - at the existing time-based heartbeat and at every chunk boundary, exactly
	 * as before - through {@link JobService#updateProgress}, which commits in a transaction of its own
	 * ({@code REQUIRES_NEW}) and so survives this transaction's own eventual commit or rollback either way. Once
	 * more, with the exact count reached, the moment anything here fails - the periodic heartbeat alone would only
	 * leave the last tick's count, not the precise point of failure.
	 * <p>
	 * Whether the job has been asked to stop is checked at every chunk boundary, and once more after the walk ends -
	 * covering a frozen id list with nothing in it, which never enters the loop. Found stopped, this throws rather
	 * than returning: a plain return from inside a {@code @Transactional} method commits, and stopping an atomic move
	 * must undo it instead.
	 *
	 * @return how many errands were restowed - always equal to the full affected set on success, since any failure
	 *         partway through rolls the whole transaction back rather than leaving a partial result.
	 */
	@Transactional
	public int executeMove(final LabelMoveRun run) {
		var processed = 0;
		var total = run.errandIds().size();

		try {
			metadataService.revalidateAndReparent(run.namespace(), run.municipalityId(), run.labelId(), run.newParentId());

			var ids = run.errandIds().stream().sorted().toList();
			var lastReport = System.nanoTime();

			for (var start = 0; start < ids.size(); start += batchSize) {
				var chunk = ids.subList(start, Math.min(start + batchSize, ids.size()));

				for (var errand : errandsRepository.findAllByIdForUpdate(chunk)) {
					errandService.persistLabelMigration(errand);
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

			return processed;
		} catch (final RuntimeException e) {
			jobService.updateProgress(run.jobId(), processed);
			throw new LabelMoveAbortedException(ROLLED_BACK_MESSAGE.formatted(processed, total, reasonFor(e)), e);
		}
	}

	/**
	 * Whether the job has left the state a run works in. A job that is gone counts as stopped too: there is nothing
	 * left to report against, so there is no reason to keep restowing errands on its behalf.
	 * <p>
	 * {@link JobService#statusOf} reads this with a transaction of its own ({@code REQUIRES_NEW}) rather than joining
	 * {@link #executeMove}'s, specifically so that a stop or a lease steal committed by some other request after this
	 * run's own long transaction began is actually visible here - joining would instead reuse that long transaction's
	 * own snapshot and never see it.
	 */
	private boolean isStopped(final LabelMoveRun run) {
		return !jobService.statusOf(run.jobId())
			.filter(RUNNING::equals)
			.isPresent();
	}

	/**
	 * The reason a failure is reported with. Distinguishes the two cases an operator reading the job's own message
	 * benefits from telling apart from an opaque failure: a stop they themselves asked for, and a lock-acquisition
	 * timeout, which means nothing is actually broken - the namespace was merely busy - and a retry once it is idle is
	 * expected to succeed.
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
	 * {@link LabelMoveRun} so that {@link EventService#createLabelMoveEvent} can rebuild the original type. Mirrors
	 * {@code EventService#toExecutedBy}'s own fallback: a value that fails to parse (the "unknown" placeholder for a
	 * move with no caller) is returned as it was.
	 */
	private static String displayValue(final String startedBy) {
		var parsed = Identifier.parse(startedBy);
		return parsed != null ? parsed.getValue() : startedBy;
	}

	/**
	 * Thrown from inside {@link #executeMove} to roll the transaction back when the job has been asked to stop - a
	 * plain {@code return} there would commit instead. Caught by the same {@code catch} every other failure is, so it
	 * is reported through the same path; {@link #reportAborted} tells it apart only to log it at a level that does
	 * not read as an error.
	 */
	private static final class StoppedException extends RuntimeException {
		StoppedException() {
			super(STOPPED_REASON);
		}
	}

	/**
	 * Wraps whatever caused {@link #executeMove} to roll back, carrying the fully-formed message
	 * {@link #reportAborted} reports the job as failed with - built at the throw site, where {@code processed} and
	 * {@code total} are in scope, rather than reconstructed later from the bare cause.
	 */
	private static final class LabelMoveAbortedException extends RuntimeException {
		LabelMoveAbortedException(final String message, final Throwable cause) {
			super(message, cause);
		}
	}
}
