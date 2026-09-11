package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionOutcomeTest {

	@Test
	void enumValues() {
		assertThat(DecisionOutcome.values()).containsExactlyInAnyOrder(DecisionOutcome.APPROVAL, DecisionOutcome.PARTIAL_APPROVAL, DecisionOutcome.REJECTION, DecisionOutcome.DISMISSAL, DecisionOutcome.DISCONTINUATION, DecisionOutcome.OTHER);
	}

	@Test
	void enumToString() {
		assertThat(DecisionOutcome.APPROVAL).hasToString("APPROVAL");
		assertThat(DecisionOutcome.PARTIAL_APPROVAL).hasToString("PARTIAL_APPROVAL");
		assertThat(DecisionOutcome.REJECTION).hasToString("REJECTION");
		assertThat(DecisionOutcome.DISMISSAL).hasToString("DISMISSAL");
		assertThat(DecisionOutcome.DISCONTINUATION).hasToString("DISCONTINUATION");
		assertThat(DecisionOutcome.OTHER).hasToString("OTHER");
	}
}
