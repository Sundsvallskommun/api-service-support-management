package se.sundsvall.supportmanagement.integration.db.model.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommunicationTypeTest {
	
	@Test
	void enumValues() {
		assertThat(CommunicationType.values()).containsExactlyInAnyOrder(CommunicationType.SMS, CommunicationType.EMAIL);
	}

	@Test
	void enumToString() {
		assertThat(CommunicationType.SMS).hasToString("SMS");
		assertThat(CommunicationType.EMAIL).hasToString("EMAIL");
	}

}
