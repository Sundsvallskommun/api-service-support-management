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
	 */
	@Bean("searchReindexTaskExecutor")
	AsyncTaskExecutor searchReindexTaskExecutor() {
		return JobExecutors.bounded("search-reindex-", 1);
	}
}
