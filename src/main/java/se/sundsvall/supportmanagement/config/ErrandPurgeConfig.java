package se.sundsvall.supportmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

@Configuration
class ErrandPurgeConfig {

	/**
	 * Threads for purge runs, kept apart from the scheduler pool. A run walks a namespace one errand at a time and may
	 * hold its thread for hours.
	 * <p>
	 * Bounded to {@link ErrandPurgeProperties#maxConcurrentRuns()} threads and given no queue
	 * ({@link JobExecutors#bounded}), so a request that arrives with every thread busy is rejected outright and answered
	 * as such.
	 * <p>
	 * No shutdown grace period, unlike {@code LabelRestructureConfig}'s pool: each errand a purge removes is committed on
	 * its own, so a run cut off by a deploy is no worse off than one cut off at any other point in its walk.
	 */
	@Bean("errandPurgeTaskExecutor")
	AsyncTaskExecutor errandPurgeTaskExecutor(final ErrandPurgeProperties properties) {
		return JobExecutors.bounded("errand-purge-", properties.maxConcurrentRuns());
	}
}
