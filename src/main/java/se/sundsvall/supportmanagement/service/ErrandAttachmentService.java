package se.sundsvall.supportmanagement.service;

import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toAttachmentEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachmentHeaders;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper;

@Service
@Transactional
public class ErrandAttachmentService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String ATTACHMENT_ENTITY_NOT_FOUND = "An attachment with id '%s' could not be found on errand with id '%s'";
	private static final String ATTACHMENT_ENTITY_NOT_CREATED = "Attachment could not be created";

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private RevisionService revisionService;

	public String createErrandAttachment(String namespace, String municipalityId, String errandId, ErrandAttachment errandAttachment) {
		final var errandEntity = getErrand(errandId, namespace, municipalityId);
		final var attachmentEntity = ofNullable(toAttachmentEntity(errandEntity, errandAttachment)).orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ATTACHMENT_ENTITY_NOT_CREATED));
		final var attachmentId = attachmentRepository.save(attachmentEntity).getId();

		revisionService.createRevision(errandEntity);
		return attachmentId;
	}

	public ErrandAttachment readErrandAttachment(String namespace, String municipalityId, String errandId, String attachmentId) {
		final var errandEntity = getErrand(errandId, namespace, municipalityId);
		return ofNullable(errandEntity.getAttachments()).orElse(Collections.emptyList()).stream()
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
		final var attachmentEntity = ofNullable(errandEntity.getAttachments()).orElse(Collections.emptyList()).stream()
			.filter(attachment -> attachment.getId().equalsIgnoreCase(attachmentId))
			.findAny()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_NOT_FOUND, attachmentId, errandId)));

		errandEntity.getAttachments().remove(attachmentEntity);
		attachmentRepository.deleteById(attachmentEntity.getId());
		revisionService.createRevision(errandEntity);
	}

	private ErrandEntity getErrand(String errandId, String namespace, String municipalityId) {
		return errandsRepository.findById(errandId)
			.filter(entity -> Objects.equals(namespace, entity.getNamespace()))
			.filter(entity -> Objects.equals(municipalityId, entity.getMunicipalityId()))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, errandId, namespace, municipalityId)));
	}
}
