package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static se.sundsvall.supportmanagement.integration.db.specification.ErrandSpecification.hasMatchingTags;

import generated.se.sundsvall.webmessagecollector.MessageDTO;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.webmessagecollector.WebMessageCollectorClient;
import se.sundsvall.supportmanagement.service.EventService;

@Component
public class WebMessageCollectorWorker {

	private static final String EVENT_LOG_COMMUNICATION = "Ärendekommunikation har skapats.";

	private final WebMessageCollectorClient webMessageCollectorClient;
	private final ErrandsRepository errandsRepository;
	private final CommunicationRepository communicationRepository;
	private final WebMessageCollectorMapper webMessageCollectorMapper;
	private final EventService eventService;

	public WebMessageCollectorWorker(final WebMessageCollectorClient webMessageCollectorClient, final ErrandsRepository errandsRepository,
		final CommunicationRepository communicationRepository, final WebMessageCollectorMapper webMessageCollectorMapper, final EventService eventService) {
		this.webMessageCollectorClient = webMessageCollectorClient;
		this.errandsRepository = errandsRepository;
		this.communicationRepository = communicationRepository;
		this.webMessageCollectorMapper = webMessageCollectorMapper;
		this.eventService = eventService;
	}

	public List<MessageDTO> getWebMessages(final String instance, final String familyId, final String municipalityId) {
		return webMessageCollectorClient.getMessages(municipalityId, familyId, instance);
	}

	@Transactional
	public void processMessage(MessageDTO message, String municipalityId) {

		final var entity = errandsRepository.findOne(hasMatchingTags(List.of(
			DbExternalTag.create().withKey("caseId").withValue(message.getExternalCaseId()),
			DbExternalTag.create().withKey("familyId").withValue(message.getFamilyId()))));

		if (entity.isPresent()) {
			entity
				.filter(errand -> !communicationRepository.existsByErrandNumberAndExternalId(errand.getErrandNumber(), message.getMessageId()))
				.map(errand -> processMessage(message, errand))
				.stream()
				.flatMap(Collection::stream)
				.forEach(this::processAttachment);

			webMessageCollectorClient.deleteMessages(municipalityId, List.of(message.getId()));
		}
	}

	private List<CommunicationAttachmentEntity> processMessage(final MessageDTO messageDTO, final ErrandEntity errand) {
		final var entity = webMessageCollectorMapper.toCommunicationEntity(messageDTO, errand);
		communicationRepository.saveAndFlush(entity);
		eventService.createErrandEvent(UPDATE, EVENT_LOG_COMMUNICATION, errand, null, null);

		return entity.getAttachments();
	}

	private void processAttachment(final CommunicationAttachmentEntity attachment) {
		final var attachmentData = webMessageCollectorClient.getAttachment(attachment.getMunicipalityId(), Integer.parseInt(attachment.getForeignId()));
		attachment.setAttachmentData(webMessageCollectorMapper.toCommunicationAttachmentDataEntity(attachmentData));
		communicationRepository.saveAndFlush(attachment.getCommunicationEntity());
	}
}
