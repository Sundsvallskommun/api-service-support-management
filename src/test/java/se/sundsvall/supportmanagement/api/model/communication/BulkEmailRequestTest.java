package se.sundsvall.supportmanagement.api.model.communication;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class BulkEmailRequestTest {

	@Test
	void testBean() {
		assertThat(BulkEmailRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var sender = "sender@sender.se";
		final var senderName = "Sender Name";
		final var recipients = List.of("recipient1@example.com", "recipient2@example.com");
		final var subject = "Subject";
		final var htmlMessage = "<html>message</html>";
		final var message = "message";
		final var emailHeaders = Map.of(EmailHeader.REFERENCES, List.of("reference1"));
		final var attachments = List.of(EmailAttachment.create());
		final var attachmentIds = List.of("id1", "id2");

		final var bean = BulkEmailRequest.create()
			.withSender(sender)
			.withSenderName(senderName)
			.withRecipients(recipients)
			.withSubject(subject)
			.withHtmlMessage(htmlMessage)
			.withMessage(message)
			.withEmailHeaders(emailHeaders)
			.withAttachments(attachments)
			.withAttachmentIds(attachmentIds);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getSender()).isEqualTo(sender);
		assertThat(bean.getSenderName()).isEqualTo(senderName);
		assertThat(bean.getRecipients()).isEqualTo(recipients);
		assertThat(bean.getSubject()).isEqualTo(subject);
		assertThat(bean.getHtmlMessage()).isEqualTo(htmlMessage);
		assertThat(bean.getMessage()).isEqualTo(message);
		assertThat(bean.getEmailHeaders()).isEqualTo(emailHeaders);
		assertThat(bean.getAttachments()).isEqualTo(attachments);
		assertThat(bean.getAttachmentIds()).isEqualTo(attachmentIds);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(BulkEmailRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new BulkEmailRequest()).hasAllNullFieldsOrProperties();
	}
}
