package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static se.sundsvall.supportmanagement.integration.db.specification.ErrandSpecification.hasMatchingTags;

import generated.se.sundsvall.webmessagecollector.MessageDTO;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.webmessagecollector.WebMessageCollectorClient;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.EventService;

@Component
public class WebMessageCollectorWorker {

	private static final String EVENT_LOG_COMMUNICATION = "Nytt meddelande";

	private final WebMessageCollectorClient webMessageCollectorClient;
	private final ErrandsRepository errandsRepository;
	private final CommunicationRepository communicationRepository;
	private final WebMessageCollectorMapper webMessageCollectorMapper;
	private final EventService eventService;
	private final CommunicationService communicationService;

	public WebMessageCollectorWorker(final WebMessageCollectorClient webMessageCollectorClient, final ErrandsRepository errandsRepository,
		final CommunicationRepository communicationRepository, final WebMessageCollectorMapper webMessageCollectorMapper, final EventService eventService,
		final CommunicationService communicationService) {
		this.webMessageCollectorClient = webMessageCollectorClient;
		this.errandsRepository = errandsRepository;
		this.communicationRepository = communicationRepository;
		this.webMessageCollectorMapper = webMessageCollectorMapper;
		this.eventService = eventService;
		this.communicationService = communicationService;
	}

	public List<MessageDTO> getWebMessages(final String instance, final String familyId, final String municipalityId) {
		return webMessageCollectorClient.getMessages(municipalityId, familyId, instance);
	}

	@Transactional
	public void processMessage(final MessageDTO message, final String municipalityId) {

		final var entity = errandsRepository.findOne(hasMatchingTags(List.of(
			DbExternalTag.create().withKey("caseId").withValue(message.getExternalCaseId()),
			DbExternalTag.create().withKey("familyId").withValue(message.getFamilyId()))));

		if (entity.isPresent()) {
			entity
				.filter(errand -> !communicationRepository.existsByErrandNumberAndExternalId(errand.getErrandNumber(), message.getMessageId()))
				.map(errand -> webMessageCollectorMapper.toCommunicationEntity(message, errand))
				.map(this::addAttachments)
				.ifPresent(communicationEntity -> saveMessage(communicationEntity, entity.get()));

			webMessageCollectorClient.deleteMessages(municipalityId, List.of(message.getId()));
		}
	}

	private void saveMessage(final CommunicationEntity communicationEntity, final ErrandEntity errand) {
		communicationService.saveCommunication(communicationEntity);
		communicationService.saveAttachment(communicationEntity, errand);
		eventService.createErrandEvent(UPDATE, EVENT_LOG_COMMUNICATION, errand, null, null);
	}

	private CommunicationEntity addAttachments(final CommunicationEntity communicationEntity) {
		communicationEntity.getAttachments().forEach(this::addAttachment);
		return communicationEntity;
	}

	private void addAttachment(final CommunicationAttachmentEntity attachment) {
		final var attachmentData = webMessageCollectorClient.getAttachment(attachment.getMunicipalityId(), Integer.parseInt(attachment.getForeignId()));
		attachment.withAttachmentData(webMessageCollectorMapper.toCommunicationAttachmentDataEntity(attachmentData))
			.withFileSize(attachmentData.length);
	}
}
