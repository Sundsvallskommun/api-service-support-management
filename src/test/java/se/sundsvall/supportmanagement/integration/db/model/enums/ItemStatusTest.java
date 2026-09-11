package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemStatusTest {

	@Test
	void enumValues() {
		assertThat(ItemStatus.values()).containsExactlyInAnyOrder(ItemStatus.DRAFT, ItemStatus.ACTIVE, ItemStatus.COMPLETED, ItemStatus.CANCELLED);
	}

	@Test
	void enumToString() {
		assertThat(ItemStatus.DRAFT).hasToString("DRAFT");
		assertThat(ItemStatus.ACTIVE).hasToString("ACTIVE");
		assertThat(ItemStatus.COMPLETED).hasToString("COMPLETED");
		assertThat(ItemStatus.CANCELLED).hasToString("CANCELLED");
	}
}
