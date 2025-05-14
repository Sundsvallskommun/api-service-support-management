package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static java.time.OffsetTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;

import generated.se.sundsvall.webmessagecollector.MessageAttachment;
import generated.se.sundsvall.webmessagecollector.MessageDTO;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.service.util.BlobBuilder;

@Component
public class WebMessageCollectorMapper {

	private final BlobBuilder blobBuilder;

	public WebMessageCollectorMapper(final BlobBuilder blobBuilder) {
		this.blobBuilder = blobBuilder;
	}

	CommunicationEntity toCommunicationEntity(final MessageDTO messageDTO, final ErrandEntity errand) {
		final var communicationEntity = CommunicationEntity.create()
			.withSender(messageDTO.getFirstName() + " " + messageDTO.getLastName())
			.withSenderUserId(messageDTO.getUserId())
			.withDirection(Direction.INBOUND)
			.withErrandNumber(errand.getErrandNumber())
			.withMunicipalityId(errand.getMunicipalityId())
			.withNamespace(errand.getNamespace())
			.withExternalId(messageDTO.getMessageId())
			.withMessageBody(messageDTO.getMessage())
			.withSent(OffsetDateTime.of(LocalDateTime.parse(messageDTO.getSent()), now(systemDefault()).getOffset()))
			.withType(CommunicationType.WEB_MESSAGE);

		return communicationEntity.withAttachments(toCommunicationAttachmentEntities(messageDTO.getAttachments(), communicationEntity));
	}

	private List<CommunicationAttachmentEntity> toCommunicationAttachmentEntities(final List<MessageAttachment> attachments, final CommunicationEntity communicationEntity) {

		return Optional.ofNullable(attachments).orElse(emptyList()).stream()
			.map(attachment -> toCommunicationAttachmentEntity(attachment, communicationEntity))
			.toList();
	}

	private CommunicationAttachmentEntity toCommunicationAttachmentEntity(final MessageAttachment attachment, final CommunicationEntity communicationEntity) {
		return CommunicationAttachmentEntity.create()
			.withMunicipalityId(communicationEntity.getMunicipalityId())
			.withNamespace(communicationEntity.getNamespace())
			.withForeignId(attachment.getAttachmentId().toString())
			.withCommunicationEntity(communicationEntity)
			.withContentType(attachment.getMimeType())
			.withFileName(attachment.getName());

	}

	AttachmentDataEntity toAttachmentDataEntity(final byte[] attachmentData) {
		return AttachmentDataEntity.create()
			.withFile(blobBuilder.createBlob(attachmentData));

	}

}
