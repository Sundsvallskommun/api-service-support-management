package se.sundsvall.supportmanagement.service.mapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentPurpose;
import se.sundsvall.supportmanagement.api.model.attachment.UpdateErrandAttachmentRequest;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.supportmanagement.service.mapper.Channels.WEB_UI;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.detectMimeTypeFromStream;

public final class ErrandAttachmentMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ErrandAttachmentMapper.class);

	private ErrandAttachmentMapper() {}

	public static AttachmentEntity toAttachmentEntity(final ErrandEntity errandEntity, final MultipartFile file, final ErrandAttachment errandAttachment) {
		if (anyNull(errandEntity, file)) {
			return null;
		}

		try {
			return AttachmentEntity.create()
				.withErrandEntity(errandEntity)
				.withNamespace(errandEntity.getNamespace())
				.withMunicipalityId(errandEntity.getMunicipalityId())
				.withFileSize(Math.toIntExact(file.getSize()))
				.withAttachmentData(new AttachmentDataEntity().withFile(Hibernate.getLobHelper().createBlob(file.getInputStream(), file.getSize())))
				.withFileName(file.getOriginalFilename())
				.withMimeType(detectMimeTypeFromStream(file.getOriginalFilename(), file.getInputStream()))
				.withChannel(ofNullable(errandAttachment).map(ErrandAttachment::getChannel).orElse(WEB_UI))
				.withReceived(ofNullable(errandAttachment).map(ErrandAttachment::getReceived).orElse(null));
		} catch (final IOException e) {
			LOGGER.warn("Exception when reading file", e);
			throw Problem.valueOf(BAD_REQUEST, "Could not read input stream!");
		}
	}

	public static AttachmentEntity toAttachmentEntity(final ErrandEntity errandEntity, final ResponseEntity<InputStreamResource> errandAttachment, final String fileName, final int fileSize, final String channel) {
		if (anyNull(errandEntity, errandAttachment, errandAttachment.getBody())) {
			return null;
		}

		final InputStream content;
		try {
			content = errandAttachment.getBody().getInputStream();
		} catch (final Exception _) {
			throw Problem.valueOf(BAD_REQUEST, "Could not read input stream!");
		}

		return AttachmentEntity.create()
			.withErrandEntity(errandEntity)
			.withNamespace(errandEntity.getNamespace())
			.withMunicipalityId(errandEntity.getMunicipalityId())
			.withFileSize(fileSize)
			.withAttachmentData(new AttachmentDataEntity().withFile(Hibernate.getLobHelper().createBlob(content, fileSize)))
			.withFileName(fileName)
			.withMimeType(detectMimeTypeFromStream(fileName, content))
			.withChannel(channel);
	}

	public static List<ErrandAttachment> toErrandAttachments(final List<AttachmentEntity> attachmentEntities) {
		return Optional.ofNullable(attachmentEntities).orElse(emptyList()).stream()
			.map(ErrandAttachmentMapper::toErrandAttachment)
			.filter(Objects::nonNull)
			.toList();
	}

	/**
	 * Writes the changes of the request to the attachment. What the request leaves out is left as it is.
	 *
	 * @param  entity  the attachment to change.
	 * @param  request the changes.
	 * @param  purpose the purpose the request names, already looked up, or null to leave the stored one.
	 * @return         the changed attachment.
	 */
	public static AttachmentEntity updateAttachmentEntity(final AttachmentEntity entity, final UpdateErrandAttachmentRequest request, final AttachmentPurposeEntity purpose) {
		ofNullable(purpose).ifPresent(entity::setPurpose);
		ofNullable(request.getReceived()).ifPresent(entity::setReceived);
		return entity;
	}

	public static ErrandAttachment toErrandAttachment(final AttachmentEntity attachmentEntity) {
		return Optional.ofNullable(attachmentEntity)
			.map(e -> ErrandAttachment.create()
				.withFileName(e.getFileName())
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withReceived(e.getReceived())
				.withSequenceNumber(e.getSequenceNumber())
				.withId(e.getId())
				.withMimeType(e.getMimeType())
				.withFileSize(e.getFileSize())
				.withChannel(e.getChannel())
				.withHash(e.getHash())
				.withPurpose(toErrandAttachmentPurpose(e.getPurpose())))
			.orElse(null);
	}

	public static ErrandAttachmentPurpose toErrandAttachmentPurpose(final AttachmentPurposeEntity attachmentPurposeEntity) {
		return ofNullable(attachmentPurposeEntity)
			.map(e -> ErrandAttachmentPurpose.create()
				.withId(e.getId())
				.withName(e.getName())
				.withDisplayName(e.getDisplayName()))
			.orElse(null);
	}
}
