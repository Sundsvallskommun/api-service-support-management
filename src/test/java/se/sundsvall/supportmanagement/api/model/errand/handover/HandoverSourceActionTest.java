package se.sundsvall.supportmanagement.api.model.errand.handover;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.errand.handover.HandoverSourceAction.*;

class HandoverSourceActionTest {

	@Test
	void enums() {
		assertThat(HandoverSourceAction.values()).containsExactly(CLOSE, RETAIN, SUSPEND);
	}

	@Test
	void enumValues() {
		assertThat(CLOSE).hasToString("CLOSE");
	}
}
