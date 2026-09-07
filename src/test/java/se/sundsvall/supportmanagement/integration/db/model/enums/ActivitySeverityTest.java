package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.WARN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.values;

class ActivitySeverityTest {

	@Test
	void enums() {
		assertThat(values()).containsExactlyInAnyOrder(INFO, WARN, ERROR);
	}
}
