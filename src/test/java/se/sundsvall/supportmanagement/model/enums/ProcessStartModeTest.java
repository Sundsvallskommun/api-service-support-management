package se.sundsvall.supportmanagement.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.model.enums.ProcessStartMode.MANUAL;
import static se.sundsvall.supportmanagement.model.enums.ProcessStartMode.values;

class ProcessStartModeTest {

	@Test
	void enums() {
		assertThat(values()).containsExactlyInAnyOrder(AUTOMATIC, MANUAL);
	}
}
