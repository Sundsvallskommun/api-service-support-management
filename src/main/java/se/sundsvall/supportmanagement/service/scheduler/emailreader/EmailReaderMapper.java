package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toMap;

import generated.se.sundsvall.emailreader.Email;
import generated.se.sundsvall.emailreader.EmailAttachment;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.ContactChannel;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEmailHeaderEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;
import se.sundsvall.supportmanagement.service.util.BlobBuilder;

@Component
public class EmailReaderMapper {

	private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

	private final BlobBuilder blobBuilder;

	public EmailReaderMapper(final BlobBuilder blobBuilder) {
		this.blobBuilder = blobBuilder;
	}

	List<AttachmentEntity> toAttachments(final Email email) {
		if (isNull(email)) {
			return emptyList();
		}
		return Optional.ofNullable(email.getAttachments()).orElse(emptyList())
			.stream()
			.map(emailAttachment -> AttachmentEntity.create()
				.withId(randomUUID().toString())
				.withAttachmentData(AttachmentDataEntity.create().withFile(blobBuilder.createBlob(emailAttachment.getContent())))
				.withFileName(emailAttachment.getName())
				.withMimeType(emailAttachment.getContentType()))
			.toList();
	}

	CommunicationEntity toCommunicationEntity(final Email email, final ErrandEntity errand) {
		if (isNull(email)) {
			return null;
		}
		return CommunicationEntity.create()
			.withNamespace(errand.getNamespace())
			.withMunicipalityId(errand.getMunicipalityId())
			.withErrandNumber(errand.getErrandNumber())
			.withDirection(Direction.INBOUND)
			.withExternalId("")
			.withSubject(email.getSubject())
			.withMessageBody(email.getMessage())
			.withSender(email.getSender())
			.withSent(email.getReceivedAt())
			.withType(CommunicationType.EMAIL)
			.withTarget(Optional.ofNullable(email.getRecipients()).orElse(emptyList()).stream().findFirst().orElse(null))
			.withEmailHeaders(toEmailHeaders(email))
			.withAttachments(toMessageAttachments(email.getAttachments(), errand));
	}

	private List<CommunicationEmailHeaderEntity> toEmailHeaders(final Email email) {
		return Optional.ofNullable(email.getHeaders())
			.orElseGet(Collections::emptyMap)
			.entrySet()
			.stream()
			.map(entry -> CommunicationEmailHeaderEntity.create()
				.withHeader(EmailHeader.valueOf(entry.getKey()))
				.withValues(entry.getValue()))
			.toList();
	}

	private List<CommunicationAttachmentEntity> toMessageAttachments(final List<EmailAttachment> attachments, final ErrandEntity errand) {
		return Optional.ofNullable(attachments).orElse(Collections.emptyList()).stream()
			.map(attachment -> CommunicationAttachmentEntity.create()
				.withMunicipalityId(errand.getMunicipalityId())
				.withNamespace(errand.getNamespace())
				.withName(attachment.getName())
				.withFileSize(getFileSize(attachment.getContent()))
				.withAttachmentData(toMessageAttachmentData(attachment))
				.withContentType(attachment.getContentType()))
			.toList();
	}

	private CommunicationAttachmentDataEntity toMessageAttachmentData(final EmailAttachment attachment) {
		return CommunicationAttachmentDataEntity.create()
			.withFile(blobBuilder.createBlob(attachment.getContent()));
	}

	public Errand toErrand(final Email email, final String status, final boolean addSenderAsStakeholder,
		final String stakeholderRole, final String errandChannel) {

		final var errand = Errand.create()
			.withTitle(email.getSubject())
			.withDescription(email.getMessage())
			.withStatus(status)
			.withPriority(Priority.MEDIUM)
			.withChannel(errandChannel)
			.withClassification(Classification.create().withCategory(email.getMetadata().get("classification.category")).withType(email.getMetadata().get("classification.type")));

		if (StringUtils.hasText(email.getMetadata().get("labels"))) {
			errand.setLabels(Arrays.stream(email.getMetadata().get("labels").split(";")).toList());
		}

		if (addSenderAsStakeholder) {
			errand.withStakeholders(List.of(
				Stakeholder.create()
					.withRole(stakeholderRole)
					.withContactChannels(List.of(
						ContactChannel.create().withType("EMAIL").withValue(email.getSender())))));
		}

		return errand;
	}

	public EmailRequest createEmailRequest(final Email email, final String sender, final String messageTemplate, final String subject) {
		return EmailRequest.create()
			.withSubject(subject)
			.withRecipient(email.getSender()) // Because we are sending a return response
			.withEmailHeaders(toEmailHeaderMap(email))
			.withSender(sender)
			.withSenderName(sender)
			.withMessage(messageTemplate);
	}

	private Map<EmailHeader, List<String>> toEmailHeaderMap(final Email email) {

		final var map = email.getHeaders().entrySet().stream()
			.collect(toMap(
				entry -> EmailHeader.valueOf(entry.getKey()), Map.Entry::getValue, (oldValue, newValue) -> newValue));

		final var messageId = Optional.ofNullable(map.get(EmailHeader.MESSAGE_ID))
			.flatMap(list -> list.stream().findFirst())
			.orElse(null);

		if (messageId != null) {
			map.put(EmailHeader.IN_REPLY_TO, List.of(messageId));

			Optional.ofNullable(map.get(EmailHeader.REFERENCES))
				.ifPresentOrElse(references -> references.add(messageId),
					() -> map.put(EmailHeader.REFERENCES, List.of(messageId)));

			map.remove(EmailHeader.MESSAGE_ID);
		}

		return map;
	}

	private int getFileSize(String base64Content) {
		return BASE64_DECODER.decode(Optional.ofNullable(base64Content).orElse("").getBytes(UTF_8)).length;
	}
}
