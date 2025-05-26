package se.sundsvall.supportmanagement.api.model.communication.conversation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType.D2C;
import static se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType.D2D;

import org.junit.jupiter.api.Test;

class ConversationTypeTest {

	@Test
	void enums() {
		assertThat(ConversationType.values()).containsExactlyInAnyOrder(D2C, D2D);
	}
}
