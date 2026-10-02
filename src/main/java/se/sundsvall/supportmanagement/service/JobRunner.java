package se.sundsvall.supportmanagement.service;

/**
 * The envelope a label-tree job (move, merge, restructure) is carried out inside, shared by
 * {@link LabelMoveWorker}, {@link LabelMergeWorker} and {@link LabelTreeRestructureWorker} so that none of the three
 * has to reimplement the same start/fail bookkeeping: mark the job running, do the type-specific work, and fail it
 * with a clear message if anything goes wrong - including a thread taken down by something that is not an
 * {@link Exception} (an {@link Error}), which would otherwise leave the job reading as running for as long as it
 * lives. Never throws past {@link #run} on its own account.
 *
 * @param <R> the run type this carries out - {@code LabelMoveRun}, {@code LabelMergeRun} or
 *            {@code LabelRestructureRun}.
 */
abstract class JobRunner<R> {

	private final JobService jobService;

	protected JobRunner(final JobService jobService) {
		this.jobService = jobService;
	}

	/**
	 * Runs one job to its end.
	 * <p>
	 * Deliberately not {@code final}: {@link MetadataService} injects each of this class's three subclasses behind
	 * {@code @Lazy} to break a construction cycle (the worker sits behind {@code ErrandService}, which eventually
	 * leads back to {@code MetadataService} itself), and Spring backs that with a CGLIB subclass proxy - a proxy
	 * built by objenesis, without ever running a real constructor, whose fields (including this class's own
	 * {@code jobService}) stay unset until a call is intercepted and delegated to the real bean. CGLIB cannot
	 * override a {@code final} method, so a final {@code run} here would leave the proxy's own copy
	 * uninterceptable: the call would execute directly against the proxy's own, never-initialized
	 * {@code jobService}, failing with a {@code NullPointerException} on the executor thread that nothing reports
	 * back - the job would stay {@code PENDING} forever instead of ending as failed.
	 *
	 * @param run the run to carry out.
	 */
	public void run(final R run) {
		final var jobId = jobId(run);
		logStarted(run);

		var ended = false;
		try {
			jobService.setRunning(jobId);
			work(run);
			ended = true;
		} catch (final Exception e) {
			logAborted(run, e);
			jobService.fail(jobId, abortedMessage(e));
			ended = true;
		} finally {
			// A thread taken down by something that is not an exception - an Error - would otherwise leave the job
			// reading as running for as long as it lives.
			if (!ended) {
				jobService.fail(jobId, endedWithoutResultMessage());
			}
		}

		logEnded(run);
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
	 * Logs the run starting, with whatever detail belongs on that line for this kind of job.
	 */
	protected abstract void logStarted(R run);

	/**
	 * Logs whatever detail belongs on an aborted run, at ERROR level with {@code e} attached for its stack trace.
	 */
	protected abstract void logAborted(R run, Exception e);

	/**
	 * The message {@link JobService#fail} is given for a run that threw.
	 */
	protected abstract String abortedMessage(Exception e);

	/**
	 * Logs the run having ended - reached regardless of whether it succeeded or was caught and failed above, but not
	 * for a thread taken down by an {@link Error}, which unwinds past this entirely.
	 */
	protected abstract void logEnded(R run);

	/**
	 * The message a job is failed with when the thread carrying it out was taken down by something that let neither
	 * {@link #work} nor {@link #logAborted}/{@link #abortedMessage} run.
	 */
	protected abstract String endedWithoutResultMessage();
}
