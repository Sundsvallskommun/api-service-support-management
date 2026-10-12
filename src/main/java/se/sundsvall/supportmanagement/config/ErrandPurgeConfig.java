package se.sundsvall.supportmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
class ErrandPurgeConfig {

	/**
	 * Threads for purge runs, kept apart from the scheduler pool. A run walks a namespace one errand at a time and may
	 * hold its thread for hours.
	 * <p>
	 * Bounded to {@link ErrandPurgeProperties#maxConcurrentRuns()} threads and given no queue, so a request that arrives
	 * with every thread busy is rejected outright and answered as such.
	 */
	@Bean("errandPurgeTaskExecutor")
	AsyncTaskExecutor errandPurgeTaskExecutor(final ErrandPurgeProperties properties) {
		final var executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("errand-purge-");
		executor.setCorePoolSize(properties.maxConcurrentRuns());
		executor.setMaxPoolSize(properties.maxConcurrentRuns());
		executor.setQueueCapacity(0);
		executor.setAllowCoreThreadTimeOut(true);
		return executor;
	}
}
