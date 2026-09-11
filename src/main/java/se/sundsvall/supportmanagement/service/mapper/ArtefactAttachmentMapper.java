package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentLink;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * Maps the link between a handling artefact and an attachment of the errand.
 * <p>
 * One implementation for all four artefacts, which is what the {@link AttachmentLink} interface is for: the link
 * entities differ only in the column naming their owner, and nothing in the mapping depends on which one it is.
 */
public final class ArtefactAttachmentMapper {

	private ArtefactAttachmentMapper() {}

	public static ArtefactAttachment toArtefactAttachment(final AttachmentLink link) {
		return ofNullable(link)
			.map(l -> ArtefactAttachment.create()
				.withAttachmentId(ofNullable(l.getAttachmentEntity()).map(AttachmentEntity::getId).orElse(null))
				.withFileName(ofNullable(l.getAttachmentEntity()).map(AttachmentEntity::getFileName).orElse(null))
				.withMimeType(ofNullable(l.getAttachmentEntity()).map(AttachmentEntity::getMimeType).orElse(null))
				.withFileSize(ofNullable(l.getAttachmentEntity()).map(AttachmentEntity::getFileSize).orElse(null))
				.withPurpose(ofNullable(l.getAttachmentEntity()).map(AttachmentEntity::getPurpose).map(ErrandAttachmentMapper::toErrandAttachmentPurpose).orElse(null))
				.withSortOrder(l.getSortOrder())
				.withCreated(l.getCreated())
				.withCreatedBy(l.getCreatedBy()))
			.orElse(null);
	}

	public static List<ArtefactAttachment> toArtefactAttachments(final List<? extends AttachmentLink> links) {
		return ofNullable(links).orElse(emptyList()).stream()
			.map(ArtefactAttachmentMapper::toArtefactAttachment)
			.toList();
	}
}
