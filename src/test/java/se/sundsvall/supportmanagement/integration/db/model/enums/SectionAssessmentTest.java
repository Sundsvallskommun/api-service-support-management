package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SectionAssessmentTest {

	@Test
	void enumValues() {
		assertThat(SectionAssessment.values()).containsExactlyInAnyOrder(SectionAssessment.PENDING, SectionAssessment.APPROVED, SectionAssessment.DEFICIENCY, SectionAssessment.NOT_APPLICABLE);
	}

	@Test
	void enumToString() {
		assertThat(SectionAssessment.PENDING).hasToString("PENDING");
		assertThat(SectionAssessment.APPROVED).hasToString("APPROVED");
		assertThat(SectionAssessment.DEFICIENCY).hasToString("DEFICIENCY");
		assertThat(SectionAssessment.NOT_APPLICABLE).hasToString("NOT_APPLICABLE");
	}
}
