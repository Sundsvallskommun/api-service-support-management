package se.sundsvall.supportmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
class ErrandPurgeConfig {

	/**
	 * Threads for purge runs, deliberately kept apart from the scheduler pool.
	 * <p>
	 * A purge walks a namespace one errand at a time and may be at it for hours. Borrowing a scheduler thread for that
	 * long would hold back email collection, notification dispatch and everything else sharing that pool.
	 * <p>
	 * Bounded, since only one run at a time is allowed per namespace but nothing stops a service holding many namespaces
	 * from having all of them purged at once, and they share a database and the services a deletion reaches into with
	 * everything else the service does. The pool is given no queue, so a request that arrives with every thread busy is
	 * rejected outright and answered as such. A bounded {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
	 * would instead hold the request thread until a run finished, which for a purge means hours.
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
