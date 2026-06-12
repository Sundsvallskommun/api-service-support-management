package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ATTACHMENT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.DECISION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.HANDOVER_IN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.HANDOVER_OUT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.NOTE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SUSPENSION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SYSTEM;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.values;

class EventSubTypeTest {

	@Test
	void enums() {
		assertThat(values()).containsExactlyInAnyOrder(ATTACHMENT, DECISION, ERRAND, HANDOVER_IN, HANDOVER_OUT, MESSAGE, NOTE, SYSTEM, SUSPENSION);
	}

	@Test
	void enumValues() {
		assertThat(ATTACHMENT.getValue()).isEqualTo("ATTACHMENT");
		assertThat(DECISION.getValue()).isEqualTo("DECISION");
		assertThat(ERRAND.getValue()).isEqualTo("ERRAND");
		assertThat(MESSAGE.getValue()).isEqualTo("MESSAGE");
		assertThat(NOTE.getValue()).isEqualTo("NOTE");
		assertThat(SYSTEM.getValue()).isEqualTo("SYSTEM");
		assertThat(SUSPENSION.getValue()).isEqualTo("SUSPENSION");
	}
}
