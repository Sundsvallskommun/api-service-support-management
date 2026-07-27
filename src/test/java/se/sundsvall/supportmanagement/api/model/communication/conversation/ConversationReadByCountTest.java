package se.sundsvall.supportmanagement.api.model.communication.conversation;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ConversationReadByCountTest {

	@Test
	void testBean() {
		assertThat(ConversationReadByCount.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var conversationId = "2af1002e-008f-4bdc-924b-daaae31f1118";
		final var messageCount = 10;
		final var readByCount = List.of(ReadByCountEntry.create().withIdentifier(Identifier.create().withType("adAccount").withValue("joe01doe")).withCount(5));
		final var readByPartCount = List.of(PartReadByCountEntry.create().withPart("KC-23010001").withCount(8));

		final var o = ConversationReadByCount.create()
			.withConversationId(conversationId)
			.withMessageCount(messageCount)
			.withReadByCount(readByCount)
			.withReadByPartCount(readByPartCount);

		assertThat(o).isNotNull();
		assertThat(o.getConversationId()).isEqualTo(conversationId);
		assertThat(o.getMessageCount()).isEqualTo(messageCount);
		assertThat(o.getReadByCount()).isEqualTo(readByCount);
		assertThat(o.getReadByPartCount()).isEqualTo(readByPartCount);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ConversationReadByCount.create()).hasAllNullFieldsOrProperties();
		assertThat(new ConversationReadByCount()).hasAllNullFieldsOrProperties();
	}
}
