package se.sundsvall.supportmanagement.service.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.model.RevisionType.CURRENT;
import static se.sundsvall.supportmanagement.service.model.RevisionType.PREVIOUS;

class RevisionTypeTest {

	@Test
	void testRevisionTypeEnum() {
		assertThat(RevisionType.values()).containsExactlyInAnyOrder(
			CURRENT,
			PREVIOUS);
	}

	@Test
	void testRevisionTypeValues() {
		assertThat(CURRENT.getValue()).isEqualTo("current");
		assertThat(PREVIOUS.getValue()).isEqualTo("previous");
	}
}
