package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactAttachmentMapper.toArtefactAttachment;

/**
 * Linking attachments of the errand to a handling artefact, written once for all four of them.
 * <p>
 * Each artefact holds the attachments it uses in a collection of its own, backed by a join table, and nothing here
 * depends on which artefact it is - the caller passes in that collection, and gets the lookup, the invariant and the
 * duplicate check from here. What that buys is that the rule about an attachment belonging to the same errand as the
 * artefact holds in one place rather than in four.
 * <p>
 * What the attachment is for is not among the things written here. That belongs to the attachment rather than to any
 * one link to it, and is written through the attachment resource of the errand.
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
	 * The attachment is fetched <em>through the errand</em>, which is what upholds the invariant JPA cannot express: the
	 * two foreign keys of the join table know nothing about each other, so nothing else would stop an attachment of one
	 * errand from being linked to an artefact of another. Fetching it this way makes belonging a consequence of the
	 * lookup, and the outcome a 404 rather than a 400.
	 *
	 * @param attachments the attachments of the artefact, which the attachment is added to.
	 */
	@Transactional
	public ArtefactAttachment link(final String namespace, final String municipalityId, final String errandId, final String attachmentId, final List<AttachmentEntity> attachments) {
		return addLink(namespace, municipalityId, errandId, attachmentId, attachments);
	}

	/**
	 * Removes the link. The attachment stays on the errand, which is the whole point of the link being a relation on top of
	 * the ownership rather than an ownership of its own.
	 * <p>
	 * Matched by id rather than by equality, since an attachment compares its data and its errand, and reaching those
	 * would load the file and the errand for nothing.
	 *
	 * @param attachments the attachments of the artefact, which the attachment is taken out of.
	 */
	@Transactional
	public void unlink(final String attachmentId, final List<AttachmentEntity> attachments) {
		if (isNull(attachments) || !attachments.removeIf(attachment -> hasId(attachment, attachmentId))) {
			throw Problem.valueOf(NOT_FOUND, LINK_NOT_FOUND.formatted(attachmentId));
		}
	}

	private ArtefactAttachment addLink(final String namespace, final String municipalityId, final String errandId, final String attachmentId, final List<AttachmentEntity> attachments) {
		final var attachmentEntity = attachmentRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ATTACHMENT_NOT_FOUND.formatted(attachmentId, errandId)));

		if (ofNullable(attachments).orElse(emptyList()).stream().anyMatch(attachment -> hasId(attachment, attachmentId))) {
			throw Problem.valueOf(CONFLICT, ALREADY_LINKED.formatted(attachmentId));
		}

		attachments.add(attachmentEntity);

		return toArtefactAttachment(attachmentEntity);
	}

	private static boolean hasId(final AttachmentEntity attachment, final String attachmentId) {
		return ofNullable(attachment)
			.map(AttachmentEntity::getId)
			.filter(attachmentId::equalsIgnoreCase)
			.isPresent();
	}
}
