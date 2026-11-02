package se.sundsvall.supportmanagement.integration.messageexchange.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.supportmanagement.ApplicationTest;
import se.sundsvall.supportmanagement.integration.messaging.configuration.MessagingProperties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ApplicationTest
class MessageExchangePropertiesTest {

	@Autowired
	private MessagingProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}
}
