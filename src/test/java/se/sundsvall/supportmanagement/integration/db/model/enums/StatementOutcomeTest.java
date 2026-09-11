package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatementOutcomeTest {

	@Test
	void enumValues() {
		assertThat(StatementOutcome.values()).containsExactlyInAnyOrder(StatementOutcome.SUPPORTS, StatementOutcome.SUPPORTS_WITH_CONDITIONS, StatementOutcome.NO_OBJECTION, StatementOutcome.OPPOSES, StatementOutcome.NOT_APPLICABLE,
			StatementOutcome.NO_RESPONSE);
	}

	@Test
	void enumToString() {
		assertThat(StatementOutcome.SUPPORTS).hasToString("SUPPORTS");
		assertThat(StatementOutcome.SUPPORTS_WITH_CONDITIONS).hasToString("SUPPORTS_WITH_CONDITIONS");
		assertThat(StatementOutcome.NO_OBJECTION).hasToString("NO_OBJECTION");
		assertThat(StatementOutcome.OPPOSES).hasToString("OPPOSES");
		assertThat(StatementOutcome.NOT_APPLICABLE).hasToString("NOT_APPLICABLE");
		assertThat(StatementOutcome.NO_RESPONSE).hasToString("NO_RESPONSE");
	}
}
