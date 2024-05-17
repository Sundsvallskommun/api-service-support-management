package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static java.time.OffsetTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.service.util.BlobBuilder;

import generated.se.sundsvall.webmessagecollector.MessageAttachment;
import generated.se.sundsvall.webmessagecollector.MessageDTO;

@Component
public class WebMessageCollectorMapper {

	private final BlobBuilder blobBuilder;

	public WebMessageCollectorMapper(final BlobBuilder blobBuilder) {this.blobBuilder = blobBuilder;}

	CommunicationEntity toCommunicationEntity(final MessageDTO messageDTO, final String errandNumber) {
		final var communicationEntity = CommunicationEntity.create()
			.withSender(messageDTO.getFirstName() + " " + messageDTO.getLastName())
			.withId(UUID.randomUUID().toString())
			.withDirection(Direction.INBOUND)
			.withErrandNumber(errandNumber)
			.withExternalCaseID(messageDTO.getExternalCaseId())
			.withMessageBody(messageDTO.getMessage())
			.withSent(OffsetDateTime.of(LocalDateTime.parse(messageDTO.getSent()), now(systemDefault()).getOffset()))
			.withType(CommunicationType.EMAIL)
			.withViewed(true);

		return communicationEntity.withAttachments(toCommunicationAttachmentEntities(messageDTO.getAttachments(), communicationEntity));
	}

	private List<CommunicationAttachmentEntity> toCommunicationAttachmentEntities(final List<MessageAttachment> attachments, final CommunicationEntity communicationEntity) {

		return Optional.ofNullable(attachments).orElse(emptyList()).stream()
			.map(attachment -> toCommunicationAttachmentEntity(attachment, communicationEntity))
			.toList();
	}

	private CommunicationAttachmentEntity toCommunicationAttachmentEntity(final MessageAttachment attachment, final CommunicationEntity communicationEntity) {
		return CommunicationAttachmentEntity.create()
			.withId(String.valueOf(attachment.getAttachmentId()))
			.withCommunicationEntity(communicationEntity)
			.withContentType(attachment.getMimeType())
			.withName(attachment.getName());

	}

	CommunicationAttachmentDataEntity toCommunicationAttachmentDataEntity(final byte[] attachmentData) {
		return CommunicationAttachmentDataEntity.create()
			.withFile(blobBuilder.createBlob(attachmentData));

	}

}
