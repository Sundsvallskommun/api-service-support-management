package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcceptTest {

	@Test
	void enumValues() {
		assertThat(Accept.values()).containsExactlyInAnyOrder(Accept.TRUE, Accept.FALSE, Accept.REWORK);
	}

	@Test
	void enumToString() {
		assertThat(Accept.TRUE).hasToString("TRUE");
		assertThat(Accept.FALSE).hasToString("FALSE");
		assertThat(Accept.REWORK).hasToString("REWORK");
	}

}
