package se.sundsvall.supportmanagement.service.purge;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeStatus;
import se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState;

import static java.util.Objects.isNull;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.RUNNING;

/**
 * A single purge run, from the moment it is accepted until it reaches a state it cannot leave.
 * <p>
 * Written by the thread doing the work and read by every thread answering a status request, which is why the counters
 * are atomics and the rest is volatile. {@link #finish} assigns the state last, so a reader that sees a job as finished
 * is guaranteed to see the timestamp and the message that go with it.
 */
public class PurgeJob {

	/**
	 * Stands for an unlimited run. Reaching it would take longer than the service will ever be up, so a run without a
	 * limit and a run with an unreachable one behave identically and the counting needs no special case.
	 */
	private static final long UNLIMITED = Long.MAX_VALUE;

	private final String jobId;
	private final String namespace;
	private final String municipalityId;
	private final PurgeSettings settings;
	private final long maxErrands;
	private final String startedBy;
	private final OffsetDateTime started;

	private final AtomicLong processed = new AtomicLong();
	private final AtomicLong deleted = new AtomicLong();
	private final AtomicLong failed = new AtomicLong();

	private volatile boolean stopRequested;
	private volatile PurgeState state = RUNNING;
	private volatile OffsetDateTime finished;
	private volatile String message;

	public PurgeJob(final String jobId, final String namespace, final String municipalityId, final PurgeSettings settings,
		final String startedBy, final OffsetDateTime started) {

		this.jobId = jobId;
		this.namespace = namespace;
		this.municipalityId = municipalityId;
		this.settings = settings;
		this.maxErrands = isNull(settings.maxErrands()) ? UNLIMITED : settings.maxErrands();
		this.startedBy = startedBy;
		this.started = started;
	}

	public String getJobId() {
		return jobId;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public OffsetDateTime getOlderThan() {
		return settings.olderThan();
	}

	public boolean isDryRun() {
		return settings.dryRun();
	}

	/**
	 * Who asked for the run, captured when it was accepted. The identifier of a caller lives on the request thread, which
	 * is not the thread the run is carried out on, so it has to be carried by the run itself.
	 */
	public String getStartedBy() {
		return startedBy;
	}

	public OffsetDateTime getStarted() {
		return started;
	}

	public OffsetDateTime getFinished() {
		return finished;
	}

	public PurgeState getState() {
		return state;
	}

	/**
	 * Asks the run to stop. It keeps its state until the run notices, since a run that has been asked to stop is still
	 * running until it actually does.
	 */
	public void requestStop() {
		this.stopRequested = true;
	}

	public boolean isStopRequested() {
		return stopRequested;
	}

	/**
	 * How many more errands the run may handle before it reaches the limit it was started with.
	 */
	public long remainingBudget() {
		return maxErrands - processed.get();
	}

	/**
	 * Records an errand the run reached but did not remove: every errand of a dry run, and any errand that turned out to
	 * be gone already by the time the run got to it.
	 */
	public void recordCounted() {
		processed.incrementAndGet();
	}

	public void recordDeleted() {
		processed.incrementAndGet();
		deleted.incrementAndGet();
	}

	public void recordFailed() {
		processed.incrementAndGet();
		failed.incrementAndGet();
	}

	/**
	 * Moves the run to the state it ends in. Only the first call has any effect, so a run that has already stopped is not
	 * overwritten by a later attempt to end it.
	 *
	 * @param finalState the state the run ends in.
	 * @param reason     why it ended. Always given for a run that did not reach the end of its namespace, since
	 *                   {@link PurgeState#STOPPED} alone does not say whether a run was asked to stop or ran into the
	 *                   limit it was started with.
	 * @param at         when it ended.
	 */
	public synchronized void finish(final PurgeState finalState, final String reason, final OffsetDateTime at) {
		if (state != RUNNING) {
			return;
		}

		this.message = reason;
		this.finished = at;
		this.state = finalState;
	}

	public ErrandPurgeStatus toStatus() {
		return ErrandPurgeStatus.create()
			.withJobId(jobId)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withOlderThan(settings.olderThan())
			.withDryRun(settings.dryRun())
			.withState(state)
			.withStartedBy(startedBy)
			.withStarted(started)
			.withFinished(finished)
			.withProcessed(processed.get())
			.withDeleted(deleted.get())
			.withFailed(failed.get())
			.withMessage(message);
	}
}
