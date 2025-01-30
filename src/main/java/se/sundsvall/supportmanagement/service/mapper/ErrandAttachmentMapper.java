package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.detectMimeTypeFromStream;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
				.withId(e.getId())
				.withMimeType(e.getMimeType()))
			.orElse(null);
	}

}
