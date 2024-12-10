package se.sundsvall.supportmanagement.service.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public abstract class ProcessingHealthIndicator implements HealthIndicator {

	private final AtomicBoolean healthy = new AtomicBoolean(true);
	private final AtomicBoolean errors = new AtomicBoolean(false);

	@Override
	public Health health() {
		if (healthy.get()) {
			return Health.up().build();
		} else {
			return Health.status("RESTRICTED").build();
		}
	}

	public void setUnhealthy() {
		healthy.set(false);
		errors.set(true);
	}

	public void setHealthy() {
		healthy.set(true);
		errors.set(false);
	}

	public void resetErrors() {
		errors.set(false);
	}

	public boolean hasErrors() {
		return errors.get();
	}
}
