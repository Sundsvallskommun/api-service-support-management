package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.detectMimeType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

public class ErrandAttachmentMapper {

	private ErrandAttachmentMapper() {}

	public static AttachmentEntity toAttachmentEntity(final ErrandEntity errandEntity, final ErrandAttachment errandAttachment) {
		if (anyNull(errandEntity, errandAttachment)) {
			return null;
		}

		byte[] byteArray = decodeBase64(errandAttachment.getBase64EncodedString());

		return AttachmentEntity.create()
			.withId(toAttachmentId(errandAttachment.getErrandAttachmentHeader()))
			.withErrandEntity(errandEntity)
			.withFile(byteArray)
			.withFileName(toFileName(errandAttachment.getErrandAttachmentHeader()))
			.withMimeType(detectMimeType(toFileName(errandAttachment.getErrandAttachmentHeader()), byteArray));
	}

	public static List<ErrandAttachmentHeader> toErrandAttachmentHeaders(final List<AttachmentEntity> attachmentEntities) {
		return Optional.ofNullable(attachmentEntities).orElse(emptyList()).stream()
			.map(ErrandAttachmentMapper::toErrandAttachmentHeader)
			.filter(Objects::nonNull)
			.toList();
	}

	public static ErrandAttachment toErrandAttachment(final AttachmentEntity attachmentEntity) {
		return Optional.ofNullable(attachmentEntity)
			.map(e -> ErrandAttachment.create()
				.withBase64EncodedString(encodeBase64String(e.getFile()))
				.withErrandAttachmentHeader(toErrandAttachmentHeader(e)))
			.orElse(null);
	}

	private static ErrandAttachmentHeader toErrandAttachmentHeader(final AttachmentEntity attachmentEntity) {
		return Optional.ofNullable(attachmentEntity)
			.map(e -> ErrandAttachmentHeader.create()
				.withFileName(e.getFileName())
				.withId(e.getId())
				.withMimeType(e.getMimeType()))
			.orElse(null);
	}

	private static String toFileName(ErrandAttachmentHeader errandAttachmentHeader) {
		return Optional.ofNullable(errandAttachmentHeader)
			.map(ErrandAttachmentHeader::getFileName)
			.orElse(null);
	}

	private static String toAttachmentId(ErrandAttachmentHeader errandAttachmentHeader) {
		return Optional.ofNullable(errandAttachmentHeader)
			.map(ErrandAttachmentHeader::getId)
			.orElse(null);
	}
}
