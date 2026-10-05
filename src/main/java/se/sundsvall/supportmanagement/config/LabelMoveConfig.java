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
	 */
	@Bean("labelMoveTaskExecutor")
	AsyncTaskExecutor labelMoveTaskExecutor(final LabelMoveProperties properties) {
		final var executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("label-move-");
		executor.setCorePoolSize(properties.maxConcurrentRuns());
		executor.setMaxPoolSize(properties.maxConcurrentRuns());
		executor.setQueueCapacity(0);
		executor.setAllowCoreThreadTimeOut(true);
		return executor;
	}
}
