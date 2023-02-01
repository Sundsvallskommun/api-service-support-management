package se.sundsvall.supportmanagement.service;

import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toAttachmentEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachment;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachments;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Service
public class ErrandAttachmentService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found";
	private static final String ATTACHMENT_ENTITY_NOT_FOUND = "An attachment with id '%s' could not be found";
	private static final String ATTACHMENT_ENTITY_NOT_CREATED = "Attachment could not be created";
	private static final String ATTACHMENT_ENTITY_DO_NOT_BELONG_TO_ERRAND = "Attachment with id '%s' was not found for errand with id '%s'";

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Autowired
	private ErrandsRepository errandsRepository;

	public String createErrandAttachment(final String errandId, final ErrandAttachment errandAttachment) {
		final var errandEntity = errandsRepository.findById(errandId).orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, errandId)));
		final var attachmentEntity = ofNullable(toAttachmentEntity(errandEntity, errandAttachment)).orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ATTACHMENT_ENTITY_NOT_CREATED));
		return attachmentRepository.save(attachmentEntity).getId();
	}

	public ErrandAttachment readErrandAttachment(final String errandId, final String attachmentId) {
		verifyExistingErrand(errandId);

		final var attachmentEntity =  attachmentRepository.findById(attachmentId).orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_NOT_FOUND, attachmentId)));
		verifyAttachmentBelongsToErrand(errandId, attachmentEntity);

		return toErrandAttachment(attachmentEntity);
	}

	public List<ErrandAttachment> readErrandAttachments(final String errandId) {

		verifyExistingErrand(errandId);

		return toErrandAttachments(attachmentRepository.findByErrandEntityId(errandId));
	}

	public void deleteErrandAttachment(final String errandId, final String attachmentId) {

		verifyExistingErrand(errandId);

		attachmentRepository.deleteById(attachmentId);
	}

	private void verifyAttachmentBelongsToErrand(final String errandId, AttachmentEntity attachmentEntity) {
		final var attachmentErrandId = ofNullable(attachmentEntity.getErrandEntity()).map(ErrandEntity::getId).orElse(null);

		if(!Objects.equals(attachmentErrandId, errandId)) {
			throw Problem.valueOf(BAD_REQUEST, String.format(ATTACHMENT_ENTITY_DO_NOT_BELONG_TO_ERRAND, attachmentEntity.getId(), errandId));
		}
	}

	private void verifyExistingErrand(final String id) {
		if (!errandsRepository.existsById(id)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id));
		}
	}
}
