package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toAttachmentEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachmentHeaders;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.filter.ExecutingUserSupplier;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper;

@Service
@Transactional
public class ErrandAttachmentService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String ATTACHMENT_ENTITY_NOT_FOUND = "An attachment with id '%s' could not be found on errand with id '%s'";
	private static final String ATTACHMENT_ENTITY_NOT_CREATED = "Attachment could not be created";
	private static final String EVENT_LOG_ADD_ATTACHMENT = "En bilaga har lagts till i ärendet.";
	private static final String EVENT_LOG_REMOVE_ATTACHMENT = "En bilaga har tagits bort från ärendet.";

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private RevisionService revisionService;

	@Autowired
	private EventService eventService;

	@Autowired
	private ExecutingUserSupplier executingUserSupplier;

	public String createErrandAttachment(String namespace, String municipalityId, String errandId, ErrandAttachment errandAttachment) {
		final var errandEntity = getErrand(errandId, namespace, municipalityId);
		final var attachmentEntity = ofNullable(toAttachmentEntity(errandEntity, errandAttachment)).orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ATTACHMENT_ENTITY_NOT_CREATED));

		// Update errand with new attachment and create new revision
		final var latestRevision = revisionService.createErrandRevision(errandsRepository.save(errandEntity));

		// Create log event
		final var previousRevision = revisionService.getErrandRevisionByVersion(errandId, latestRevision.getVersion() - 1);
		eventService.createEvent(UPDATE, EVENT_LOG_ADD_ATTACHMENT, errandEntity, latestRevision, previousRevision, executingUserSupplier.getAdUser());

		return attachmentEntity.getId();
	}

	public ErrandAttachment readErrandAttachment(String namespace, String municipalityId, String errandId, String attachmentId) {
		final var errandEntity = getErrand(errandId, namespace, municipalityId);
		return ofNullable(errandEntity.getAttachments()).orElse(emptyList()).stream()
			.filter(attachment -> attachment.getId().equalsIgnoreCase(attachmentId))
			.findAny()
			.map(ErrandAttachmentMapper::toErrandAttachment)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_NOT_FOUND, attachmentId, errandId)));
	}

	public List<ErrandAttachmentHeader> readErrandAttachmentHeaders(String namespace, String municipalityId, String errandId) {
		final var errandEntity = getErrand(errandId, namespace, municipalityId);
		return toErrandAttachmentHeaders(errandEntity.getAttachments());
	}

	public void deleteErrandAttachment(String namespace, String municipalityId, String errandId, String attachmentId) {
		final var errandEntity = getErrand(errandId, namespace, municipalityId);
		final var attachmentEntity = ofNullable(errandEntity.getAttachments()).orElse(emptyList()).stream()
			.filter(attachment -> attachment.getId().equalsIgnoreCase(attachmentId))
			.findAny()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_NOT_FOUND, attachmentId, errandId)));

		// Update errand after removal of attachment and create new revision
		errandEntity.getAttachments().remove(attachmentEntity);
		final var latestRevision = revisionService.createErrandRevision(errandsRepository.save(errandEntity));

		// Create log event
		final var previousRevision = revisionService.getErrandRevisionByVersion(errandId, latestRevision.getVersion() - 1);
		eventService.createEvent(UPDATE, EVENT_LOG_REMOVE_ATTACHMENT, errandEntity, latestRevision, previousRevision, executingUserSupplier.getAdUser());
	}

	private ErrandEntity getErrand(String errandId, String namespace, String municipalityId) {
		return errandsRepository.findById(errandId)
			.filter(entity -> Objects.equals(namespace, entity.getNamespace()))
			.filter(entity -> Objects.equals(municipalityId, entity.getMunicipalityId()))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, errandId, namespace, municipalityId)));
	}
}
