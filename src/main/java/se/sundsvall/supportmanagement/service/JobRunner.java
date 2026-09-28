package se.sundsvall.supportmanagement.service;

/**
 * The envelope a job-service run is carried out inside, once dispatched by {@link JobService#launch}: mark the job
 * running, do the work, and end it - failed with the work's own message if it threw, failed with a generic one if
 * the thread was taken down by something that is not an exception (an {@link Error}) before {@link #work} could
 * report anything of its own. Never throws past {@link #run} on its own account - an {@code Error} is the one
 * exception this still lets through, since the {@code finally} below has already failed the job by the time it does.
 * <p>
 * Ending in a state of its own on success is {@link #work}'s job, not this envelope's - usually a call of its own to
 * {@link JobService#complete} once it has something to say about the outcome.
 *
 * @param <R> the run type this carries out - {@code LabelMoveRun}, {@code PurgeRun}, or a future kind of job's own.
 */
public abstract class JobRunner<R> {

	private final JobService jobService;

	protected JobRunner(final JobService jobService) {
		this.jobService = jobService;
	}

	/**
	 * Runs one job to its end.
	 * <p>
	 * Deliberately not {@code final}: {@link MetadataService} injects its {@code LabelMoveRunner} behind
	 * {@code @Lazy} to break a construction cycle, and Spring backs that with a CGLIB subclass proxy - a proxy built
	 * by objenesis, without ever running a real constructor, whose fields stay unset until a call is intercepted and
	 * delegated to the real bean. CGLIB cannot override a {@code final} method, so a final {@code run} here would
	 * leave the proxy's own copy uninterceptable: the call would execute directly against the proxy's own,
	 * never-initialized {@code jobService}, failing with a {@code NullPointerException} on the executor thread that
	 * nothing reports back - the job would stay {@code PENDING} forever instead of ending as failed.
	 *
	 * @param run the run to carry out.
	 */
	public void run(final R run) {
		final var jobId = jobId(run);
		var ended = false;

		try {
			jobService.setRunning(jobId);
			work(run);
			ended = true;
		} catch (final Exception e) {
			jobService.fail(jobId, reportAborted(run, e));
			ended = true;
		} finally {
			// A thread taken down by something that is not an exception - an Error - would otherwise leave the job
			// reading as running for as long as it lives.
			if (!ended) {
				jobService.fail(jobId, endedWithoutResultMessage());
			}
		}
	}

	/**
	 * The id of the job this run reports against.
	 */
	protected abstract String jobId(R run);

	/**
	 * Carries the run to its end. Expected to leave the job in a terminal state of its own on success, typically by
	 * calling {@link JobService#complete}.
	 */
	protected abstract void work(R run);

	/**
	 * Logs whatever detail belongs on an aborted run and returns the message {@link JobService#fail} is given for it.
	 */
	protected abstract String reportAborted(R run, Exception e);

	/**
	 * The message a job is failed with when the thread carrying it out was taken down by something that let neither
	 * {@link #work} nor {@link #reportAborted} run.
	 */
	protected abstract String endedWithoutResultMessage();
}
