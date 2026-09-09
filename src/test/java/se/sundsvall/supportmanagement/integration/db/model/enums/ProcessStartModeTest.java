package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.MANUAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.values;

class ProcessStartModeTest {

	@Test
	void enums() {
		assertThat(values()).containsExactlyInAnyOrder(AUTOMATIC, MANUAL);
	}
}
