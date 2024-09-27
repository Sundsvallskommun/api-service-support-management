package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
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

import generated.se.sundsvall.emailreader.Email;
import generated.se.sundsvall.emailreader.EmailAttachment;

@Component
public class EmailReaderMapper {

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
			.withId(randomUUID().toString())
			.withNamespace(errand.getNamespace())
			.withMunicipalityId(errand.getMunicipalityId())
			.withErrandNumber(errand.getErrandNumber())
			.withDirection(Direction.INBOUND)
			.withExternalCaseID("")
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
				.withId(randomUUID().toString())
				.withName(attachment.getName())
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

	public EmailRequest createEmailRequest(final Email email, final String sender, final String messageTemplate) {
		return EmailRequest.create()
			.withSubject(email.getSubject())
			.withRecipient(email.getSender()) // Because we are sending a return response
			.withEmailHeaders(toEmailHeaderMap(email))
			.withSender(sender)
			.withSenderName("")
			.withMessage(messageTemplate);
	}

	private Map<EmailHeader, List<String>> toEmailHeaderMap(final Email email) {
		return email.getHeaders().entrySet().stream()
			.collect(toMap(
				entry -> EmailHeader.valueOf(entry.getKey()),
				Map.Entry::getValue,
				(oldValue, newValue) -> newValue));
	}

}
