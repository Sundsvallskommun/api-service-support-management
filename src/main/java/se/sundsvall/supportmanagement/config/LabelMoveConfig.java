package se.sundsvall.supportmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

@Configuration
class LabelMoveConfig {

	/**
	 * Threads for label-move runs, deliberately kept apart from the scheduler pool.
	 * <p>
	 * A move walks every errand under the moved label and may take a while for a large subtree. Borrowing a scheduler
	 * thread for that long would hold back everything else sharing that pool.
	 * <p>
	 * Bounded and given no queue ({@link JobExecutors#bounded}), so a request that arrives with every thread busy is
	 * rejected outright and answered as such, rather than holding the request thread the way an unbounded executor
	 * would.
	 * <p>
	 * Given a grace period on shutdown, so a rolling deploy's SIGTERM does not cut a run off mid-transaction - the
	 * whole move (re-parent and every restow) is one transaction (see {@code LabelMoveRunner}'s own doc), so a cut-off
	 * run is simply rolled back rather than left half done, and this grace period is what lets a move already close
	 * to finishing actually get there instead of being rolled back needlessly by a deploy landing a moment too soon.
	 */
	@Bean("labelMoveTaskExecutor")
	AsyncTaskExecutor labelMoveTaskExecutor(final LabelMoveProperties properties) {
		final var executor = JobExecutors.bounded("label-move-", properties.maxConcurrentRuns());
		// Unlike ErrandPurgeConfig's own pool: a move is bounded and short enough (minutes, not hours) that a grace
		// period on shutdown is worth having - a purge, by contrast, runs for so long that a short grace before its
		// threads are interrupted would be meaningless next to it.
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds((int) properties.shutdownGracePeriod().toSeconds());
		return executor;
	}
}
