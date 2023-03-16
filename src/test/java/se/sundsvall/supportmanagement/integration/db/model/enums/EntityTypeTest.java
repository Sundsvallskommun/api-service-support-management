package se.sundsvall.supportmanagement.integration.db.model.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EntityTypeTest {

	@Test
	void enumValues() {
		assertThat(EntityType.values()).containsExactlyInAnyOrder(EntityType.CATEGORY, EntityType.EXTERNAL_ID_TYPE, EntityType.STATUS, EntityType.TYPE);
	}

	@Test
	void enumToString() {
		assertThat(EntityType.CATEGORY).hasToString("CATEGORY");
		assertThat(EntityType.EXTERNAL_ID_TYPE).hasToString("EXTERNAL_ID_TYPE");
		assertThat(EntityType.STATUS).hasToString("STATUS");
		assertThat(EntityType.TYPE).hasToString("TYPE");
	}
}
