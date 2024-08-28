package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class EmailProcessingHealthIndicatorTest {

	@Test
	void initialState() {
		var healthIndicator = new EmailProcessingHealthIndicator();

		assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
		assertThat(healthIndicator.hasErrors()).isFalse();
	}

	@Test
	void setUnhealthy() {
		var healthIndicator = new EmailProcessingHealthIndicator();
		healthIndicator.setUnhealthy();

		assertThat(healthIndicator.health().getStatus().getCode()).isEqualTo("RESTRICTED");
		assertThat(healthIndicator.hasErrors()).isTrue();
	}

	@Test
	void setHealthy() {
		var healthIndicator = new EmailProcessingHealthIndicator();
		healthIndicator.setUnhealthy();
		healthIndicator.setHealthy();

		assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
		assertThat(healthIndicator.hasErrors()).isFalse();
	}

	@Test
	void resetErrors() {
		var healthIndicator = new EmailProcessingHealthIndicator();
		healthIndicator.setUnhealthy();
		healthIndicator.resetErrors();

		assertThat(healthIndicator.health().getStatus().getCode()).isEqualTo("RESTRICTED");
		assertThat(healthIndicator.hasErrors()).isFalse();
	}
}
