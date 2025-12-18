package se.sundsvall.supportmanagement.integration.db.model.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValueTypeTest {

	@Test
	void enumValues() {
		assertThat(ValueType.values()).containsExactlyInAnyOrder(
			ValueType.BOOLEAN,
			ValueType.INTEGER,
			ValueType.STRING);
	}

	@Test
	void enumToString() {
		assertThat(ValueType.BOOLEAN).hasToString("BOOLEAN");
		assertThat(ValueType.INTEGER).hasToString("INTEGER");
		assertThat(ValueType.STRING).hasToString("STRING");
	}
}
