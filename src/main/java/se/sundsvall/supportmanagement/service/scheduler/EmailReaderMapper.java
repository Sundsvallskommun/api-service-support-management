package se.sundsvall.supportmanagement.service.scheduler;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.ContactChannel;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEmailHeaderEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
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
		if (email == null) {
			return Collections.emptyList();
		}
		return Optional.ofNullable(email.getAttachments()).orElse(Collections.emptyList())
			.stream()
			.map(emailAttachment ->
				AttachmentEntity.create()
					.withId(UUID.randomUUID().toString())
					.withAttachmentData(new AttachmentDataEntity().withFile(blobBuilder.createBlob(emailAttachment.getContent())))
					.withFileName(emailAttachment.getName())
					.withMimeType(emailAttachment.getContentType()))
			.toList();
	}

	CommunicationEntity toCommunicationEntity(final Email email) {
		if (email == null) {
			return null;
		}
		return CommunicationEntity.create()
			.withId(UUID.randomUUID().toString())
			.withDirection(Direction.INBOUND)
			.withExternalCaseID("")
			.withSubject(email.getSubject())
			.withSent(email.getReceivedAt())
			.withType(CommunicationType.EMAIL)
			.withTarget(email.getSender())
			.withEmailHeaders(toEmailHeaders(email))
			.withAttachments(toMessageAttachments(email.getAttachments()));
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

	private List<CommunicationAttachmentEntity> toMessageAttachments(final List<EmailAttachment> attachments) {

		return Optional.ofNullable(attachments).orElse(Collections.emptyList()).stream()
			.map(attachment -> CommunicationAttachmentEntity.create()
				.withId(UUID.randomUUID().toString())
				.withName(attachment.getName())
				.withAttachmentData(toMessageAttachmentData(attachment))
				.withContentType(attachment.getContentType()))
			.toList();
	}

	private CommunicationAttachmentDataEntity toMessageAttachmentData(final EmailAttachment attachment) {
		return CommunicationAttachmentDataEntity.create()
			.withFile(blobBuilder.createBlob(attachment.getContent()));
	}

	public Errand toErrand(final Email email) {

		return Errand.create()
			.withTitle(email.getSubject())
			.withDescription(email.getMessage())
			.withStatus("NEW")
			.withClassification(Classification.create().withCategory(email.getMetadata().get("classification.category")).withType(email.getMetadata().get("classification.type")))
			.withStakeholders(List.of(
				Stakeholder.create().withContactChannels(List.of(
						ContactChannel.create().withType("EMAIL").withValue(email.getSender())
					)
				)));

	}

}
