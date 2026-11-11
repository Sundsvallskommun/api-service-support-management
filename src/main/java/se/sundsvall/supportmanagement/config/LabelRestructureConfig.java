package se.sundsvall.supportmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

@Configuration
class LabelRestructureConfig {

	/**
	 * Threads for label-restructure runs, deliberately kept apart from the scheduler pool.
	 * <p>
	 * A restructure walks every errand under each moved/merged label and may take a while for a large subtree.
	 * Borrowing a scheduler thread for that long would hold back everything else sharing that pool.
	 * <p>
	 * Bounded and given no queue ({@link JobExecutors#bounded}), so a request that arrives with every thread busy is
	 * rejected outright and answered as such, rather than holding the request thread the way an unbounded executor
	 * would.
	 */
	@Bean("labelRestructureTaskExecutor")
	AsyncTaskExecutor labelRestructureTaskExecutor(final LabelRestructureProperties properties) {
		return JobExecutors.bounded("label-restructure-", properties.maxConcurrentRuns());
	}
}
