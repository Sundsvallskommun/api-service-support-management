package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationChannelTypeTest {

	@Test
	void enumValues() {
		assertThat(NotificationChannelType.values()).containsExactlyInAnyOrder(
			NotificationChannelType.INTERNAL,
			NotificationChannelType.EMAIL,
			NotificationChannelType.SMS);
	}

	@Test
	void enumToString() {
		assertThat(NotificationChannelType.INTERNAL).hasToString("INTERNAL");
		assertThat(NotificationChannelType.EMAIL).hasToString("EMAIL");
		assertThat(NotificationChannelType.SMS).hasToString("SMS");
	}
}
