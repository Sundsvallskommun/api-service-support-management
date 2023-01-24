package se.sundsvall.supportmanagement.service.mapper;

import org.overviewproject.mime_types.MimeTypeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeTypeUtils;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

public class ErrandAttachmentMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ErrandAttachmentMapper.class);
	private static final MimeTypeDetector DETECTOR = new MimeTypeDetector();

	private ErrandAttachmentMapper() {
	}

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

	public static List<ErrandAttachment> toErrandAttachments(final List<AttachmentEntity> attachmentEntities) {
		if (isNull(attachmentEntities)) {
			return emptyList();
		}

		return attachmentEntities.stream()
			.map(ErrandAttachmentMapper::toErrandAttachment)
			.filter(Objects::nonNull)
			.toList();
	}

	public static ErrandAttachment toErrandAttachment(final AttachmentEntity attachmentEntity) {
		if (isNull(attachmentEntity)) {
			return null;
		}

		return new ErrandAttachment()
			.withBase64EncodedString(encodeBase64String(attachmentEntity.getFile()))
			.withMimeType(attachmentEntity.getMimeType())
			.withErrandAttachmentHeader(toErrandAttachmentHeader(attachmentEntity));
	}

	private static ErrandAttachmentHeader toErrandAttachmentHeader(final AttachmentEntity attachmentEntity) {
		if (isNull(attachmentEntity)) {
			return null;
		}

		return new ErrandAttachmentHeader()
			.withFileName(attachmentEntity.getFileName())
			.withId(attachmentEntity.getId());
	}

	private static String detectMimeType(String fileName, byte[] byteArray) {
		try (InputStream stream = new ByteArrayInputStream(byteArray)) {
			return DETECTOR.detectMimeType(fileName, stream);
		} catch (Exception e) {
			LOGGER.warn(String.format("Exception when detecting mime type of file with filename '%s'", fileName), e);
			return MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE; // Return mime type for arbitrary binary files
		}
	}

	private static String toFileName(ErrandAttachmentHeader errandAttachmentHeader) {
		return errandAttachmentHeader != null ? errandAttachmentHeader.getFileName() : null;
	}

	private static String toAttachmentId(ErrandAttachmentHeader errandAttachmentHeader) {
		return errandAttachmentHeader != null ? errandAttachmentHeader.getId() : null;
	}
}
