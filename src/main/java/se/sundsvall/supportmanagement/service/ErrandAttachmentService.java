package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INSUFFICIENT_STORAGE;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.ATTACHMENT;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toAttachmentEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachments;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Service
@Transactional
public class ErrandAttachmentService {

	private static final String ATTACHMENT_ENTITY_NOT_FOUND = "An attachment with id '%s' could not be found on errand with id '%s'";
	private static final String ATTACHMENT_ENTITY_NOT_CREATED = "Attachment could not be created";
	private static final String EVENT_LOG_ADD_ATTACHMENT = "En bilaga har lagts till i ärendet.";
	private static final String EVENT_LOG_REMOVE_ATTACHMENT = "En bilaga har tagits bort från ärendet.";

	private final ErrandsRepository errandsRepository;
	private final AccessControlService accessControlService;
	private final AttachmentRepository attachmentRepository;
	private final RevisionService revisionService;
	private final EventService eventService;
	private final EntityManager entityManager;
	private final Semaphore semaphore;

	public ErrandAttachmentService(
		final ErrandsRepository errandsRepository,
		final AccessControlService accessControlService,
		final RevisionService revisionService, final EventService eventService,
		final AttachmentRepository attachmentRepository, final EntityManager entityManager, final Semaphore semaphore) {
		this.errandsRepository = errandsRepository;
		this.accessControlService = accessControlService;
		this.revisionService = revisionService;
		this.eventService = eventService;
		this.attachmentRepository = attachmentRepository;
		this.entityManager = entityManager;
		this.semaphore = semaphore;
	}

	public String createErrandAttachment(final String namespace, final String municipalityId, final String errandId, final MultipartFile errandAttachment) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);

		return createErrandAttachmentInternal(errandEntity, () -> toAttachmentEntity(errandEntity, errandAttachment, entityManager));
	}

	public String createErrandAttachment(final String namespace, final String municipalityId, final String errandId, final ResponseEntity<InputStreamResource> file) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);

		return createErrandAttachmentInternal(errandEntity, () -> toAttachmentEntity(errandEntity, file, entityManager));
	}

	private String createErrandAttachmentInternal(final ErrandEntity errandEntity,
		final Supplier<AttachmentEntity> attachmentEntitySupplier) {
		var attachmentEntity = ofNullable(attachmentEntitySupplier.get())
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ATTACHMENT_ENTITY_NOT_CREATED));

		// Save
		attachmentEntity = attachmentRepository.save(attachmentEntity);
		errandEntity.getAttachments().add(attachmentEntity);

		// Update errand with new attachment and create new revision
		final var revisionResult = revisionService.createErrandRevision(errandEntity);

		// Create log event
		eventService.createErrandEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandEntity, revisionResult.latest(), revisionResult.previous(), ATTACHMENT);

		return attachmentEntity.getId();
	}

	public void readErrandAttachment(final String namespace, final String municipalityId, final String errandId, final String attachmentId, final HttpServletResponse response) {

		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, R, RW);

		final var attachmentEntity = attachmentRepository
			.findById(attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_NOT_FOUND, attachmentId, errandId)));

		streamAttachmentData(attachmentEntity, response);
	}

	public List<ErrandAttachment> readErrandAttachments(final String namespace, final String municipalityId, final String errandId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, R, RW);
		return toErrandAttachments(errandEntity.getAttachments());
	}

	public void deleteErrandAttachment(final String namespace, final String municipalityId, final String errandId, final String attachmentId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);
		final var attachmentEntity = ofNullable(errandEntity.getAttachments()).orElse(emptyList()).stream()
			.filter(attachment -> attachment.getId().equalsIgnoreCase(attachmentId))
			.findAny()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_NOT_FOUND, attachmentId, errandId)));

		final ErrandEntity entity;
		try {
			// Update errand after removal of attachment and create new revision
			errandEntity.getAttachments().remove(attachmentEntity);
			entity = errandsRepository.save(errandEntity);

		} catch (final Exception e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, String.format("Failed to delete attachment with id '%s' from errand with id '%s'", attachmentId, errandId));
		}
		final var revisionResult = revisionService.createErrandRevision(entity);
		// Create log event
		if (nonNull(revisionResult)) {
			eventService.createErrandEvent(UPDATE, EVENT_LOG_REMOVE_ATTACHMENT, errandEntity, revisionResult.latest(), revisionResult.previous(), ATTACHMENT);
		}
	}

	public void createErrandAttachment(final AttachmentEntity attachmentEntity, final ErrandEntity errandEntity) {
		attachmentRepository.saveAndFlush(attachmentEntity);
		final var revisionResult = revisionService.createErrandRevision(errandEntity);
		if (revisionResult != null) {
			eventService.createErrandEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandEntity, revisionResult.latest(), revisionResult.previous(), ATTACHMENT);
		}
	}

	public List<AttachmentEntity> findByNamespaceAndMunicipalityIdAndIdIn(final String namespace, final String municipalityId, final List<String> attachmentIds) {
		if (attachmentIds == null) {
			return emptyList();
		}
		final var attachments = attachmentRepository.findByNamespaceAndMunicipalityIdAndIdIn(namespace, municipalityId, attachmentIds);
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
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			semaphore.release(fileSize);
		}
	}
}
