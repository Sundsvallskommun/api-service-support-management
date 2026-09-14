package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * Maps an attachment of the errand as a handling artefact uses it.
 * <p>
 * One implementation for all four artefacts: each holds the attachments of the errand it uses, and nothing in the
 * mapping depends on which artefact it is. Only what describes the file is read, never the file itself.
 */
public final class ArtefactAttachmentMapper {

	private ArtefactAttachmentMapper() {}

	public static ArtefactAttachment toArtefactAttachment(final AttachmentEntity attachment) {
		return ofNullable(attachment)
			.map(a -> ArtefactAttachment.create()
				.withAttachmentId(a.getId())
				.withFileName(a.getFileName())
				.withMimeType(a.getMimeType())
				.withFileSize(a.getFileSize())
				.withPurpose(ofNullable(a.getPurpose()).map(ErrandAttachmentMapper::toErrandAttachmentPurpose).orElse(null)))
			.orElse(null);
	}

	public static List<ArtefactAttachment> toArtefactAttachments(final List<AttachmentEntity> attachments) {
		return ofNullable(attachments).orElse(emptyList()).stream()
			.map(ArtefactAttachmentMapper::toArtefactAttachment)
			.toList();
	}
}
