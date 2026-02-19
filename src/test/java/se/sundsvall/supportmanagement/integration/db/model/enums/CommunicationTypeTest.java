package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommunicationTypeTest {

	@Test
	void enumValues() {
		assertThat(CommunicationType.values()).containsExactlyInAnyOrder(
			CommunicationType.SMS,
			CommunicationType.EMAIL,
			CommunicationType.WEB_MESSAGE);
	}

	@Test
	void enumToString() {
		assertThat(CommunicationType.SMS).hasToString("SMS");
		assertThat(CommunicationType.EMAIL).hasToString("EMAIL");
		assertThat(CommunicationType.WEB_MESSAGE).hasToString("WEB_MESSAGE");
	}

}
