package se.sundsvall.supportmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

@Configuration
class SearchReindexConfig {

	/**
	 * The thread a rebuild of a namespace asked for through the API is carried out on. One, since one rebuild runs at a
	 * time across every pod anyway, guarded by a lock of its own; the nightly reindex carries out its rebuilds on its own
	 * thread instead, one after the other.
	 * <p>
	 * With room for one rebuild to wait. A rebuild releases the lock as it ends, a moment before its thread is free
	 * again, and a rebuild asked for in that moment gets the lock and would be refused the thread. The lock keeps more
	 * than one from ever waiting.
	 */
	@Bean("searchReindexTaskExecutor")
	AsyncTaskExecutor searchReindexTaskExecutor() {
		final var executor = JobExecutors.bounded("search-reindex-", 1);
		executor.setQueueCapacity(1);
		return executor;
	}
}
