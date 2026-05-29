package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DbSubscriptionTargetTypeTest {

	@Test
	void enumValues() {
		assertThat(DbSubscriptionTargetType.values()).containsExactlyInAnyOrder(
			DbSubscriptionTargetType.ERRAND,
			DbSubscriptionTargetType.NAMESPACE);
	}

	@Test
	void enumToString() {
		assertThat(DbSubscriptionTargetType.ERRAND).hasToString("ERRAND");
		assertThat(DbSubscriptionTargetType.NAMESPACE).hasToString("NAMESPACE");
	}
}
