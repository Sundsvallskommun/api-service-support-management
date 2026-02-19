package se.sundsvall.supportmanagement.api.model.communication.conversation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class MessageTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Message.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var attachments = List.of(Attachment.create());
		final var content = "content";
		final var created = now();
		final var createdBy = Identifier.create();
		final var id = UUID.randomUUID().toString();
		final var inReplyToMessageId = UUID.randomUUID().toString();
		final var readBy = List.of(ReadBy.create());
		final var type = MessageType.USER_CREATED;

		final var o = Message.create()
			.withAttachments(attachments)
			.withContent(content)
			.withCreated(created)
			.withCreatedBy(createdBy)
			.withId(id)
			.withInReplyToMessageId(inReplyToMessageId)
			.withReadBy(readBy)
			.withType(type);

		assertThat(o).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(o.getAttachments()).isEqualTo(attachments);
		assertThat(o.getContent()).isEqualTo(content);
		assertThat(o.getCreated()).isEqualTo(created);
		assertThat(o.getCreatedBy()).isEqualTo(createdBy);
		assertThat(o.getId()).isEqualTo(id);
		assertThat(o.getInReplyToMessageId()).isEqualTo(inReplyToMessageId);
		assertThat(o.getReadBy()).isEqualTo(readBy);
		assertThat(o.getType()).isEqualTo(type);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Message.create()).hasAllNullFieldsOrProperties();
		assertThat(new Message()).hasAllNullFieldsOrProperties();
	}
}
