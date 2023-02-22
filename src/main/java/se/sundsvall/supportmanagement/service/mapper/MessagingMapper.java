package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.detectMimeType;

import java.util.List;

import generated.se.sundsvall.messaging.Email;
import generated.se.sundsvall.messaging.EmailAttachment;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.ExternalReference;
import generated.se.sundsvall.messaging.Party;
import generated.se.sundsvall.messaging.Sms;
import generated.se.sundsvall.messaging.SmsRequest;
import se.sundsvall.supportmanagement.integration.db.model.EmbeddableCustomer;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.util.ServiceUtil;

public class MessagingMapper {

	private static final String ERRAND_ID = "errandId";

	private MessagingMapper() {}

	public static EmailRequest toEmailRequest(ErrandEntity errandEntity, se.sundsvall.supportmanagement.api.model.communication.EmailRequest emailRequest) {
		return new EmailRequest()
			.attachments(toAttachments(emailRequest.getAttachments()))
			.emailAddress(emailRequest.getRecipient())
			.htmlMessage(emailRequest.getHtmlMessage())
			.message(emailRequest.getMessage())
			.party(toParty(errandEntity))
			.sender(toEmail(emailRequest))
			.subject(emailRequest.getSubject());
	}

	public static SmsRequest toSmsRequest(ErrandEntity errandEntity, se.sundsvall.supportmanagement.api.model.communication.SmsRequest smsRequest) {
		return new SmsRequest()
			.message(smsRequest.getMessage())
			.mobileNumber(smsRequest.getRecipient())
			.party(toParty(errandEntity))
			.sender(toSms(smsRequest));
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

	private static Party toParty(ErrandEntity errandEntity) {
		return ofNullable(errandEntity.getCustomer())
			.map(EmbeddableCustomer::getId)
			.filter(ServiceUtil::isValidUuid)
			.map(id -> toParty(id, errandEntity.getId()))
			.orElse(null);
	}

	private static Party toParty(String id, String errandId) {
		return new Party()
			.partyId(id)
			.externalReferences(List.of(toExternalReference(errandId)));
	}

	private static ExternalReference toExternalReference(String id) {
		return new ExternalReference()
			.key(ERRAND_ID)
			.value(id);
	}

	private static Email toEmail(se.sundsvall.supportmanagement.api.model.communication.EmailRequest emailRequest) {
		return new Email()
			.name(ofNullable(emailRequest.getSenderName()).orElse(emailRequest.getSender()))
			.address(emailRequest.getSender());
	}

	private static Sms toSms(se.sundsvall.supportmanagement.api.model.communication.SmsRequest smsRequest) {
		return new Sms()
			.name(smsRequest.getSender());
	}
}
