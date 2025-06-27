package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.detectMimeTypeFromStream;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

public final class ErrandAttachmentMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ErrandAttachmentMapper.class);

	private ErrandAttachmentMapper() {}

	public static AttachmentEntity toAttachmentEntity(final ErrandEntity errandEntity, final MultipartFile errandAttachment, final EntityManager entityManager) {
		if (anyNull(errandEntity, errandAttachment)) {
			return null;
		}

		try {
			final Session session = entityManager.unwrap(Session.class);
			return AttachmentEntity.create()
				.withErrandEntity(errandEntity)
				.withNamespace(errandEntity.getNamespace())
				.withMunicipalityId(errandEntity.getMunicipalityId())
				.withFileSize(Math.toIntExact(errandAttachment.getSize()))
				.withAttachmentData(new AttachmentDataEntity().withFile(session.getLobHelper().createBlob(errandAttachment.getInputStream(), errandAttachment.getSize())))
				.withFileName(errandAttachment.getOriginalFilename())
				.withMimeType(detectMimeTypeFromStream(errandAttachment.getOriginalFilename(), errandAttachment.getInputStream()));
		} catch (final IOException e) {
			LOGGER.warn("Exception when reading file", e);
			throw Problem.valueOf(Status.BAD_REQUEST, "Could not read input stream!");
		}
	}

	public static AttachmentEntity toAttachmentEntity(final ErrandEntity errandEntity, final ResponseEntity<InputStreamResource> errandAttachment, final EntityManager entityManager) {
		if (anyNull(errandEntity, errandAttachment, errandAttachment.getBody())) {
			return null;
		}

		final InputStream content;
		try {
			content = errandAttachment.getBody().getInputStream();
		} catch (final Exception e) {
			throw Problem.valueOf(Status.BAD_REQUEST, "Could not read input stream!");
		}

		final Session session = entityManager.unwrap(Session.class);
		return AttachmentEntity.create()
			.withErrandEntity(errandEntity)
			.withNamespace(errandEntity.getNamespace())
			.withMunicipalityId(errandEntity.getMunicipalityId())
			.withFileSize(Math.toIntExact(errandAttachment.getHeaders().getContentLength()))
			.withAttachmentData(new AttachmentDataEntity().withFile(session.getLobHelper().createBlob(content, errandAttachment.getHeaders().getContentLength())))
			.withFileName(errandAttachment.getHeaders().getContentDisposition().getFilename())
			.withMimeType(detectMimeTypeFromStream(errandAttachment.getBody().getFilename(), content));
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
				.withMimeType(e.getMimeType()))
			.orElse(null);
	}

}
