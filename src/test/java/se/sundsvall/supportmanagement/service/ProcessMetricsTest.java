package se.sundsvall.supportmanagement.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.ProcessMetrics.CONCURRENT_TASK_DETECTED;
import static se.sundsvall.supportmanagement.service.ProcessMetrics.ERRAND_CONFLICT;

class ProcessMetricsTest {

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
	private final ProcessMetrics metrics = new ProcessMetrics(registry);

	/**
	 * The zero has to be visible. A counter that appears only once it has something to count cannot be told apart from
	 * one that is never raised at all, and both of these are alarming precisely when they stay still.
	 */
	@Test
	void bothCountersExistBeforeAnythingHasHappened() {
		assertThat(registry.find(ERRAND_CONFLICT).counter()).isNotNull();
		assertThat(registry.find(CONCURRENT_TASK_DETECTED).counter()).isNotNull();
	}

	@Test
	void countingAConflictRaisesTheConflictCounterAlone() {
		metrics.errandConflict();
		metrics.errandConflict();

		assertThat(registry.get(ERRAND_CONFLICT).counter().count()).isEqualTo(2);
		assertThat(registry.get(CONCURRENT_TASK_DETECTED).counter().count()).isZero();
	}

	@Test
	void countingConcurrentTasksRaisesThatCounterAlone() {
		metrics.concurrentTaskDetected();

		assertThat(registry.get(CONCURRENT_TASK_DETECTED).counter().count()).isEqualTo(1);
		assertThat(registry.get(ERRAND_CONFLICT).counter().count()).isZero();
	}
}
