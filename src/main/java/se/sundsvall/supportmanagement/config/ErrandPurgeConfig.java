package se.sundsvall.supportmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

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
	 * everything else the service does. The pool is given no queue ({@link JobExecutors#bounded}), so a request that
	 * arrives with every thread busy is rejected outright and answered as such. A bounded
	 * {@link org.springframework.core.task.SimpleAsyncTaskExecutor} would instead hold the request thread until a run
	 * finished, which for a purge means hours.
	 * <p>
	 * No shutdown grace period, unlike {@code LabelMoveConfig}'s own pool: a purge runs for hours, so any grace period
	 * short enough to be worth configuring would be meaningless next to it - a run cut off by a deploy is no worse off
	 * than one cut off at any other point in its walk, since each errand it removes is already committed on its own as
	 * it goes.
	 */
	@Bean("errandPurgeTaskExecutor")
	AsyncTaskExecutor errandPurgeTaskExecutor(final ErrandPurgeProperties properties) {
		return JobExecutors.bounded("errand-purge-", properties.maxConcurrentRuns());
	}
}
