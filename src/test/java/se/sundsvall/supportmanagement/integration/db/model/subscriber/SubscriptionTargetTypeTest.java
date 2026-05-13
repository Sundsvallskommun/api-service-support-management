package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTargetTypeTest {

	@Test
	void enumValues() {
		assertThat(SubscriptionTargetType.values()).containsExactlyInAnyOrder(
			SubscriptionTargetType.ERRAND,
			SubscriptionTargetType.NAMESPACE);
	}

	@Test
	void enumToString() {
		assertThat(SubscriptionTargetType.ERRAND).hasToString("ERRAND");
		assertThat(SubscriptionTargetType.NAMESPACE).hasToString("NAMESPACE");
	}
}
