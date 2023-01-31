package se.sundsvall.supportmanagement.api.model.messaging;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class EmailRequestTest {

	@Test
	void testBean() {
		assertThat(EmailRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var attachments = List.of(EmailAttachment.create());
		final var message = "message";
		final var recipient = "recipient";
		final var sender = "sender";
		final var subject = "subject";

		final var bean = EmailRequest.create()
			.withAttachments(attachments)
			.withMessage(message)
			.withRecipient(recipient)
			.withSender(sender)
			.withSubject(subject);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getAttachments()).isEqualTo(attachments);
		assertThat(bean.getMessage()).isEqualTo(message);
		assertThat(bean.getRecipient()).isEqualTo(recipient);
		assertThat(bean.getSender()).isEqualTo(sender);
		assertThat(bean.getSubject()).isEqualTo(subject);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EmailRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new EmailRequest()).hasAllNullFieldsOrProperties();
	}
}
