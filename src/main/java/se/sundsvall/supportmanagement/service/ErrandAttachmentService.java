package se.sundsvall.supportmanagement.service;

import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toAttachmentEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachment;
import static se.sundsvall.supportmanagement.service.mapper.ErrandAttachmentMapper.toErrandAttachmentHeaders;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Service
public class ErrandAttachmentService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found for municipality with id '%s'";
	private static final String ATTACHMENT_ENTITY_NOT_FOUND = "An attachment with id '%s' could not be found";
	private static final String ATTACHMENT_ENTITY_NOT_CREATED = "Attachment could not be created";
	private static final String ATTACHMENT_ENTITY_DO_NOT_BELONG_TO_ERRAND = "Attachment with id '%s' was not found for errand with id '%s'";

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Autowired
	private ErrandsRepository errandsRepository;

	public String createErrandAttachment(String municipalityId, String errandId, ErrandAttachment errandAttachment) {
		final var errandEntity = errandsRepository.findById(errandId)
			.filter(entity -> Objects.equals(municipalityId, entity.getMunicipalityId()))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, errandId, municipalityId)));

		final var attachmentEntity = ofNullable(toAttachmentEntity(errandEntity, errandAttachment)).orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ATTACHMENT_ENTITY_NOT_CREATED));
		return attachmentRepository.save(attachmentEntity).getId();
	}

	public ErrandAttachment readErrandAttachment(String municipalityId, String errandId, String attachmentId) {
		verifyExistingErrand(errandId, municipalityId);

		final var attachmentEntity =  attachmentRepository.findById(attachmentId).orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_NOT_FOUND, attachmentId)));
		verifyAttachmentBelongsToErrand(errandId, attachmentEntity);

		return toErrandAttachment(attachmentEntity);
	}

	public List<ErrandAttachmentHeader> readErrandAttachmentHeaders(String municipalityId, String errandId) {

		verifyExistingErrand(errandId, municipalityId);

		return toErrandAttachmentHeaders(attachmentRepository.findByErrandEntityId(errandId));
	}

	public void deleteErrandAttachment(String municipalityId, String errandId, String attachmentId) {

		verifyExistingErrand(errandId, municipalityId);

		attachmentRepository.deleteById(attachmentId);
	}

	private void verifyAttachmentBelongsToErrand(final String errandId, AttachmentEntity attachmentEntity) {
		final var attachmentErrandId = ofNullable(attachmentEntity.getErrandEntity()).map(ErrandEntity::getId).orElse(null);

		if(!Objects.equals(attachmentErrandId, errandId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ATTACHMENT_ENTITY_DO_NOT_BELONG_TO_ERRAND, attachmentEntity.getId(), errandId));
		}
	}

	private void verifyExistingErrand(String id, String municipalityId) {
		if (!errandsRepository.existsByIdAndMunicipalityId(id, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, municipalityId));
		}
	}
}
