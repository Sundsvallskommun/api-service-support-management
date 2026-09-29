package se.sundsvall.supportmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
class LabelMoveConfig {

	/**
	 * Threads for label-move runs, deliberately kept apart from the scheduler pool.
	 * <p>
	 * A move walks every errand under the moved label and may take a while for a large subtree. Borrowing a scheduler
	 * thread for that long would hold back everything else sharing that pool.
	 * <p>
	 * Bounded and given no queue, so a request that arrives with every thread busy is rejected outright and answered as
	 * such, rather than holding the request thread the way an unbounded executor would.
	 * <p>
	 * Given a grace period on shutdown, so a rolling deploy's SIGTERM does not cut a run off between one errand's write
	 * and the next - which, left uninterrupted, only shrinks how much of a large move an interrupted run leaves for its
	 * own resume (see {@code LabelMoveRunner}'s own doc) rather than eliminating the need for one; the pool still stops
	 * once the grace period elapses; a run still mid-page at that point is cut off exactly as it would have been
	 * without this, just less often.
	 */
	@Bean("labelMoveTaskExecutor")
	AsyncTaskExecutor labelMoveTaskExecutor(final LabelMoveProperties properties) {
		final var executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("label-move-");
		executor.setCorePoolSize(properties.maxConcurrentRuns());
		executor.setMaxPoolSize(properties.maxConcurrentRuns());
		executor.setQueueCapacity(0);
		executor.setAllowCoreThreadTimeOut(true);
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds((int) properties.shutdownGracePeriod().toSeconds());
		return executor;
	}
}
