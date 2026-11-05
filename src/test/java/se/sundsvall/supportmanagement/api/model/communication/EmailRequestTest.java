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
		final var recipients = List.of("recipient1", "recipient2");
		final var cc = List.of("cc1", "cc2");
		final var sender = "sender";
		final var senderName = "senderName";
		final var subject = "subject";
		final var attachmentsIds = List.of("attachment1", "attachment2");
		final var emailHeaders = Map.of(EmailHeader.REFERENCES, List.of("reference1", "reference2"));
		final var internal = true;
		// Act
		final var bean = EmailRequest.create()
			.withAttachments(attachments)
			.withHtmlMessage(htmlMessage)
			.withMessage(message)
			.withRecipient(recipient)
			.withRecipients(recipients)
			.withCc(cc)
			.withSender(sender)
			.withSenderName(senderName)
			.withSubject(subject)
			.withEmailHeaders(emailHeaders)
			.withAttachmentIds(attachmentsIds)
			.withInternal(internal);
		// Assert
		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getAttachments()).isEqualTo(attachments);
		assertThat(bean.getHtmlMessage()).isEqualTo(htmlMessage);
		assertThat(bean.getMessage()).isEqualTo(message);
		assertThat(bean.getRecipient()).isEqualTo(recipient);
		assertThat(bean.getRecipients()).isEqualTo(recipients);
		assertThat(bean.getCc()).isEqualTo(cc);
		assertThat(bean.getSender()).isEqualTo(sender);
		assertThat(bean.getSenderName()).isEqualTo(senderName);
		assertThat(bean.getSubject()).isEqualTo(subject);
		assertThat(bean.getEmailHeaders()).isEqualTo(emailHeaders);
		assertThat(bean.getAttachmentIds()).isEqualTo(attachmentsIds);
		assertThat(bean.getInternal()).isEqualTo(internal);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EmailRequest.create()).hasAllNullFieldsOrPropertiesExcept("internal");
		assertThat(new EmailRequest()).hasAllNullFieldsOrPropertiesExcept("internal");
	}

	@Test
	void allRecipientsCombinesRecipientAndRecipientsWithoutBlanksAndDuplicates() {
		final var emailRequest = EmailRequest.create()
			.withRecipient("first@example.com")
			.withRecipients(List.of("", "second@example.com", " ", "first@example.com"));

		assertThat(emailRequest.allRecipients()).containsExactly("first@example.com", "second@example.com");
	}

	@Test
	void allRecipientsLeavesOutBlankRecipient() {
		final var emailRequest = EmailRequest.create()
			.withRecipient("")
			.withRecipients(List.of("first@example.com"));

		assertThat(emailRequest.allRecipients()).containsExactly("first@example.com");
	}

	@Test
	void allCcWithoutBlanksAndDuplicates() {
		final var emailRequest = EmailRequest.create()
			.withCc(List.of("", "cc@example.com", "cc@example.com"));

		assertThat(emailRequest.allCc()).containsExactly("cc@example.com");
	}

	@Test
	void allRecipientsAndAllCcWhenNoneAreSet() {
		final var emailRequest = EmailRequest.create();

		assertThat(emailRequest.allRecipients()).isEmpty();
		assertThat(emailRequest.allCc()).isEmpty();
	}
}
