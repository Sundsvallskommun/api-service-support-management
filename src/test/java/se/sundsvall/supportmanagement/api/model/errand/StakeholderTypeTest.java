package se.sundsvall.supportmanagement.api.model.errand;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.errand.StakeholderType.EMPLOYEE;
import static se.sundsvall.supportmanagement.api.model.errand.StakeholderType.ENTERPRISE;
import static se.sundsvall.supportmanagement.api.model.errand.StakeholderType.PRIVATE;

import org.junit.jupiter.api.Test;

class StakeholderTypeTest {

	@Test
	void enums() {
		assertThat(StakeholderType.values()).containsExactlyInAnyOrder(EMPLOYEE, ENTERPRISE, PRIVATE);
	}

	@Test
	void enumValues() {
		assertThat(EMPLOYEE).hasToString("EMPLOYEE");
		assertThat(ENTERPRISE).hasToString("ENTERPRISE");
		assertThat(PRIVATE).hasToString("PRIVATE");
	}
}
