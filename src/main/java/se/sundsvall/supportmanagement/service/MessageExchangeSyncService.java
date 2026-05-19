package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.messageexchange.Attachment;
import generated.se.sundsvall.messageexchange.Message;
import java.util.Collections;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MessageExchangeIntegrationConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeIntegrationConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.mergeIntoConversationEntity;
import static se.sundsvall.supportmanagement.service.mapper.IdentifierMapper.resolveChannel;

@Service
public class MessageExchangeSyncService {

	private static final String EVENT_LOG_CONVERSATION = "Ny händelse för %s";

	private final MessageExchangeClient messageExchangeClient;
	private final ErrandAttachmentService attachmentService;
	private final ConversationRepository conversationRepository;
	private final EventService eventService;
	private final ErrandsRepository errandsRepository;
	private final MessageExchangeIntegrationConfigRepository messageExchangeIntegrationConfigRepository;

	@Value("${integration.message-exchange.namespace:supportmanagement}")
	private String messageExchangeNamespace;

	public MessageExchangeSyncService(final MessageExchangeClient messageExchangeClient, final ErrandAttachmentService attachmentService, final ConversationRepository conversationRepository, final EventService eventService,
		final ErrandsRepository errandsRepository, final MessageExchangeIntegrationConfigRepository messageExchangeIntegrationConfigRepository) {

		this.messageExchangeClient = messageExchangeClient;
		this.attachmentService = attachmentService;
		this.conversationRepository = conversationRepository;
		this.eventService = eventService;
		this.errandsRepository = errandsRepository;
		this.messageExchangeIntegrationConfigRepository = messageExchangeIntegrationConfigRepository;
	}

	public void syncConversation(final ConversationEntity conversationEntity, final generated.se.sundsvall.messageexchange.Conversation conversation) {
		if (ofNullable(conversationEntity.getLatestSyncedSequenceNumber()).orElse(0L) < ofNullable(conversation.getLatestSequenceNumber()).orElse(0L)) {
			final var errandEntity = errandsRepository.getReferenceById(conversationEntity.getErrandId());
			final var shouldCreateNotification = syncMessages(conversationEntity, errandEntity);
			eventService.createErrandEvent(UPDATE, EVENT_LOG_CONVERSATION.formatted(conversation.getTopic()), errandEntity, null, null, shouldCreateNotification, MESSAGE);
		}

		final var updatedConversationEntity = mergeIntoConversationEntity(conversationEntity, conversation);
		conversationRepository.save(updatedConversationEntity);
	}

	boolean syncMessages(final ConversationEntity conversationEntity, final ErrandEntity errandEntity) {

		final var filter = "sequenceNumber.id >" + ofNullable(conversationEntity.getLatestSyncedSequenceNumber()).orElse(0L);

		final var response = messageExchangeClient.getMessages(conversationEntity.getMunicipalityId(), messageExchangeNamespace, conversationEntity.getMessageExchangeId(), filter, Pageable.unpaged());

		if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to retrieve messages from Message Exchange");
		}

		response.getBody().forEach(message -> message.getAttachments().forEach(attachment -> syncAttachment(conversationEntity, message, attachment)));

		final var hasIncomingFromOther = containsMessageFromOtherThan(response.getBody(), errandEntity.getAssignedUserId());

		if (hasIncomingFromOther) {
			applyStatusChange(errandEntity);
		}

		return hasIncomingFromOther;
	}

	private static boolean containsMessageFromOtherThan(final Iterable<Message> messages, final String assignedUserId) {
		for (final var message : messages) {
			final var createdBy = message.getCreatedBy();
			if (createdBy == null || !Objects.equals(createdBy.getValue(), assignedUserId)) {
				return true;
			}
		}
		return false;
	}

	void applyStatusChange(final ErrandEntity errandEntity) {
		messageExchangeIntegrationConfigRepository.getByNamespaceAndMunicipalityId(errandEntity.getNamespace(), errandEntity.getMunicipalityId())
			.filter(config -> shouldTriggerStatusChange(config, errandEntity))
			.ifPresent(config -> {
				errandEntity.setStatus(config.getStatusChangeTo());
				errandsRepository.save(errandEntity);
			});
	}

	private static boolean shouldTriggerStatusChange(final MessageExchangeIntegrationConfigEntity config, final ErrandEntity errandEntity) {
		return config.getTriggerStatusChangeOn() != null
			&& config.getStatusChangeTo() != null
			&& Objects.equals(errandEntity.getStatus(), config.getTriggerStatusChangeOn());
	}

	void syncAttachment(final ConversationEntity conversationEntity, final Message message, final Attachment attachment) {
		final var errandEntity = errandsRepository.getReferenceById(conversationEntity.getErrandId());

		// Skip if attachment with same fileName already exists on the errand
		final var alreadyExists = ofNullable(errandEntity.getAttachments()).orElse(Collections.emptyList()).stream()
			.anyMatch(existing -> Objects.equals(existing.getFileName(), attachment.getFileName()));
		if (alreadyExists) {
			return;
		}

		final var file = messageExchangeClient.getMessageAttachment(conversationEntity.getMunicipalityId(), messageExchangeNamespace, conversationEntity.getMessageExchangeId(), message.getId(), attachment.getId());
		saveAttachment(errandEntity, file, attachment.getFileName(), ofNullable(attachment.getFileSize()).orElse(0), resolveChannel(message.getCreatedBy()));
	}

	void saveAttachment(final ErrandEntity errandEntity, final ResponseEntity<InputStreamResource> file, final String fileName, final int fileSize, final String channel) {
		if (file.getBody() == null || file.getHeaders().getContentType() == null) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to retrieve attachment from Message Exchange");
		}

		attachmentService.createErrandAttachment(errandEntity, file, fileName, fileSize, channel);
	}
}
