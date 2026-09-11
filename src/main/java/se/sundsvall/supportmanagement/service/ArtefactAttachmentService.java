package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachmentLink;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentLink;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactAttachmentMapper.toArtefactAttachment;

/**
 * Linking attachments of the errand to a handling artefact, written once for all four of them.
 * <p>
 * The four link entities differ only in the column naming their owner, and nothing here depends on which one it is -
 * the caller passes in its own {@link ArtefactLinks}, and gets the lookup, the invariant and the duplicate check from
 * here. What that buys is that the rule about an attachment belonging to the same errand as the artefact holds in one
 * place rather than in four.
 * <p>
 * What the attachment is for is not among the things written here. That belongs to the attachment rather than to any
 * one link to it, and is written through the attachment resource of the errand.
 * <p>
 * The link is saved through the repository rather than by cascading from the artefact. The collections on the
 * artefacts deliberately carry no PERSIST, so that a link the attachment side has just removed cannot be written back
 * by the next flush - and the price of that is this explicit save.
 */
@Service
public class ArtefactAttachmentService {

	private static final String ATTACHMENT_NOT_FOUND = "An attachment with id '%s' could not be found in errand with id '%s'";
	private static final String ALREADY_LINKED = "An attachment with id '%s' is already linked to this artefact";
	private static final String LINK_NOT_FOUND = "An attachment with id '%s' is not linked to this artefact";

	private final AttachmentRepository attachmentRepository;
	private final ErrandAttachmentService errandAttachmentService;

	/**
	 * The attachment links of one artefact: the typed collection they are held in, how a new one is built from the
	 * attachment it is to point at, and the repository it is saved through.
	 */
	public record ArtefactLinks<L extends AttachmentLink>(List<L> links, Function<AttachmentEntity, L> factory, JpaRepository<L, String> repository) {}

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
	 * @return the id of the attachment that was created.
	 */
	@Transactional
	public <L extends AttachmentLink> String uploadAndLink(final String namespace, final String municipalityId, final String errandId, final MultipartFile file,
		final Integer sortOrder, final ArtefactLinks<L> artefactLinks) {

		final var attachmentId = errandAttachmentService.createErrandAttachment(namespace, municipalityId, errandId, file, null);
		addLink(namespace, municipalityId, errandId, attachmentId, sortOrder, artefactLinks);

		return attachmentId;
	}

	/**
	 * Links an attachment that is already on the errand.
	 * <p>
	 * The attachment is fetched <em>through the errand</em>, which is what upholds the invariant JPA cannot express: the
	 * two foreign keys of a link know nothing about each other, so nothing else would stop an attachment of one errand from
	 * being linked to an artefact of another. Fetching it this way makes belonging a consequence of the lookup, and the
	 * outcome a 404 rather than a 400.
	 */
	@Transactional
	public <L extends AttachmentLink> ArtefactAttachment link(final String namespace, final String municipalityId, final String errandId, final String attachmentId,
		final Integer sortOrder, final ArtefactLinks<L> artefactLinks) {

		return addLink(namespace, municipalityId, errandId, attachmentId, sortOrder, artefactLinks);
	}

	@Transactional
	public <L extends AttachmentLink> ArtefactAttachment update(final String attachmentId, final ArtefactAttachmentLink link, final List<L> links) {
		final var entity = findLinkOrElseThrow(links, attachmentId);
		ofNullable(link.getSortOrder()).ifPresent(entity::setSortOrder);

		return toArtefactAttachment(entity);
	}

	/**
	 * Removes the link. The attachment stays on the errand, which is the whole point of the link being a relation on top of
	 * the ownership rather than an ownership of its own.
	 * <p>
	 * Taking it out of the collection is enough: orphan removal on the artefact deletes the row, and nothing cascades
	 * from the link towards the attachment.
	 */
	@Transactional
	public <L extends AttachmentLink> void unlink(final String attachmentId, final List<L> links) {
		links.remove(findLinkOrElseThrow(links, attachmentId));
	}

	private <L extends AttachmentLink> ArtefactAttachment addLink(final String namespace, final String municipalityId, final String errandId, final String attachmentId,
		final Integer sortOrder, final ArtefactLinks<L> artefactLinks) {

		final var attachmentEntity = attachmentRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ATTACHMENT_NOT_FOUND.formatted(attachmentId, errandId)));

		if (findLink(artefactLinks.links(), attachmentId).isPresent()) {
			throw Problem.valueOf(CONFLICT, ALREADY_LINKED.formatted(attachmentId));
		}

		final var link = artefactLinks.factory().apply(attachmentEntity);
		link.setSortOrder(sortOrder);

		final var saved = artefactLinks.repository().save(link);
		artefactLinks.links().add(saved);

		return toArtefactAttachment(saved);
	}

	private <L extends AttachmentLink> L findLinkOrElseThrow(final List<L> links, final String attachmentId) {
		return findLink(links, attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, LINK_NOT_FOUND.formatted(attachmentId)));
	}

	private <L extends AttachmentLink> Optional<L> findLink(final List<L> links, final String attachmentId) {
		return ofNullable(links).orElse(emptyList()).stream()
			.filter(link -> ofNullable(link.getAttachmentEntity())
				.map(AttachmentEntity::getId)
				.filter(attachmentId::equalsIgnoreCase)
				.isPresent())
			.findFirst();
	}
}
