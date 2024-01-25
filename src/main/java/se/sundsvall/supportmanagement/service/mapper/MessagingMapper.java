package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.detectMimeType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.List;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import generated.se.sundsvall.messaging.EmailAttachment;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailRequestParty;
import generated.se.sundsvall.messaging.EmailSender;
import generated.se.sundsvall.messaging.ExternalReference;
import generated.se.sundsvall.messaging.SmsRequest;
import generated.se.sundsvall.messaging.SmsRequestParty;
import io.netty.util.internal.StringUtil;

public class MessagingMapper {

	private static final String ERRAND_ID = "errandId";
	private static final Decoder BASE64_DECODER = Base64.getDecoder();
	private static final Encoder BASE64_ENCODER = Base64.getEncoder();

	private MessagingMapper() {}

	public static EmailRequest toEmailRequest(ErrandEntity errandEntity, se.sundsvall.supportmanagement.api.model.communication.EmailRequest emailRequest) {
		return new EmailRequest()
			.attachments(toAttachments(emailRequest.getAttachments()))
			.emailAddress(emailRequest.getRecipient())
			.htmlMessage(addBase64Encoding(emailRequest.getHtmlMessage()))
			.message(emailRequest.getMessage())
			.party(toEmailRequestParty(errandEntity))
			.sender(toEmailSender(emailRequest))
			.subject(emailRequest.getSubject());
	}

	public static SmsRequest toSmsRequest(ErrandEntity errandEntity, se.sundsvall.supportmanagement.api.model.communication.SmsRequest smsRequest) {
		return new SmsRequest()
			.message(smsRequest.getMessage())
			.mobileNumber(smsRequest.getRecipient())
			.party(toSmsRequestParty(errandEntity))
			.sender(smsRequest.getSender());
	}

	private static List<EmailAttachment> toAttachments(List<se.sundsvall.supportmanagement.api.model.communication.EmailAttachment> attachments) {
		return ofNullable(attachments).orElse(emptyList()).stream()
			.map(MessagingMapper::toAttachment)
			.toList();
	}

	private static EmailAttachment toAttachment(se.sundsvall.supportmanagement.api.model.communication.EmailAttachment attachment) {
		byte[] byteArray = decodeBase64(attachment.getBase64EncodedString());

		return new EmailAttachment()
			.content(attachment.getBase64EncodedString())
			.contentType(detectMimeType(attachment.getName(), byteArray))
			.name(attachment.getName());
	}

	private static EmailRequestParty toEmailRequestParty(ErrandEntity errandEntity) {
		return new EmailRequestParty().addExternalReferencesItem(toExternalReference(errandEntity.getId()));
	}

	private static SmsRequestParty toSmsRequestParty(ErrandEntity errandEntity) {
		return new SmsRequestParty().addExternalReferencesItem(toExternalReference(errandEntity.getId()));
	}

	private static ExternalReference toExternalReference(String id) {
		return new ExternalReference()
			.key(ERRAND_ID)
			.value(id);
	}

	private static EmailSender toEmailSender(se.sundsvall.supportmanagement.api.model.communication.EmailRequest emailRequest) {
		return new EmailSender()
			.name(ofNullable(emailRequest.getSenderName()).orElse(emailRequest.getSender()))
			.address(emailRequest.getSender());
	}

	private static String addBase64Encoding(String message) {

		if (StringUtil.isNullOrEmpty(message)) {
			return message;
		}
		try {
			BASE64_DECODER.decode(message.getBytes(StandardCharsets.UTF_8));
			return message; // If decoding passes, the message is already in base64 format
		} catch (Exception e) {
			return BASE64_ENCODER.encodeToString(message.getBytes(StandardCharsets.UTF_8));
		}
	}
}
