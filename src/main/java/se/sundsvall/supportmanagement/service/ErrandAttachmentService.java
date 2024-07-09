package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toAttachmentEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachmentHeaders;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Service
@Transactional
public class ErrandAttachmentService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String ATTACHMENT_ENTITY_NOT_FOUND = "An attachment with id '%s' could not be found on errand with id '%s'";
	private static final String ATTACHMENT_ENTITY_NOT_CREATED = "Attachment could not be created";
	private static final String EVENT_LOG_ADD_ATTACHMENT = "En bilaga har lagts till i ärendet.";
	private static final String EVENT_LOG_REMOVE_ATTACHMENT = "En bilaga har tagits bort från ärendet.";

	private final ErrandsRepository errandsRepository;
	private final AttachmentRepository attachmentRepository;

	private final RevisionService revisionService;
	private final EventService eventService;

	private final EntityManager entityManager;

	public ErrandAttachmentService(final ErrandsRepository errandsRepository,
		final RevisionService revisionService, final EventService eventService,
		final AttachmentRepository attachmentRepository, final EntityManager entityManager) {
		this.errandsRepository = errandsRepository;
		this.revisionService = revisionService;
		this.eventService = eventService;
		this.attachmentRepository = attachmentRepository;
		this.entityManager = entityManager;
	}

	public String createErrandAttachment(String namespace, String municipalityId, String errandId, MultipartFile errandAttachment) {
		final var errandEntity = getErrand(errandId, namespace, municipalityId, true);
		var attachmentEntity = ofNullable(toAttachmentEntity(errandEntity, errandAttachment, entityManager))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ATTACHMENT_ENTITY_NOT_CREATED));

		// Save
		attachmentEntity = attachmentRepository.save(attachmentEntity);
		errandEntity.getAttachments().add(attachmentEntity);

		// Update errand with new attachment and create new revision
		final var revisionResult = revisionService.createErrandRevision(errandEntity);


		// Create log event
		eventService.createErrandEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandEntity, revisionResult.latest(), revisionResult.previous());

		return attachmentEntity.getId();
	}

	public void readErrandAttachment(String namespace, String municipalityId, String errandId, String attachmentId, HttpServletResponse response) throws SQLException, IOException {

		if (!errandsRepository.existsByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, errandId, namespace, municipalityId));
		}

		final var attachmentEntity = attachmentRepository
			.findById(attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_NOT_FOUND, attachmentId, errandId)));

		final var file = attachmentEntity.getAttachmentData().getFile();

		response.addHeader(CONTENT_TYPE, attachmentEntity.getMimeType());
		response.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentEntity.getFileName() + "\"");
		response.setContentLength((int) file.length());

		StreamUtils.copy(file.getBinaryStream(), response.getOutputStream());
	}

	public List<ErrandAttachmentHeader> readErrandAttachmentHeaders(String namespace, String municipalityId, String errandId) {
		final var errandEntity = getErrand(errandId, namespace, municipalityId, false);
		return toErrandAttachmentHeaders(errandEntity.getAttachments());
	}

	public void deleteErrandAttachment(String namespace, String municipalityId, String errandId, String attachmentId) {
		final var errandEntity = getErrand(errandId, namespace, municipalityId, true);
		final var attachmentEntity = ofNullable(errandEntity.getAttachments()).orElse(emptyList()).stream()
			.filter(attachment -> attachment.getId().equalsIgnoreCase(attachmentId))
			.findAny()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_NOT_FOUND, attachmentId, errandId)));

		// Update errand after removal of attachment and create new revision
		errandEntity.getAttachments().remove(attachmentEntity);
		final var revisionResult = revisionService.createErrandRevision(errandsRepository.save(errandEntity));

		// Create log event
		eventService.createErrandEvent(UPDATE, EVENT_LOG_REMOVE_ATTACHMENT, errandEntity, revisionResult.latest(), revisionResult.previous());
	}

	private ErrandEntity getErrand(String errandId, String namespace, String municipalityId, boolean lock) {

		Supplier<Optional<ErrandEntity>> optionalErrand;
		if (lock) {
			optionalErrand = () -> errandsRepository.findWithLockingById(errandId);
		} else {
			optionalErrand = () -> errandsRepository.findById(errandId);
		}

		return optionalErrand.get().filter(entity -> Objects.equals(namespace, entity.getNamespace()))
			.filter(entity -> Objects.equals(municipalityId, entity.getMunicipalityId()))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, errandId, namespace, municipalityId)));
	}

	public void createErrandAttachment(final AttachmentEntity attachmentEntity, final ErrandEntity errandEntity) {
		attachmentRepository.save(attachmentEntity);
		final var revisionResult = revisionService.createErrandRevision(errandEntity);
		if (revisionResult != null) {
			eventService.createErrandEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandEntity, revisionResult.latest(), revisionResult.previous());
		}
	}

	public List<AttachmentEntity> findByIdIn(final List<String> attachmentIds) {
		if (attachmentIds == null) {
			return emptyList();
		}
		var attachments = attachmentRepository.findByIdIn(attachmentIds);
		if (attachments.size() != attachmentIds.size()) {
			throw Problem.valueOf(BAD_REQUEST, "There was a mismatch in the given attachment Ids and the found attachments.");
		}
		return attachments;
	}

}