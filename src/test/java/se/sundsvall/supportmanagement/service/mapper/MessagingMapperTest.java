package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Map.entry;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.when;
import static org.springframework.util.MimeTypeUtils.IMAGE_PNG_VALUE;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

import generated.se.sundsvall.messaging.ExternalReference;

class MessagingMapperTest {

	private static final Decoder BASE64_DECODER = Base64.getDecoder();
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String HTML_MESSAGE = "<html>htmlMessage</html>";
	private static final String HTML_MESSAGE_IN_BASE64 = "PGh0bWw+aHRtbE1lc3NhZ2U8L2h0bWw+";
	private static final String MESSAGE = "message";
	private static final String RECIPIENT = "recipient";
	private static final String SENDER_EMAIL = "sender@sender.com";
	private static final String SENDER_NAME = "senderName";
	private static final String SUBJECT = "subject";
	private static final String FILE_CONTENT = "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==";
	private static final String FILE_NAME = "fileName";
	private static final String ERRAND_ID_KEY = "errandId";

	@Test
	void toEmailRequestWithSenderName() {
		final var result = MessagingMapper.toEmailRequest(createErrandEntity(), createEmailRequest(true, HTML_MESSAGE_IN_BASE64), List.of());

		assertThat(result.getEmailAddress()).isEqualTo(RECIPIENT);
		assertThat(new String(BASE64_DECODER.decode(result.getHtmlMessage()), StandardCharsets.UTF_8)).isEqualTo(HTML_MESSAGE);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getSubject()).isEqualTo(SUBJECT);
		assertThat(result.getSender().getAddress()).isEqualTo(SENDER_EMAIL);
		assertThat(result.getSender().getName()).isEqualTo(SENDER_NAME);
		assertThat(result.getSender().getReplyTo()).isNull();
		assertThat(result.getParty().getPartyId()).isNull();
		assertThat(result.getParty().getExternalReferences())
			.extracting(ExternalReference::getKey, ExternalReference::getValue)
			.contains(tuple(ERRAND_ID_KEY, ERRAND_ID));
		assertThat(result.getHeaders()).contains(entry(EmailHeader.MESSAGE_ID.toString(), List.of("this-is@message-id")));
		assertThat(result.getHeaders()).contains(entry(EmailHeader.IN_REPLY_TO.toString(), List.of("another@message-id")));
		assertThat(result.getHeaders()).contains(entry(EmailHeader.REFERENCES.toString(), List.of("valid@message-id", "also-valid@message-id")));

		assertThat(result.getAttachments()).isNotNull().extracting(
			generated.se.sundsvall.messaging.EmailAttachment::getContent,
			generated.se.sundsvall.messaging.EmailAttachment::getContentType,
			generated.se.sundsvall.messaging.EmailAttachment::getName).containsExactly(tuple(FILE_CONTENT, IMAGE_PNG_VALUE, FILE_NAME));
	}

	@Test
	void toEmailRequestWithoutSenderName() {
		final var result = MessagingMapper.toEmailRequest(createErrandEntity(), createEmailRequest(false, HTML_MESSAGE), List.of());

		assertThat(result.getEmailAddress()).isEqualTo(RECIPIENT);
		assertThat(new String(BASE64_DECODER.decode(result.getHtmlMessage()), StandardCharsets.UTF_8)).isEqualTo(HTML_MESSAGE);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getSubject()).isEqualTo(SUBJECT);
		assertThat(result.getSender().getAddress()).isEqualTo(SENDER_EMAIL);
		assertThat(result.getSender().getName()).isEqualTo(SENDER_EMAIL);
		assertThat(result.getSender().getReplyTo()).isNull();
		assertThat(result.getHeaders()).contains(entry(EmailHeader.MESSAGE_ID.toString(), List.of("this-is@message-id")));
		assertThat(result.getHeaders()).contains(entry(EmailHeader.IN_REPLY_TO.toString(), List.of("another@message-id")));
		assertThat(result.getHeaders()).contains(entry(EmailHeader.REFERENCES.toString(), List.of("valid@message-id", "also-valid@message-id")));
		assertThat(result.getAttachments()).isNotNull().extracting(
			generated.se.sundsvall.messaging.EmailAttachment::getContent,
			generated.se.sundsvall.messaging.EmailAttachment::getContentType,
			generated.se.sundsvall.messaging.EmailAttachment::getName).containsExactly(tuple(FILE_CONTENT, IMAGE_PNG_VALUE, FILE_NAME));
	}

	@Test
	void toSmsRequest() {
		final var result = MessagingMapper.toSmsRequest(createErrandEntity(), createSmsRequest());

		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getMobileNumber()).isEqualTo(RECIPIENT);
		assertThat(result.getSender()).isEqualTo(SENDER_NAME);
		assertThat(result.getParty().getPartyId()).isNull();
		assertThat(result.getParty().getExternalReferences())
			.extracting(ExternalReference::getKey, ExternalReference::getValue)
			.contains(tuple(ERRAND_ID_KEY, ERRAND_ID));
	}

	@Test
	void testToEmailAttachment() throws Exception {

		String originalContent = "This is a test";
		var contentBytes = originalContent.getBytes(StandardCharsets.UTF_8);
		var inputStream = new ByteArrayInputStream(contentBytes);
		var mockAttachment = Mockito.mock(AttachmentEntity.class);
		var mockAttachmentData = Mockito.mock(AttachmentDataEntity.class);
		Blob mockFile = Mockito.mock(Blob.class);

		when(mockAttachment.getAttachmentData()).thenReturn(mockAttachmentData);
		when(mockAttachmentData.getFile()).thenReturn(mockFile);
		when(mockFile.getBinaryStream()).thenReturn(inputStream);
		when(mockAttachment.getFileName()).thenReturn("test.txt");

		String expectedEncodedContent = Base64.getEncoder().encodeToString(contentBytes);
		String expectedContentType = "text/plain";
		String expectedFileName = "test.txt";

		var result = MessagingMapper.toEmailAttachment(mockAttachment);

		assertThat(result).isNotNull();
		assertThat(result.getContent()).isEqualTo(expectedEncodedContent);
		assertThat(result.getContentType()).isEqualTo(expectedContentType);
		assertThat(result.getName()).isEqualTo(expectedFileName);
	}

	private SmsRequest createSmsRequest() {
		return SmsRequest.create()
			.withMessage(MESSAGE)
			.withRecipient(RECIPIENT)
			.withSender(SENDER_NAME);
	}

	private static EmailRequest createEmailRequest(boolean hasSenderName, String htmlMessage) {
		return EmailRequest.create()
			.withAttachments(List.of(EmailAttachment.create()
				.withBase64EncodedString(FILE_CONTENT)
				.withName(FILE_NAME)))
			.withHtmlMessage(htmlMessage)
			.withMessage(MESSAGE)
			.withRecipient(RECIPIENT)
			.withSender(SENDER_EMAIL)
			.withSenderName(hasSenderName ? SENDER_NAME : null)
			.withEmailHeaders(createEmailHeaders())
			.withSubject(SUBJECT);
	}

	private static ErrandEntity createErrandEntity() {
		return ErrandEntity.create()
			.withId(ERRAND_ID);
	}

	private static Map<EmailHeader, List<String>> createEmailHeaders() {
		return Map.of(EmailHeader.MESSAGE_ID, List.of("this-is@message-id"),
			EmailHeader.IN_REPLY_TO, List.of("another@message-id"),
			EmailHeader.REFERENCES, List.of("valid@message-id", "also-valid@message-id"));
	}
}
