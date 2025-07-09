package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.MESSAGE;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.mergeIntoConversationEntity;

import generated.se.sundsvall.messageexchange.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;

@Service
public class MessageExchangeSyncService {

	private static final String EVENT_LOG_CONVERSATION = "Ny händelse i konversation %s";

	private final MessageExchangeClient messageExchangeClient;
	private final ErrandAttachmentService attachmentService;
	private final ConversationRepository conversationRepository;
	private final EventService eventService;
	private final ErrandsRepository errandsRepository;

	@Value("${integration.message-exchange.namespace:supportmanagement}")
	private String messageExchangeNamespace;

	public MessageExchangeSyncService(final MessageExchangeClient messageExchangeClient, final ErrandAttachmentService attachmentService, final ConversationRepository conversationRepository, final EventService eventService,
		final ErrandsRepository errandsRepository) {

		this.messageExchangeClient = messageExchangeClient;
		this.attachmentService = attachmentService;
		this.conversationRepository = conversationRepository;
		this.eventService = eventService;
		this.errandsRepository = errandsRepository;
	}

	public void syncConversation(final ConversationEntity conversationEntity, final generated.se.sundsvall.messageexchange.Conversation conversation) {
		if (ofNullable(conversationEntity.getLatestSyncedSequenceNumber()).orElse(0L) < ofNullable(conversation.getLatestSequenceNumber()).orElse(0L)) {
			final var errandEntity = errandsRepository.getReferenceById(conversationEntity.getErrandId());
			final var shouldCreateNotification = syncMessages(conversationEntity, errandEntity.getAssignedUserId());
			eventService.createErrandEvent(UPDATE, EVENT_LOG_CONVERSATION.formatted(conversation.getTopic()), errandEntity, null, null, shouldCreateNotification, MESSAGE);
		}

		final var updatedConversationEntity = mergeIntoConversationEntity(conversationEntity, conversation);
		conversationRepository.save(updatedConversationEntity);
	}

	boolean syncMessages(final ConversationEntity conversationEntity, String errandAssignedUserId) {

		final var filter = "sequenceNumber.id >" + ofNullable(conversationEntity.getLatestSyncedSequenceNumber()).orElse(0L);

		final var response = messageExchangeClient.getMessages(conversationEntity.getMunicipalityId(), messageExchangeNamespace, conversationEntity.getMessageExchangeId(), filter, Pageable.unpaged());

		if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to retrieve messages from Message Exchange");
		}

		response.getBody().forEach(message -> message.getAttachments().forEach(attachment -> syncAttachment(conversationEntity, message, attachment)));

		return !response.getBody().stream()
			.allMatch(message -> message.getCreatedBy() != null && message.getCreatedBy().getValue().equals(errandAssignedUserId));
	}

	void syncAttachment(final ConversationEntity conversationEntity, final Message message, final generated.se.sundsvall.messageexchange.Attachment attachment) {
		final var file = messageExchangeClient.getMessageAttachment(conversationEntity.getMunicipalityId(), messageExchangeNamespace, conversationEntity.getMessageExchangeId(), message.getId(), attachment.getId());
		saveAttachment(conversationEntity.getErrandId(), conversationEntity.getMunicipalityId(), conversationEntity.getNamespace(), file);
	}

	void saveAttachment(final String errandId, final String municipalityId, final String namespace, final ResponseEntity<InputStreamResource> file) {
		if (file.getBody() == null || file.getHeaders().getContentType() == null) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to retrieve attachment from Message Exchange");
		}

		attachmentService.createErrandAttachment(namespace, municipalityId, errandId, file);
	}
}
