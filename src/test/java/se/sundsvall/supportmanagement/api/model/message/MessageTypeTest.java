package se.sundsvall.supportmanagement.api.model.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessageTypeTest {


	@Test
	void enumValues() {
		assertThat(MessageType.values()).containsExactlyInAnyOrder(MessageType.SMS, MessageType.EMAIL);
	}

	@Test
	void enumToString() {
		assertThat(MessageType.SMS).hasToString("SMS");
		assertThat(MessageType.EMAIL).hasToString("EMAIL");
	}

}
