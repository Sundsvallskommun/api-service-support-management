package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachment;

/**
 * Linking attachments of the errand to a handling artefact, the same way for all four kinds of artefact.
 * <p>
 * Each artefact holds the attachments it uses in a collection of its own, backed by a join table. The caller passes in
 * that collection, and gets the lookup, the rule that the attachment belongs to the same errand as the artefact, and
 * the duplicate check from here.
 * <p>
 * The purpose of an attachment is not written here; it is written through the attachment resource of the errand.
 */
@Service
public class ArtefactAttachmentService {

	private static final String ATTACHMENT_NOT_FOUND = "An attachment with id '%s' could not be found in errand with id '%s'";
	private static final String ALREADY_LINKED = "An attachment with id '%s' is already linked to this artefact";
	private static final String LINK_NOT_FOUND = "An attachment with id '%s' is not linked to this artefact";

	private final AttachmentRepository attachmentRepository;
	private final ErrandAttachmentService errandAttachmentService;

	ArtefactAttachmentService(final AttachmentRepository attachmentRepository, final ErrandAttachmentService errandAttachmentService) {
		this.attachmentRepository = attachmentRepository;
		this.errandAttachmentService = errandAttachmentService;
	}

	/**
	 * Stores a file as an attachment of the errand and links it to the artefact in one call.
	 * <p>
	 * The attachment becomes an ordinary attachment of the errand - it is read, content and all, through the attachment
	 * resource of the errand, and it survives the artefact being removed.
	 *
	 * @param  attachments the attachments of the artefact, which the new one is added to.
	 * @return             the id of the attachment that was created.
	 */
	@Transactional
	public String uploadAndLink(final String namespace, final String municipalityId, final String errandId, final MultipartFile file, final List<AttachmentEntity> attachments) {
		final var attachmentId = errandAttachmentService.createErrandAttachment(namespace, municipalityId, errandId, file, null);
		addLink(namespace, municipalityId, errandId, attachmentId, attachments);

		return attachmentId;
	}

	/**
	 * Links an attachment that is already on the errand.
	 * <p>
	 * The attachment is fetched <em>through the errand</em>, so an attachment of another errand is not found and the
	 * outcome is a 404.
	 *
	 * @param attachments the attachments of the artefact, which the attachment is added to.
	 */
	@Transactional
	public ErrandAttachment link(final String namespace, final String municipalityId, final String errandId, final String attachmentId, final List<AttachmentEntity> attachments) {
		return addLink(namespace, municipalityId, errandId, attachmentId, attachments);
	}

	/**
	 * Removes the link. The attachment stays on the errand. The attachment is matched by id, ignoring case, and a 404 is
	 * thrown when it is not linked.
	 *
	 * @param attachments the attachments of the artefact, which the attachment is taken out of.
	 */
	@Transactional
	public void unlink(final String attachmentId, final List<AttachmentEntity> attachments) {
		if (isNull(attachments) || !attachments.removeIf(attachment -> hasId(attachment, attachmentId))) {
			throw Problem.valueOf(NOT_FOUND, LINK_NOT_FOUND.formatted(attachmentId));
		}
	}

	private ErrandAttachment addLink(final String namespace, final String municipalityId, final String errandId, final String attachmentId, final List<AttachmentEntity> attachments) {
		final var attachmentEntity = attachmentRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ATTACHMENT_NOT_FOUND.formatted(attachmentId, errandId)));

		if (ofNullable(attachments).orElse(emptyList()).stream().anyMatch(attachment -> hasId(attachment, attachmentId))) {
			throw Problem.valueOf(CONFLICT, ALREADY_LINKED.formatted(attachmentId));
		}

		attachments.add(attachmentEntity);

		return toErrandAttachment(attachmentEntity);
	}

	private static boolean hasId(final AttachmentEntity attachment, final String attachmentId) {
		return ofNullable(attachment)
			.map(AttachmentEntity::getId)
			.filter(attachmentId::equalsIgnoreCase)
			.isPresent();
	}
}
