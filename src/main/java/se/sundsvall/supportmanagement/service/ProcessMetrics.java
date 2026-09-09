package se.sundsvall.supportmanagement.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * The counters the process integration is followed by in operation.
 * <p>
 * Gathered in one place because a metric name is what a dashboard depends on: raised from wherever they happen to be
 * needed, the names drift apart, and a renamed counter is a panel that quietly goes flat rather than a compilation
 * error.
 * <p>
 * Registered at startup rather than on first use, so that a counter standing at zero is visible as a zero. A counter
 * that appears only once it has something to count cannot be told apart from one that is never raised at all, and both
 * of these are alarming precisely when they stay still.
 */
@Component
public class ProcessMetrics {

	/**
	 * How often a process and a handler work on the same errand at once. The most important operational indicator of the
	 * lot: rising, it says the process is working on errands that are being edited under it, and that work is being
	 * redone for nothing.
	 */
	static final String ERRAND_CONFLICT = "process.errand_conflict";

	/** Breaches of the modelling rule that no two parallel branches of a process may change the errand. */
	static final String CONCURRENT_TASK_DETECTED = "process.concurrent_task_detected";

	private final Counter errandConflict;
	private final Counter concurrentTaskDetected;

	ProcessMetrics(final MeterRegistry registry) {
		this.errandConflict = registry.counter(ERRAND_CONFLICT);
		this.concurrentTaskDetected = registry.counter(CONCURRENT_TASK_DETECTED);
	}

	public void errandConflict() {
		errandConflict.increment();
	}

	public void concurrentTaskDetected() {
		concurrentTaskDetected.increment();
	}
}
