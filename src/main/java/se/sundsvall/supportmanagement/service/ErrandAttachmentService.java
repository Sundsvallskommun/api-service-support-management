package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentPurpose;
import se.sundsvall.supportmanagement.api.model.attachment.UpdateErrandAttachmentRequest;
import se.sundsvall.supportmanagement.integration.db.AttachmentPurposeRepository;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INSUFFICIENT_STORAGE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ATTACHMENT;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toAttachmentEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachment;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachments;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.computeSha256Hex;

@Service
public class ErrandAttachmentService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandAttachmentService.class);

	private static final String ATTACHMENT_ENTITY_NOT_FOUND = "An attachment with id '%s' could not be found on errand with id '%s'";
	private static final String ATTACHMENT_ENTITY_NOT_CREATED = "Attachment could not be created";
	private static final String ATTACHMENT_PURPOSE_NOT_FOUND = "'%s' is not an attachment purpose of namespace '%s' and municipality with id '%s'";
	private static final String EVENT_LOG_ADD_ATTACHMENT = "En bilaga har lagts till i ärendet.";
	private static final String EVENT_LOG_REMOVE_ATTACHMENT = "En bilaga har tagits bort från ärendet.";
	private static final String EVENT_LOG_UPDATE_ATTACHMENT = "En bilaga i ärendet har uppdaterats.";

	private final ErrandsRepository errandsRepository;
	private final AccessControlService accessControlService;
	private final AttachmentRepository attachmentRepository;
	private final RevisionService revisionService;
	private final EventService eventService;
	private final EntityManager entityManager;
	private final Semaphore semaphore;
	private final AttachmentPurposeRepository attachmentPurposeRepository;

	public ErrandAttachmentService(
		final ErrandsRepository errandsRepository,
		final AccessControlService accessControlService,
		final RevisionService revisionService, final EventService eventService,
		final AttachmentRepository attachmentRepository, final EntityManager entityManager, final Semaphore semaphore,
		final AttachmentPurposeRepository attachmentPurposeRepository) {
		this.errandsRepository = errandsRepository;
		this.accessControlService = accessControlService;
		this.revisionService = revisionService;
		this.eventService = eventService;
		this.attachmentRepository = attachmentRepository;
		this.entityManager = entityManager;
		this.semaphore = semaphore;
		this.attachmentPurposeRepository = attachmentPurposeRepository;
	}

	@Transactional
	public String createErrandAttachment(final String namespace, final String municipalityId, final String errandId, final MultipartFile errandAttachment, final String channel) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.ATTACHMENT, RW);

		return createErrandAttachmentInternal(errandEntity, () -> toAttachmentEntity(errandEntity, errandAttachment, channel));
	}

	@Transactional
	public String createErrandAttachment(final ErrandEntity errandEntity, final ResponseEntity<InputStreamResource> file, final String fileName, final int fileSize, final String channel) {
		return createErrandAttachmentInternal(errandEntity, () -> toAttachmentEntity(errandEntity, file, fileName, fileSize, channel));
	}

	private String createErrandAttachmentInternal(final ErrandEntity errandEntity,
		final Supplier<AttachmentEntity> attachmentEntitySupplier) {
		var attachmentEntity = ofNullable(attachmentEntitySupplier.get())
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ATTACHMENT_ENTITY_NOT_CREATED));

		// Save
		attachmentEntity = attachmentRepository.saveAndFlush(attachmentEntity);

		// Compute hash by streaming from the persisted database blob
		computeAndSetHash(attachmentEntity);

		errandEntity.getAttachments().add(attachmentEntity);

		// Update errand with new attachment and create new revision
		final var revisionResult = revisionService.createErrandRevision(errandEntity);

		try {
			eventService.createErrandEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandEntity, revisionResult.latest(), revisionResult.previous(), ATTACHMENT);
		} catch (final Exception e) {
			LOG.warn("Failed to log attachment-added event for errand {}: {}", errandEntity.getId(), e.getMessage());
		}

		return attachmentEntity.getId();
	}

	@Transactional(readOnly = true)
	public void readErrandAttachment(final String namespace, final String municipalityId, final String errandId, final String attachmentId, final HttpServletResponse response) {

		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.ATTACHMENT, LR);

		// Scoped to the errand: authorising the errand says nothing about an attachment belonging to a different one.
		final var attachmentEntity = attachmentRepository
			.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_NOT_FOUND, attachmentId, errandId)));

		streamAttachmentData(attachmentEntity, response);
	}

	@Transactional(readOnly = true)
	public List<ErrandAttachment> readErrandAttachments(final String namespace, final String municipalityId, final String errandId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.ATTACHMENT, LR);
		return toErrandAttachments(errandEntity.getAttachments());
	}

	/**
	 * Writes what the attachment is for, named by the id of an attachment purpose of the namespace.
	 * <p>
	 * The only way to set it. What a file is for belongs to the file rather than to any one link to it, which is what lets
	 * the errand show it in its own attachment list and what lets an attachment belonging to no handling artefact carry one
	 * at all. A request without a purpose leaves the stored one standing; clearing it is
	 * {@link #deleteErrandAttachmentPurpose}.
	 */
	@Transactional
	public ErrandAttachment updateErrandAttachment(final String namespace, final String municipalityId, final String errandId, final String attachmentId,
		final UpdateErrandAttachmentRequest request) {

		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.ATTACHMENT, RW);
		final var purpose = ofNullable(request.getPurpose())
			.map(ErrandAttachmentPurpose::getId)
			.map(purposeId -> findPurposeOrElseThrow(namespace, municipalityId, purposeId));
		final var attachmentEntity = findAttachmentOrElseThrow(errandEntity, errandId, attachmentId);

		purpose.ifPresent(attachmentEntity::setPurpose);
		recordChange(errandEntity);

		return toErrandAttachment(attachmentEntity);
	}

	/**
	 * Clears what the attachment is for. The purpose itself stays in the metadata of the namespace.
	 */
	@Transactional
	public void deleteErrandAttachmentPurpose(final String namespace, final String municipalityId, final String errandId, final String attachmentId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.ATTACHMENT, RW);

		findAttachmentOrElseThrow(errandEntity, errandId, attachmentId).setPurpose(null);
		recordChange(errandEntity);
	}

	@Transactional
	public void deleteErrandAttachment(final String namespace, final String municipalityId, final String errandId, final String attachmentId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.ATTACHMENT, RW);
		final var attachmentEntity = findAttachmentOrElseThrow(errandEntity, errandId, attachmentId);

		final ErrandEntity entity;
		try {
			// Update errand after removal of attachment and create new revision
			errandEntity.getAttachments().remove(attachmentEntity);
			entity = errandsRepository.save(errandEntity);

		} catch (final Exception _) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, String.format("Failed to delete attachment with id '%s' from errand with id '%s'", attachmentId, errandId));
		}
		final var revisionResult = revisionService.createErrandRevision(entity);
		if (nonNull(revisionResult)) {
			try {
				eventService.createErrandEvent(UPDATE, EVENT_LOG_REMOVE_ATTACHMENT, errandEntity, revisionResult.latest(), revisionResult.previous(), ATTACHMENT);
			} catch (final Exception e) {
				LOG.warn("Failed to log attachment-removed event for errand {}: {}", errandEntity.getId(), e.getMessage());
			}
		}
	}

	@Transactional
	public void createErrandAttachment(final AttachmentEntity attachmentEntity, final ErrandEntity errandEntity) {
		attachmentRepository.saveAndFlush(attachmentEntity);

		// Compute hash by streaming from the persisted database blob
		computeAndSetHash(attachmentEntity);

		final var revisionResult = revisionService.createErrandRevision(errandEntity);
		if (revisionResult != null) {
			try {
				eventService.createErrandEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandEntity, revisionResult.latest(), revisionResult.previous(), ATTACHMENT);
			} catch (final Exception e) {
				LOG.warn("Failed to log attachment-added event for errand {}: {}", errandEntity.getId(), e.getMessage());
			}
		}
	}

	@Transactional(readOnly = true)
	public List<AttachmentEntity> findByNamespaceAndMunicipalityIdAndErrandIdAndIdIn(final String namespace, final String municipalityId, final String errandId, final List<String> attachmentIds) {
		if (attachmentIds == null) {
			return emptyList();
		}
		// Scoped to the errand, so an attachment of another errand cannot be pulled into a message.
		final var attachments = attachmentRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndIdIn(namespace, municipalityId, errandId, attachmentIds);
		if (attachments.size() != attachmentIds.size()) {
			throw Problem.valueOf(BAD_REQUEST, "There was a mismatch in the given attachment Ids and the found attachments.");
		}
		return attachments;
	}

	void streamAttachmentData(final AttachmentEntity attachment, final HttpServletResponse response) {
		final var fileSize = attachment.getFileSize();

		if (fileSize == null || fileSize == 0) {
			throw Problem.valueOf(NOT_FOUND, "Attachment with id '%s' has no data".formatted(attachment.getId()));
		}

		try {
			if (!semaphore.tryAcquire(fileSize, 5, TimeUnit.SECONDS)) {
				throw Problem.valueOf(INSUFFICIENT_STORAGE, "Insufficient storage available to process the request.");
			}
			response.addHeader(CONTENT_TYPE, attachment.getMimeType());
			response.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"");
			response.setContentLength(fileSize);
			StreamUtils.copy(attachment.getAttachmentData().getFile().getBinaryStream(), response.getOutputStream());
		} catch (final IOException | SQLException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "%s occurred when copying file with attachment id '%s' to response: %s".formatted(e.getClass().getSimpleName(), attachment.getId(), e.getMessage()));
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		} finally {
			semaphore.release(fileSize);
		}
	}

	/**
	 * The purpose is part of the errand as its revisions record it, so a change gets a revision and an event of its own -
	 * otherwise it would surface in the next unrelated revision, attributed to whoever made that one. Flushed before the
	 * snapshot, which would otherwise hold a modified timestamp the commit then replaces.
	 */
	private void recordChange(final ErrandEntity errandEntity) {
		attachmentRepository.flush();

		ofNullable(revisionService.createErrandRevision(errandEntity)).ifPresent(revisionResult -> {
			try {
				eventService.createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ATTACHMENT, errandEntity, revisionResult.latest(), revisionResult.previous(), ATTACHMENT);
			} catch (final Exception e) {
				LOG.warn("Failed to log attachment-updated event for errand {}: {}", errandEntity.getId(), e.getMessage());
			}
		});
	}

	private AttachmentEntity findAttachmentOrElseThrow(final ErrandEntity errandEntity, final String errandId, final String attachmentId) {
		return ofNullable(errandEntity.getAttachments()).orElse(emptyList()).stream()
			.filter(attachment -> attachment.getId().equalsIgnoreCase(attachmentId))
			.findAny()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ATTACHMENT_ENTITY_NOT_FOUND.formatted(attachmentId, errandId)));
	}

	/**
	 * Looked up within the namespace, so a purpose of another namespace is refused rather than borrowed.
	 */
	private AttachmentPurposeEntity findPurposeOrElseThrow(final String namespace, final String municipalityId, final String purposeId) {
		return attachmentPurposeRepository.findByIdAndNamespaceAndMunicipalityId(purposeId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, ATTACHMENT_PURPOSE_NOT_FOUND.formatted(purposeId, namespace, municipalityId)));
	}

	private void computeAndSetHash(final AttachmentEntity attachmentEntity) {
		try {
			// Refresh to get a database-backed blob (the in-memory BlobProxy is not re-readable after flush)
			entityManager.refresh(attachmentEntity);
			final var hash = computeSha256Hex(attachmentEntity.getAttachmentData().getFile().getBinaryStream());
			attachmentEntity.setHash(hash);
		} catch (final SQLException e) {
			LOG.warn("Failed to compute hash for attachment {}: {}", attachmentEntity.getId(), e.getMessage());
		}
	}
}
