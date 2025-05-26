package se.sundsvall.supportmanagement.api.model.communication.conversation;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageRequestTest {

	@Test
	void testBean() {
		assertThat(MessageRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var content = "content";
		final var inReplyToMessageId = UUID.randomUUID().toString();

		final var o = MessageRequest.create()
			.withContent(content)
			.withInReplyToMessageId(inReplyToMessageId);

		assertThat(o).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(o.getContent()).isEqualTo(content);
		assertThat(o.getInReplyToMessageId()).isEqualTo(inReplyToMessageId);

	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MessageRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new MessageRequest()).hasAllNullFieldsOrProperties();
	}
}
