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
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
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

	public static AttachmentEntity toAttachmentEntity(final ErrandEntity errandEntity, final MultipartFile errandAttachment, final String channel) {
		if (anyNull(errandEntity, errandAttachment)) {
			return null;
		}

		try {
			return AttachmentEntity.create()
				.withErrandEntity(errandEntity)
				.withNamespace(errandEntity.getNamespace())
				.withMunicipalityId(errandEntity.getMunicipalityId())
				.withFileSize(Math.toIntExact(errandAttachment.getSize()))
				.withAttachmentData(new AttachmentDataEntity().withFile(Hibernate.getLobHelper().createBlob(errandAttachment.getInputStream(), errandAttachment.getSize())))
				.withFileName(errandAttachment.getOriginalFilename())
				.withMimeType(detectMimeTypeFromStream(errandAttachment.getOriginalFilename(), errandAttachment.getInputStream()))
				.withChannel(ofNullable(channel).orElse(WEB_UI));
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

	public static ErrandAttachment toErrandAttachment(final AttachmentEntity attachmentEntity) {
		return Optional.ofNullable(attachmentEntity)
			.map(e -> ErrandAttachment.create()
				.withFileName(e.getFileName())
				.withCreated(e.getCreated())
				.withId(e.getId())
				.withMimeType(e.getMimeType())
				.withChannel(e.getChannel())
				.withHash(e.getHash()))
			.orElse(null);
	}

}
