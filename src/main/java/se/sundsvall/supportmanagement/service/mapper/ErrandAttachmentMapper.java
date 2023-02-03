package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.detectMimeType;

import java.util.List;
import java.util.Objects;

import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

public class ErrandAttachmentMapper {

	private ErrandAttachmentMapper() {}

	public static AttachmentEntity toAttachmentEntity(final ErrandEntity errandEntity, final ErrandAttachment errandAttachment) {
		if (isNull(errandEntity) || isNull(errandAttachment)) {
			return null;
		}

		byte[] byteArray = decodeBase64(errandAttachment.getBase64EncodedString());

		return new AttachmentEntity()
			.withId(toAttachmentId(errandAttachment.getErrandAttachmentHeader()))
			.withErrandEntity(errandEntity)
			.withFile(byteArray)
			.withFileName(toFileName(errandAttachment.getErrandAttachmentHeader()))
			.withMimeType(detectMimeType(toFileName(errandAttachment.getErrandAttachmentHeader()), byteArray));
	}

	public static List<ErrandAttachmentHeader> toErrandAttachmentHeaders(final List<AttachmentEntity> attachmentEntities) {
		if (isNull(attachmentEntities)) {
			return emptyList();
		}

		return attachmentEntities.stream()
			.map(ErrandAttachmentMapper::toErrandAttachmentHeader)
			.filter(Objects::nonNull)
			.toList();
	}

	public static ErrandAttachment toErrandAttachment(final AttachmentEntity attachmentEntity) {
		if (isNull(attachmentEntity)) {
			return null;
		}

		return new ErrandAttachment()
			.withBase64EncodedString(encodeBase64String(attachmentEntity.getFile()))
			.withErrandAttachmentHeader(toErrandAttachmentHeader(attachmentEntity));
	}

	private static ErrandAttachmentHeader toErrandAttachmentHeader(final AttachmentEntity attachmentEntity) {
		if (isNull(attachmentEntity)) {
			return null;
		}

		return new ErrandAttachmentHeader()
			.withFileName(attachmentEntity.getFileName())
			.withId(attachmentEntity.getId())
			.withMimeType(attachmentEntity.getMimeType());
	}

	private static String toFileName(ErrandAttachmentHeader errandAttachmentHeader) {
		return errandAttachmentHeader != null ? errandAttachmentHeader.getFileName() : null;
	}

	private static String toAttachmentId(ErrandAttachmentHeader errandAttachmentHeader) {
		return errandAttachmentHeader != null ? errandAttachmentHeader.getId() : null;
	}
}
