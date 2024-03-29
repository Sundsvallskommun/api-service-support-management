package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static se.sundsvall.supportmanagement.Constants.ERRAND_STATUS_ONGOING;
import static se.sundsvall.supportmanagement.Constants.ERRAND_STATUS_SOLVED;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.webmessagecollector.WebMessageCollectorClient;
import se.sundsvall.supportmanagement.integration.webmessagecollector.configuration.WebMessageCollectorProperties;

import generated.se.sundsvall.webmessagecollector.MessageDTO;

@Component
public class WebMessageCollectorWorker {

	private static final Logger LOG = LoggerFactory.getLogger(WebMessageCollectorWorker.class);

	private final WebMessageCollectorClient webMessageCollectorClient;

	private final WebMessageCollectorProperties webMessageCollectorProperties;

	private final ErrandsRepository errandsRepository;

	private final CommunicationRepository communicationRepository;

	private final WebMessageCollectorMapper webMessageCollectorMapper;

	public WebMessageCollectorWorker(final WebMessageCollectorClient webMessageCollectorClient, final WebMessageCollectorProperties webMessageCollectorProperties, final ErrandsRepository errandsRepository, final CommunicationRepository communicationRepository, final WebMessageCollectorMapper webMessageCollectorMapper) {
		this.webMessageCollectorClient = webMessageCollectorClient;
		this.webMessageCollectorProperties = webMessageCollectorProperties;
		this.errandsRepository = errandsRepository;
		this.communicationRepository = communicationRepository;
		this.webMessageCollectorMapper = webMessageCollectorMapper;
	}

	@Transactional
	public List<CommunicationAttachmentEntity> fetchWebMessages() {
		return webMessageCollectorProperties.familyIds().stream()
			.flatMap(familyId -> getWebMessages(familyId).stream())
			.toList();
	}

	private List<CommunicationAttachmentEntity> getWebMessages(final String familyId) {
		final var messages = webMessageCollectorClient.getMessages(familyId);
		LOG.info("Got {} messages from the WebMessageCollectorClient", messages.size());

		return processMessages(messages);
	}

	private List<CommunicationAttachmentEntity> processMessages(final List<MessageDTO> messages) {
		return messages.stream()
			.map(messageDTO -> errandsRepository.findByExternalTagsValue(messageDTO.getExternalCaseId())
				.filter(this::shouldBeUpdated)
				.map(errand -> processMessage(messageDTO, errand)))
			.filter(Optional::isPresent)
			.flatMap(optional -> optional.get().stream())
			.toList();
	}

	private List<CommunicationAttachmentEntity> processMessage(final MessageDTO messageDTO, final ErrandEntity errand) {
		updateErrandStatus(errand);
		final var entity = webMessageCollectorMapper.toCommunicationEntity(messageDTO, errand.getErrandNumber());
		communicationRepository.saveAndFlush(entity);
		return entity.getAttachments();
	}


	@Transactional
	public void processAttachments(final CommunicationAttachmentEntity attachment) {
		final var attachmentData = webMessageCollectorClient.getAttachment(Integer.parseInt(attachment.getId()));
		attachment.setAttachmentData(webMessageCollectorMapper.toCommunicationAttachmentDataEntity(attachmentData));
		communicationRepository.saveAndFlush(attachment.getCommunicationEntity());
	}

	private void updateErrandStatus(final ErrandEntity errand) {
		if (errand.getStatus().equals(ERRAND_STATUS_SOLVED)) {
			errand.setStatus(ERRAND_STATUS_ONGOING);
			errandsRepository.save(errand);
		}
	}

	private boolean shouldBeUpdated(final ErrandEntity errand) {
		return errand.getStatus().equals(ERRAND_STATUS_SOLVED)
			&& errand.getTouched().isAfter(OffsetDateTime.now().minusDays(5));
	}


}
