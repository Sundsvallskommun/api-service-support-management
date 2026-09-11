package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeasureResultTest {

	@Test
	void enumValues() {
		assertThat(MeasureResult.values()).containsExactlyInAnyOrder(MeasureResult.COMPLETED, MeasureResult.PARTIALLY_COMPLETED, MeasureResult.NOT_COMPLETED, MeasureResult.NOT_APPLICABLE);
	}

	@Test
	void enumToString() {
		assertThat(MeasureResult.COMPLETED).hasToString("COMPLETED");
		assertThat(MeasureResult.PARTIALLY_COMPLETED).hasToString("PARTIALLY_COMPLETED");
		assertThat(MeasureResult.NOT_COMPLETED).hasToString("NOT_COMPLETED");
		assertThat(MeasureResult.NOT_APPLICABLE).hasToString("NOT_APPLICABLE");
	}
}
