package se.sundsvall.supportmanagement.integration.db.model.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.ATTACHMENT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.DECISION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.MESSAGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.NOTE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.SUSPENSION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.SYSTEM;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.values;

import org.junit.jupiter.api.Test;

class NotificationSubTypeTest {

	@Test
	void enums() {
		assertThat(values()).containsExactlyInAnyOrder(ATTACHMENT, DECISION, ERRAND, MESSAGE, NOTE, SYSTEM, SUSPENSION);
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
