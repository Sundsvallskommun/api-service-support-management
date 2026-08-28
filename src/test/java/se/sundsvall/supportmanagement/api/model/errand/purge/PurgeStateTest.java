package se.sundsvall.supportmanagement.api.model.errand.purge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.COMPLETED;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.FAILED;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.RUNNING;
import static se.sundsvall.supportmanagement.api.model.errand.purge.PurgeState.STOPPED;

class PurgeStateTest {

	@Test
	void enums() {
		assertThat(PurgeState.values()).containsExactly(RUNNING, COMPLETED, STOPPED, FAILED);
	}

	@Test
	void enumToString() {
		assertThat(RUNNING).hasToString("RUNNING");
		assertThat(COMPLETED).hasToString("COMPLETED");
		assertThat(STOPPED).hasToString("STOPPED");
		assertThat(FAILED).hasToString("FAILED");
	}
}
