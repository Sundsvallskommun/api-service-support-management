package se.sundsvall.supportmanagement.integration.db.model.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TagTypeTest {

	@Test
	void enumValues() {
		assertThat(TagType.values()).containsExactlyInAnyOrder(TagType.CATEGORY, TagType.EXTERNAL_ID_TYPE, TagType.STATUS, TagType.TYPE);
	}

	@Test
	void enumToString() {
		assertThat(TagType.CATEGORY).hasToString("CATEGORY");
		assertThat(TagType.EXTERNAL_ID_TYPE).hasToString("EXTERNAL_ID_TYPE");
		assertThat(TagType.STATUS).hasToString("STATUS");
		assertThat(TagType.TYPE).hasToString("TYPE");
	}
}
