package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.RevisionType.CURRENT;
import static se.sundsvall.supportmanagement.service.RevisionType.PREVIOUS;

import org.junit.jupiter.api.Test;

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
