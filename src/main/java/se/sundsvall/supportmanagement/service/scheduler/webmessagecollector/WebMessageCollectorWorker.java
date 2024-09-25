package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static se.sundsvall.supportmanagement.Constants.ERRAND_STATUS_ONGOING;
import static se.sundsvall.supportmanagement.Constants.ERRAND_STATUS_SOLVED;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import se.sundsvall.supportmanagement.service.EventService;

import generated.se.sundsvall.webmessagecollector.MessageDTO;

@Component
public class WebMessageCollectorWorker {

	private static final Logger LOG = LoggerFactory.getLogger(WebMessageCollectorWorker.class);

	private static final String EVENT_LOG_COMMUNICATION = "Ärendekommunikation har skapats.";

	private final WebMessageCollectorClient webMessageCollectorClient;

	private final WebMessageCollectorProperties webMessageCollectorProperties;

	private final ErrandsRepository errandsRepository;

	private final CommunicationRepository communicationRepository;

	private final WebMessageCollectorMapper webMessageCollectorMapper;

	private final EventService eventService;

	public WebMessageCollectorWorker(final WebMessageCollectorClient webMessageCollectorClient, final WebMessageCollectorProperties webMessageCollectorProperties, final ErrandsRepository errandsRepository, final CommunicationRepository communicationRepository, final WebMessageCollectorMapper webMessageCollectorMapper, final EventService eventService) {
		this.webMessageCollectorClient = webMessageCollectorClient;
		this.webMessageCollectorProperties = webMessageCollectorProperties;
		this.errandsRepository = errandsRepository;
		this.communicationRepository = communicationRepository;
		this.webMessageCollectorMapper = webMessageCollectorMapper;
		this.eventService = eventService;
	}

	@Transactional
	public Map<String, List<CommunicationAttachmentEntity>> fetchWebMessages() {
		return Optional.ofNullable(webMessageCollectorProperties.familyIds())
			.orElse(emptyMap())
			.entrySet().stream()
			.collect(toMap(
				Entry::getKey,
				municipalityIdEntry -> municipalityIdEntry.getValue().entrySet().stream()
					.flatMap(instanceEntry -> instanceEntry.getValue().stream()
						.flatMap(familyId -> getWebMessages(instanceEntry.getKey(), familyId, municipalityIdEntry.getKey())
							.stream()))
					.toList()
			));
	}

	private List<CommunicationAttachmentEntity> getWebMessages(final String instance, final String familyId, final String municipalityId) {
		final var messages = webMessageCollectorClient.getMessages(municipalityId, familyId, instance);
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
		final var entity = webMessageCollectorMapper.toCommunicationEntity(messageDTO, errand);
		communicationRepository.saveAndFlush(entity);
		eventService.createErrandEvent(UPDATE, EVENT_LOG_COMMUNICATION, errand, null, null);
		return entity.getAttachments();
	}


	@Transactional
	public void processAttachments(final CommunicationAttachmentEntity attachment, final String municipalityId) {
		final var attachmentData = webMessageCollectorClient.getAttachment(municipalityId, Integer.parseInt(attachment.getId()));
		attachment.setAttachmentData(webMessageCollectorMapper.toCommunicationAttachmentDataEntity(attachmentData));
		communicationRepository.saveAndFlush(attachment.getCommunicationEntity());
	}

	private void updateErrandStatus(final ErrandEntity errand) {
		if (errand.getStatus().equals(ERRAND_STATUS_SOLVED)) {
			errandsRepository.save(errand.withStatus(ERRAND_STATUS_ONGOING));
		}
	}

	private boolean shouldBeUpdated(final ErrandEntity errand) {
		return errand.getStatus().equals(ERRAND_STATUS_SOLVED)
			&& errand.getTouched().isAfter(OffsetDateTime.now().minusDays(5));
	}


}
