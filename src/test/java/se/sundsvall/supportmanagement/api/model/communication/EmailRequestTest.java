package se.sundsvall.supportmanagement.api.model.communication;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

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
		// Arrange
		final var attachments = List.of(EmailAttachment.create());
		final var htmlMessage = "htmlMessage";
		final var message = "message";
		final var recipient = "recipient";
		final var sender = "sender";
		final var senderName = "senderName";
		final var subject = "subject";
		final var attachmentsIds = List.of("attachment1", "attachment2");
		final var emailHeaders = Map.of(EmailHeader.REFERENCES, List.of("reference1", "reference2"));
		// Act
		final var bean = EmailRequest.create()
			.withAttachments(attachments)
			.withHtmlMessage(htmlMessage)
			.withMessage(message)
			.withRecipient(recipient)
			.withSender(sender)
			.withSenderName(senderName)
			.withSubject(subject)
			.withEmailHeaders(emailHeaders)
			.withAttachmentIds(attachmentsIds);
		// Assert
		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getAttachments()).isEqualTo(attachments);
		assertThat(bean.getHtmlMessage()).isEqualTo(htmlMessage);
		assertThat(bean.getMessage()).isEqualTo(message);
		assertThat(bean.getRecipient()).isEqualTo(recipient);
		assertThat(bean.getSender()).isEqualTo(sender);
		assertThat(bean.getSenderName()).isEqualTo(senderName);
		assertThat(bean.getSubject()).isEqualTo(subject);
		assertThat(bean.getEmailHeaders()).isEqualTo(emailHeaders);
		assertThat(bean.getAttachmentIds()).isEqualTo(attachmentsIds);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EmailRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new EmailRequest()).hasAllNullFieldsOrProperties();
	}
}
