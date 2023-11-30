package se.sundsvall.supportmanagement.api.model.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
