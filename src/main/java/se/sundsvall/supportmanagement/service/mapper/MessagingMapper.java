package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.detectMimeType;

import generated.se.sundsvall.messaging.EmailAttachment;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailRequestParty;
import generated.se.sundsvall.messaging.EmailSender;
import generated.se.sundsvall.messaging.ExternalReference;
import generated.se.sundsvall.messaging.SmsRequest;
import generated.se.sundsvall.messaging.SmsRequestParty;
import generated.se.sundsvall.messaging.WebMessageAttachment;
import generated.se.sundsvall.messaging.WebMessageParty;
import generated.se.sundsvall.messaging.WebMessageRequest;
import generated.se.sundsvall.messaging.WebMessageSender;
import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

public class MessagingMapper {

	private static final Logger LOG = LoggerFactory.getLogger(MessagingMapper.class);
	private static final String ERRAND_ID = "errandId";
	private static final String CHANNEL_ESERVICE = "ESERVICE";
	private static final String CHANNEL_ESERVICE_INTERNAL = "ESERVICE_INTERNAL";
	private static final String CASE_ID_KEY = "caseId";
	private static final String FLOW_INSTANCE_ID_KEY = "flowInstanceId";
	private static final Decoder BASE64_DECODER = Base64.getDecoder();
	private static final Encoder BASE64_ENCODER = Base64.getEncoder();

	private MessagingMapper() {}

	public static EmailRequest toEmailRequest(ErrandEntity errandEntity, se.sundsvall.supportmanagement.api.model.communication.EmailRequest emailRequest, List<EmailAttachment> attachments) {
		return new EmailRequest()
			.attachments(Stream.of(attachments, toAttachments(emailRequest.getAttachments()))
				.flatMap(List::stream)
				.toList())
			.emailAddress(emailRequest.getRecipient())
			.htmlMessage(addBase64Encoding(emailRequest.getHtmlMessage()))
			.message(emailRequest.getMessage())
			.party(toEmailRequestParty(errandEntity))
			.sender(toEmailSender(emailRequest))
			.headers(toEmailHeaders(emailRequest.getEmailHeaders()))
			.subject(emailRequest.getSubject());
	}

	public static Map<String, List<String>> toEmailHeaders(Map<EmailHeader, List<String>> emailHeaders) {
		return ofNullable(emailHeaders).orElse(Map.of()).entrySet().stream()
			.collect(Collectors.toMap(
				e -> e.getKey().toString(),
				Map.Entry::getValue));
	}

	public static SmsRequest toSmsRequest(ErrandEntity errandEntity, se.sundsvall.supportmanagement.api.model.communication.SmsRequest smsRequest) {
		return new SmsRequest()
			.message(smsRequest.getMessage())
			.mobileNumber(smsRequest.getRecipient())
			.party(toSmsRequestParty(errandEntity))
			.sender(smsRequest.getSender());
	}

	public static List<EmailAttachment> toEmailAttachments(final List<AttachmentEntity> attachments) {
		return ofNullable(attachments).orElse(emptyList()).stream()
			.map(MessagingMapper::toEmailAttachment)
			.toList();
	}

	public static WebMessageRequest toWebMessageRequest(ErrandEntity errandEntity, se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest webMessageRequest,
		final List<AttachmentEntity> attachments, final String senderUserId) {
		return new WebMessageRequest()
			.message(webMessageRequest.getMessage())
			.party(toWebMessageRequestParty(errandEntity))
			.sender(toWebMessageRequestSender(senderUserId))
			.oepInstance(toOepInstance(errandEntity.getChannel()))
			.attachments(Stream.of(
				toWebMessageAttachmentsFromAttachmentEntity(attachments),
				toWebMessageAttachmentsFromRequest(webMessageRequest.getAttachments()))
				.flatMap(List::stream)
				.toList());
	}

	private static List<WebMessageAttachment> toWebMessageAttachmentsFromAttachmentEntity(List<AttachmentEntity> attachments) {
		return ofNullable(attachments).orElse(emptyList()).stream()
			.map(MessagingMapper::toWebMessageAttachment)
			.toList();
	}

	private static List<WebMessageAttachment> toWebMessageAttachmentsFromRequest(List<se.sundsvall.supportmanagement.api.model.communication.WebMessageAttachment> attachments) {
		return ofNullable(attachments).orElse(emptyList()).stream()
			.map(MessagingMapper::toWebMessageAttachment)
			.toList();
	}

