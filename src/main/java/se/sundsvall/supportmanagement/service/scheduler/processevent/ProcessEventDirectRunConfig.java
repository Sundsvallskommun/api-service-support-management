package se.sundsvall.supportmanagement.service.scheduler.processevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;

@Configuration
class ProcessEventDirectRunConfig {

	static final String PROCESS_EVENT_EXECUTOR = "processEventExecutor";

	private static final Logger LOG = LoggerFactory.getLogger(ProcessEventDirectRunConfig.class);

	/**
	 * Threads for the direct runs of the relay, kept apart from the scheduler pool and from the request threads.
	 * <p>
	 * Bounded, and a direct run arriving at a full pool is dropped with a warning, without anything being thrown in the
	 * thread serving the write. The scheduled run takes every row a direct run did not.
	 */
	@Bean(PROCESS_EVENT_EXECUTOR)
	ThreadPoolTaskExecutor processEventExecutor(final ProcessEngineProperties processEngineProperties) {
		final var directRun = processEngineProperties.directRun();
		final var executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("process-event-");
		executor.setCorePoolSize(directRun.corePoolSize());
		executor.setMaxPoolSize(directRun.maxPoolSize());
		executor.setQueueCapacity(directRun.queueCapacity());
		executor.setRejectedExecutionHandler((_, pool) -> LOG.warn(
			"Dropped a direct run of the process event relay, since all {} threads are busy and {} runs are waiting. The scheduled run delivers its events instead",
			pool.getMaximumPoolSize(), pool.getQueue().size()));
		return executor;
	}
}
