package se.sundsvall.supportmanagement.api.model.errand;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.errand.CustomerType.EMPLOYEE;
import static se.sundsvall.supportmanagement.api.model.errand.CustomerType.ENTERPRISE;
import static se.sundsvall.supportmanagement.api.model.errand.CustomerType.PRIVATE;

import org.junit.jupiter.api.Test;

class CustomerTypeTest {

	@Test
	void enums() {
		assertThat(CustomerType.values()).containsExactlyInAnyOrder(EMPLOYEE, ENTERPRISE, PRIVATE);
	}

	@Test
	void enumValues() {
		assertThat(EMPLOYEE).hasToString("EMPLOYEE");
		assertThat(ENTERPRISE).hasToString("ENTERPRISE");
		assertThat(PRIVATE).hasToString("PRIVATE");
	}
}
