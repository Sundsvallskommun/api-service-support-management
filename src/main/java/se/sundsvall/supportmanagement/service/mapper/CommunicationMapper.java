package se.sundsvall.supportmanagement.service.mapper;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.detectMimeType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.CommunicationAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.RequestAttachment;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEmailHeaderEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;
import se.sundsvall.supportmanagement.service.util.BlobBuilder;

@Component
public class CommunicationMapper {

	private final BlobBuilder blobBuilder;

	public CommunicationMapper(final BlobBuilder blobBuilder) {
		this.blobBuilder = blobBuilder;
	}

	@NotNull
	private static Map<EmailHeader, List<String>> toHeaders(final CommunicationEntity entity) {
		return Optional.ofNullable(entity.getEmailHeaders())
			.orElse(List.of()).stream()
			.collect(Collectors.toMap(CommunicationEmailHeaderEntity::getHeader, CommunicationEmailHeaderEntity::getValues));
	}

	public List<Communication> toCommunications(final List<CommunicationEntity> entities) {
		return entities.stream()
			.map(this::toCommunication)
			.toList();
	}

	public Communication toCommunication(final CommunicationEntity entity) {

		return Optional.ofNullable(entity).map(communication -> Communication.create()
			.withSender(entity.getSender())
			.withEmailHeaders(toHeaders(entity))
			.withCommunicationID(entity.getId())
			.withErrandNumber(entity.getErrandNumber())
			.withDirection(entity.getDirection())
			.withMessageBody(entity.getMessageBody())
			.withSent(entity.getSent())
			.withSubject(entity.getSubject())
			.withCommunicationType(entity.getType())
			.withTarget(entity.getTarget())
			.withRecipients(entity.getRecipients())
			.withViewed(entity.isViewed())
			.withInternal(entity.isInternal())
			.withCommunicationAttachments(toCommunicationAttachments(entity.getAttachments(), entity.getErrandAttachments())))
			.orElse(null);
	}

	public List<AttachmentEntity> toAttachments(final CommunicationEntity communicationEntity) {
		return Optional.ofNullable(communicationEntity.getAttachments()).orElse(Collections.emptyList())
			.stream()
			.map(attachment -> AttachmentEntity.create()
				.withFileName(attachment.getFileName())
				.withNamespace(communicationEntity.getNamespace())
				.withMunicipalityId(communicationEntity.getMunicipalityId())
				.withMimeType(attachment.getMimeType())
				.withFileSize(attachment.getFileSize())
				.withAttachmentData(attachment.getAttachmentData()))
			.toList();
	}

	public List<CommunicationAttachment> toCommunicationAttachments(final List<CommunicationAttachmentEntity> communicationAttachments, final List<AttachmentEntity> errandAttachments) {
		final List<CommunicationAttachment> attachments = new ArrayList<>();
		Optional.ofNullable(communicationAttachments)
			.orElse(Collections.emptyList())
			.forEach(attachment -> attachments.add(toAttachment(attachment)));
		Optional.ofNullable(errandAttachments)
			.orElse(Collections.emptyList())
			.forEach(attachment -> attachments.add(toAttachment(attachment)));

		return attachments;
	}

	public CommunicationAttachment toAttachment(final CommunicationAttachmentEntity entity) {
		return CommunicationAttachment.create()
			.withAttachmentID(entity.getId())
			.withFileName(entity.getFileName())
			.withContentType(entity.getMimeType());
	}

	public CommunicationAttachment toAttachment(final AttachmentEntity entity) {
		return CommunicationAttachment.create()
			.withAttachmentID(entity.getId())
			.withFileName(entity.getFileName())
			.withContentType(entity.getMimeType());
	}

	public CommunicationEntity toCommunicationEntity(final String namespace, final String municipalityId, final EmailRequest request) {
		return CommunicationEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withSender(request.getSender())
			.withEmailHeaders(toEmailHeaders(request.getEmailHeaders()))
			.withDirection(Direction.OUTBOUND)
			.withMessageBody(request.getMessage())
			.withSent(OffsetDateTime.now())
			.withSubject(request.getSubject())
			.withType(CommunicationType.EMAIL)
			.withTarget(request.getRecipient())
			.withRecipients(List.of(request.getRecipient()))
			.withInternal(request.getInternal())
			.withAttachments(toMessageAttachments(namespace, municipalityId, request.getAttachments()))
			.withViewed(false);
	}

	private List<CommunicationEmailHeaderEntity> toEmailHeaders(final Map<EmailHeader, List<String>> emailHeaders) {
		return Optional.ofNullable(emailHeaders)
			.orElse(Collections.emptyMap()).entrySet().stream()
			.map(entry -> CommunicationEmailHeaderEntity.create()
				.withHeader(entry.getKey())
				.withValues(entry.getValue()))
			.toList();
	}

	public CommunicationEntity toCommunicationEntity(final String namespace, final String municipalityId, final SmsRequest request) {
		return CommunicationEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withSender(request.getSender())
			.withDirection(Direction.OUTBOUND)
			.withMessageBody(request.getMessage())
			.withSent(OffsetDateTime.now())
			.withType(CommunicationType.SMS)
			.withInternal(request.getInternal())
			.withTarget(request.getRecipient())
			.withRecipients(List.of(request.getRecipient()))
			.withViewed(false);
	}

	public CommunicationEntity toCommunicationEntity(final String namespace, final String municipalityId, final String errandNumber, final WebMessageRequest request,
		final String sender, final String senderUserId) {
		return CommunicationEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withErrandNumber(errandNumber)
			.withDirection(Direction.OUTBOUND)
			.withMessageBody(request.getMessage())
			.withSent(OffsetDateTime.now())
			.withType(CommunicationType.WEB_MESSAGE)
			.withAttachments(toMessageAttachments(namespace, municipalityId, request.getAttachments()))
			.withViewed(false)
			.withSender(sender)
			.withInternal(request.getInternal())
			.withSenderUserId(senderUserId);
	}

	private List<CommunicationAttachmentEntity> toMessageAttachments(final String namespace, final String municipalityId, final List<? extends RequestAttachment> attachments) {

		return Optional.ofNullable(attachments)
			.orElse(Collections.emptyList()).stream()
			.map(attachment -> toMessageAttachment(namespace, municipalityId, attachment))
			.toList();
	}

	private CommunicationAttachmentEntity toMessageAttachment(final String namespace, final String municipalityId, final RequestAttachment attachment) {
		final byte[] byteArray = decodeBase64(attachment.getBase64EncodedString());

		return CommunicationAttachmentEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withFileName(attachment.getFileName())
			.withFileSize(byteArray.length)
			.withAttachmentData(toMessageAttachmentData(byteArray))
			.withContentType(detectMimeType(attachment.getFileName(), byteArray));
	}

	private AttachmentDataEntity toMessageAttachmentData(final byte[] byteArray) {
		return AttachmentDataEntity.create()
			.withFile(blobBuilder.createBlob(byteArray));
	}
}
