package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.mergeIntoConversationEntity;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toConversation;

import generated.se.sundsvall.messageexchange.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Conversation;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;
import se.sundsvall.supportmanagement.service.util.ConversationEvent;

@Service
public class MessageExchangeSyncService {

	private final MessageExchangeClient messageExchangeClient;
	private final ErrandAttachmentService attachmentService;
	private final ConversationRepository conversationRepository;
	private final ApplicationEventPublisher applicationEventPublisher;

	@Value("${integration.message-exchange.namespace:casedata}")
	private String messageExchangeNamespace;

	public MessageExchangeSyncService(final MessageExchangeClient messageExchangeClient, final ErrandAttachmentService attachmentService, final ConversationRepository conversationRepository, final ApplicationEventPublisher applicationEventPublisher) {
		this.messageExchangeClient = messageExchangeClient;
		this.attachmentService = attachmentService;
		this.conversationRepository = conversationRepository;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public Conversation syncConversation(final ConversationEntity conversationEntity, final generated.se.sundsvall.messageexchange.Conversation conversation) {
		// TODO: Create notification if sequence number is not the latest
		applicationEventPublisher.publishEvent(ConversationEvent.create().withConversationEntity(conversationEntity).withRequestId(RequestId.get()));
		final var updatedConversation = toConversation(conversation, conversationEntity);
		conversationRepository.save(mergeIntoConversationEntity(conversationEntity, conversation));
		return updatedConversation;
	}

	@TransactionalEventListener
	void syncMessages(final ConversationEvent conversationEvent) {
		final var conversationEntity = conversationEvent.getConversationEntity();
		RequestId.init(conversationEvent.getRequestId());

		final var filter = "sequenceNumber >" + conversationEntity.getLatestSyncedSequenceNumber();

		final var response = messageExchangeClient.getMessages(conversationEntity.getMunicipalityId(), conversationEntity.getNamespace(), conversationEntity.getMessageExchangeId(), filter, Pageable.unpaged());

		if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to retrieve messages from Message Exchange");
		}

		response.getBody().forEach(message -> message.getAttachments().forEach(attachment -> syncAttachment(conversationEntity, message, attachment)));
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
