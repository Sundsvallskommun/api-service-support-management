package se.sundsvall.supportmanagement.api.model.errand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.HIGH;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.LOW;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.MEDIUM;

class PriorityTest {

	@Test
	void enums() {
		assertThat(Priority.values()).containsExactlyInAnyOrder(HIGH, LOW, MEDIUM);
	}

	@Test
	void enumValues() {
		assertThat(HIGH).hasToString("HIGH");
		assertThat(LOW).hasToString("LOW");
		assertThat(MEDIUM).hasToString("MEDIUM");
	}
}
