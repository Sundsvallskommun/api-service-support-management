package se.sundsvall.supportmanagement.api.model.communication.conversation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType.EXTERNAL;
import static se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType.INTERNAL;

class ConversationTypeTest {

	@Test
	void enums() {
		assertThat(ConversationType.values()).containsExactlyInAnyOrder(EXTERNAL, INTERNAL);
	}
}
