package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTypeTest {

	@Test
	void enumValues() {
		assertThat(EntityType.values()).containsExactlyInAnyOrder(EntityType.CATEGORY, EntityType.EXTERNAL_ID_TYPE, EntityType.STATUS, EntityType.TYPE, EntityType.ROLE, EntityType.CONTACT_REASON);
	}

	@Test
	void enumToString() {
		assertThat(EntityType.CATEGORY).hasToString("CATEGORY");
		assertThat(EntityType.EXTERNAL_ID_TYPE).hasToString("EXTERNAL_ID_TYPE");
		assertThat(EntityType.STATUS).hasToString("STATUS");
		assertThat(EntityType.TYPE).hasToString("TYPE");
		assertThat(EntityType.ROLE).hasToString("ROLE");
		assertThat(EntityType.CONTACT_REASON).hasToString("CONTACT_REASON");
	}
}
