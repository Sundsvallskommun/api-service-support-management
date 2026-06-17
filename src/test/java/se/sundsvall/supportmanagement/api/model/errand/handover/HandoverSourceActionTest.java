package se.sundsvall.supportmanagement.api.model.errand.handover;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.errand.handover.HandoverSourceAction.CLOSE;
import static se.sundsvall.supportmanagement.api.model.errand.handover.HandoverSourceAction.RETAIN;
import static se.sundsvall.supportmanagement.api.model.errand.handover.HandoverSourceAction.SUSPEND;

class HandoverSourceActionTest {

	@Test
	void enums() {
		assertThat(HandoverSourceAction.values()).containsExactly(CLOSE, RETAIN, SUSPEND);
	}

	@Test
	void enumToString() {
		assertThat(CLOSE).hasToString("CLOSE");
		assertThat(RETAIN).hasToString("RETAIN");
		assertThat(SUSPEND).hasToString("SUSPEND");
	}
}
