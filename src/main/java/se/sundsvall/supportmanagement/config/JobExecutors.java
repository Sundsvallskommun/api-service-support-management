package se.sundsvall.supportmanagement.config;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The part of building a job executor that every kind of job shares - bounded, no queue, core threads allowed to
 * time out - factored out of {@code LabelMoveConfig} and {@code ErrandPurgeConfig}, which used to build this same
 * executor twice over, differing only in thread-name prefix and pool size. The pools themselves stay separate: a
 * purge "may be at it for hours", so sharing one pool between the two kinds of job would let a bulk deletion starve
 * label moves for just as long, and the 503 a caller gets when a pool has no thread free promises "too many jobs
 * <em>of this kind</em>", which is only true per kind.
 */
final class JobExecutors {

	private JobExecutors() {}

	/**
	 * A pool bounded to {@code poolSize} threads with no queue, so a request that arrives with every thread busy is
	 * rejected outright and answered as such, rather than holding the request thread the way an unbounded executor -
	 * or a bounded {@link org.springframework.core.task.SimpleAsyncTaskExecutor} - would.
	 */
	static ThreadPoolTaskExecutor bounded(final String threadNamePrefix, final int poolSize) {
		final var executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix(threadNamePrefix);
		executor.setCorePoolSize(poolSize);
		executor.setMaxPoolSize(poolSize);
		executor.setQueueCapacity(0);
		executor.setAllowCoreThreadTimeOut(true);
		return executor;
	}
}
