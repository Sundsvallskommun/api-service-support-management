package se.sundsvall.supportmanagement.api.model.errand;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.HIGH;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.LOW;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.MEDIUM;

import org.junit.jupiter.api.Test;

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
