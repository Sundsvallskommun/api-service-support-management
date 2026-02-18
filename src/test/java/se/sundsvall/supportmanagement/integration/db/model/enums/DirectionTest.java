package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DirectionTest {

	@Test
	void enumValues() {
		assertThat(Direction.values()).containsExactlyInAnyOrder(Direction.INBOUND, Direction.OUTBOUND);
	}

	@Test
	void enumToString() {
		assertThat(Direction.INBOUND).hasToString("INBOUND");
		assertThat(Direction.OUTBOUND).hasToString("OUTBOUND");
	}

}