	private static WebMessageRequest.OepInstanceEnum toOepInstance(String channel) {
		return switch (channel) {
			case CHANNEL_ESERVICE -> WebMessageRequest.OepInstanceEnum.EXTERNAL;
			case CHANNEL_ESERVICE_INTERNAL -> WebMessageRequest.OepInstanceEnum.INTERNAL;
			default -> throw Problem.valueOf(INTERNAL_SERVER_ERROR, String.format("Mapping is only possible when errand is created via channel '%s' or '%s'", CHANNEL_ESERVICE, CHANNEL_ESERVICE_INTERNAL));
		};
	}

	static EmailAttachment toEmailAttachment(final AttachmentEntity attachment) {
		try (InputStream attachmentInputStream = attachment.getAttachmentData().getFile().getBinaryStream()) {
			final byte[] bytes = IOUtils.toByteArray(attachmentInputStream);
			final String encoded = Base64.getEncoder().encodeToString(bytes);

			return new EmailAttachment()
				.content(encoded)
				.contentType(detectMimeType(attachment.getFileName(), bytes))
				.name(attachment.getFileName());
		} catch (SQLException | IOException e) {
			LOG.error("Attachment mapping error", e);
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, String.format("Failed to map attachment with id '%s'", attachment.getId()));
		}
	}

	static WebMessageAttachment toWebMessageAttachment(final AttachmentEntity attachment) {
		try (InputStream attachmentInputStream = attachment.getAttachmentData().getFile().getBinaryStream()) {
			final byte[] bytes = IOUtils.toByteArray(attachmentInputStream);
			final String encoded = Base64.getEncoder().encodeToString(bytes);

			return new WebMessageAttachment()
				.base64Data(encoded)
				.mimeType(detectMimeType(attachment.getFileName(), bytes))
				.fileName(attachment.getFileName());
		} catch (SQLException | IOException e) {
			LOG.error("Attachment mapping error", e);
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, String.format("Failed to map attachment with id '%s'", attachment.getId()));
		}
	}

	private static List<EmailAttachment> toAttachments(List<se.sundsvall.supportmanagement.api.model.communication.EmailAttachment> attachments) {
		return ofNullable(attachments).orElse(emptyList()).stream()
			.map(MessagingMapper::toAttachment)
			.toList();
	}

	private static EmailAttachment toAttachment(se.sundsvall.supportmanagement.api.model.communication.EmailAttachment attachment) {
		final byte[] byteArray = decodeBase64(attachment.getBase64EncodedString());

		return new EmailAttachment()
			.content(attachment.getBase64EncodedString())
			.contentType(detectMimeType(attachment.getFileName(), byteArray))
			.name(attachment.getFileName());
	}

	private static WebMessageAttachment toWebMessageAttachment(final se.sundsvall.supportmanagement.api.model.communication.WebMessageAttachment attachment) {

		return new WebMessageAttachment()
			.base64Data(attachment.getBase64EncodedString())
			.mimeType(detectMimeType(attachment.getFileName(), decodeBase64(attachment.getBase64EncodedString())))
			.fileName(attachment.getFileName());
	}

	private static EmailRequestParty toEmailRequestParty(ErrandEntity errandEntity) {
		return new EmailRequestParty().addExternalReferencesItem(toExternalReference(errandEntity.getId()));
	}

	private static SmsRequestParty toSmsRequestParty(ErrandEntity errandEntity) {
		return new SmsRequestParty().addExternalReferencesItem(toExternalReference(errandEntity.getId()));
	}

	private static WebMessageParty toWebMessageRequestParty(ErrandEntity errandEntity) {
		return new WebMessageParty()
			.addExternalReferencesItem(toExternalReference(errandEntity.getId()))
			.addExternalReferencesItem(new ExternalReference()
				.key(FLOW_INSTANCE_ID_KEY)
				.value(ofNullable(errandEntity.getExternalTags()).orElse(emptyList()).stream()
					.filter(tag -> CASE_ID_KEY.equals(tag.getKey()))
					.map(DbExternalTag::getValue)
					.findFirst()
					.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, String.format("Web message cannot be created without externalTag with key '%s'", CASE_ID_KEY)))));
	}

	private static WebMessageSender toWebMessageRequestSender(String senderUserId) {
		return Optional.ofNullable(senderUserId)
			.map(id -> new WebMessageSender().userId(senderUserId))
			.orElse(null);
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
		} catch (final Exception e) {
			return BASE64_ENCODER.encodeToString(message.getBytes(StandardCharsets.UTF_8));
		}
	}
}
