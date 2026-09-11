package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionMethodTest {

	@Test
	void enumValues() {
		assertThat(DecisionMethod.values()).containsExactlyInAnyOrder(DecisionMethod.MANUAL, DecisionMethod.AUTOMATIC);
	}

	@Test
	void enumToString() {
		assertThat(DecisionMethod.MANUAL).hasToString("MANUAL");
		assertThat(DecisionMethod.AUTOMATIC).hasToString("AUTOMATIC");
	}
}
