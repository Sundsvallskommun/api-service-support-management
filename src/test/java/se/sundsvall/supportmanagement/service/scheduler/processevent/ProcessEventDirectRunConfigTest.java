package se.sundsvall.supportmanagement.service.scheduler.processevent;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.DirectRun;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.LoopGuard;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ProcessEventDirectRunConfigTest {

	@Test
	@DisplayName("Verification that the pool is sized from the settings and kept apart by its name")
	void thePoolIsSizedFromTheSettings() {
		final var executor = new ProcessEventDirectRunConfig().processEventExecutor(properties(2, 4, 500));

		assertThat(executor.getCorePoolSize()).isEqualTo(2);
		assertThat(executor.getMaxPoolSize()).isEqualTo(4);
		assertThat(executor.getQueueCapacity()).isEqualTo(500);
		assertThat(executor.getThreadNamePrefix()).isEqualTo("process-event-");
	}

	@Test
	@DisplayName("Verification that a full pool drops a direct run rather than throwing in the thread that committed the errand")
	void aFullPoolDropsTheRunRatherThanThrowing() throws InterruptedException {
		final var executor = new ProcessEventDirectRunConfig().processEventExecutor(properties(1, 1, 1));
		executor.initialize();

		final var release = new CountDownLatch(1);
		final var dropped = new AtomicInteger();

		try {
			executor.execute(() -> awaitQuietly(release));
			executor.execute(() -> awaitQuietly(release));

			assertThatNoException().isThrownBy(() -> executor.execute(dropped::incrementAndGet));
		} finally {
			release.countDown();
			executor.shutdown();
		}

		assertThat(executor.getThreadPoolExecutor().awaitTermination(5, SECONDS)).isTrue();
		assertThat(dropped).hasValue(0);
	}

	private static ProcessEngineProperties properties(final int corePoolSize, final int maxPoolSize, final int queueCapacity) {
		return new ProcessEngineProperties(new LoopGuard(20, Duration.ofMinutes(10)), new DirectRun(true, corePoolSize, maxPoolSize, queueCapacity));
	}

	private static void awaitQuietly(final CountDownLatch latch) {
		try {
			latch.await(5, SECONDS);
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		}
	}
}
