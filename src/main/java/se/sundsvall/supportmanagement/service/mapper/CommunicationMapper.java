package se.sundsvall.supportmanagement.service.mapper;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.detectMimeType;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.CommunicationAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.service.util.BlobBuilder;

@Component
public class CommunicationMapper {

	private final BlobBuilder blobBuilder;

	public CommunicationMapper(final BlobBuilder blobBuilder) {this.blobBuilder = blobBuilder;}

	public List<Communication> toCommunications(final List<CommunicationEntity> entities) {
		return entities.stream()
			.map(this::toCommunication)
			.toList();
	}

	public Communication toCommunication(final CommunicationEntity entity) {
		if (entity == null) {
			return null;
		}
		return Communication.create()
			.withCommunicationID(entity.getId())
			.withErrandNumber(entity.getErrandNumber())
			.withDirection(entity.getDirection())
			.withMessageBody(entity.getMessageBody())
			.withSent(entity.getSent())
			.withSubject(entity.getSubject())
			.withCommunicationType(entity.getType())
			.withTarget(entity.getTarget())
			.withViewed(entity.isViewed())
			.withCommunicationAttachments(toAttachments(entity.getAttachments()));
	}


	public List<AttachmentEntity> toAttachments(final CommunicationEntity communicationEntity) {


		return Optional.ofNullable(communicationEntity.getAttachments()).orElse(Collections.emptyList())
			.stream()
			.map(emailAttachment ->
				AttachmentEntity.create()
					.withId(UUID.randomUUID().toString())
					.withFileName(emailAttachment.getName())
					.withMimeType(emailAttachment.getContentType())
					.withAttachmentData(new AttachmentDataEntity().withFile(emailAttachment.getAttachmentData().getFile()))

			)
			.toList();
	}

	public List<CommunicationAttachment> toAttachments(final List<CommunicationAttachmentEntity> attachments) {

		return Optional.ofNullable(attachments)
			.orElse(Collections.emptyList())
			.stream()
			.map(this::toAttachment)
			.toList();
	}

	public CommunicationAttachment toAttachment(final CommunicationAttachmentEntity entity) {
		return CommunicationAttachment.create()
			.withAttachmentID(entity.getId())
			.withName(entity.getName())
			.withContentType(entity.getContentType());
	}

	public CommunicationEntity toCommunicationEntity(final EmailRequest request) {
		return CommunicationEntity.create()
			.withId(UUID.randomUUID().toString())
			.withDirection(Direction.OUTBOUND)
			.withMessageBody(request.getMessage())
			.withSent(OffsetDateTime.now())
			.withSubject(request.getSubject())
			.withType(CommunicationType.EMAIL)
			.withTarget(request.getRecipient())
			.withAttachments(toMessageAttachments(request.getAttachments()))
			.withViewed(false);
	}

	public CommunicationEntity toCommunicationEntity(final SmsRequest request) {
		return CommunicationEntity.create()
			.withId(UUID.randomUUID().toString())
			.withDirection(Direction.OUTBOUND)
			.withMessageBody(request.getMessage())
			.withSent(OffsetDateTime.now())
			.withType(CommunicationType.SMS)
			.withTarget(request.getRecipient())
			.withViewed(false);
	}

	private List<CommunicationAttachmentEntity> toMessageAttachments(final List<EmailAttachment> attachments) {

		return Optional.ofNullable(attachments)
			.orElse(Collections.emptyList()).stream()
			.map(this::toMessageAttachment)
			.toList();
	}

	private CommunicationAttachmentEntity toMessageAttachment(final EmailAttachment attachment) {
		final byte[] byteArray = decodeBase64(attachment.getBase64EncodedString());

		return CommunicationAttachmentEntity.create()
			.withId(UUID.randomUUID().toString())
			.withName(attachment.getName())
			.withAttachmentData(toMessageAttachmentData(attachment))
			.withContentType(detectMimeType(attachment.getName(), byteArray))
			.withAttachmentData(toMessageAttachmentData(attachment));
	}

	private CommunicationAttachmentDataEntity toMessageAttachmentData(final EmailAttachment attachment) {
		return CommunicationAttachmentDataEntity.create()
			.withFile(blobBuilder.createBlob(attachment.getBase64EncodedString()));
	}

}
