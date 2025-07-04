package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.mergeIntoConversationEntity;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toConversation;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toConversationEntity;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toConversationList;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toMessageExchangeConversation;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toMessagePage;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toMessageRequest;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Conversation;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Message;
import se.sundsvall.supportmanagement.api.model.communication.conversation.MessageRequest;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;
import se.sundsvall.supportmanagement.service.scheduler.messageexchange.MessageExchangeScheduler;

@Service
public class ConversationService {

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ConversationService.class);

	private static final String NO_CONVERSATION_ID_RETURNED = "ID of conversation was not returned in location header!";
	private static final String NO_CONVERSATION_FOUND = "No conversation with ID:'%s', errandId:'%s', municipalityId:'%s' and namespace:'%s' was found!";

	private final MessageExchangeClient messageExchangeClient;
	private final ConversationRepository conversationRepository;
	private final MessageExchangeScheduler messageExchangeScheduler;
	private final CommunicationService communicationService;

	@Value("${integration.messageexchange.namespace:draken}")
	private String messageExchangeNamespace;

	public ConversationService(
		final MessageExchangeClient messageExchangeClient,
		final ConversationRepository conversationRepository,
		final MessageExchangeScheduler messageExchangeScheduler,
		final CommunicationService communicationService) {

		this.messageExchangeClient = messageExchangeClient;
		this.conversationRepository = conversationRepository;
		this.messageExchangeScheduler = messageExchangeScheduler;
		this.communicationService = communicationService;
	}

	public Conversation createConversation(final String municipalityId, final String namespace, final String errandId, final ConversationRequest conversationRequest) {

		// Create conversation in MessageExchange
		final var createResponse = messageExchangeClient.createConversation(municipalityId, messageExchangeNamespace, toMessageExchangeConversation(municipalityId, messageExchangeNamespace, conversationRequest));

		// Extract MessageExchange conversation ID.
		final var location = Optional.ofNullable(createResponse.getHeaders().getLocation())
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, NO_CONVERSATION_ID_RETURNED))
			.getPath();
		final var messageExchangeConversationId = location.substring(location.lastIndexOf('/') + 1);

		// Fetch conversation from MessageExchange.
		final var messageExchangeConversation = fetchConversationFromMessageExchange(municipalityId, messageExchangeConversationId);

		// Save conversation in DB.
		final var conversationEntity = conversationRepository.save(toConversationEntity(municipalityId, namespace, errandId, conversationRequest.getType(), messageExchangeConversation));

		return toConversation(messageExchangeConversation, conversationEntity);
	}

	public Conversation readConversationById(final String municipalityId, final String namespace, final String errandId, final String conversationId) {

		// Fetch conversation from DB.
		final var conversationEntity = getConversationEntity(municipalityId, namespace, errandId, conversationId);

		// Fetch conversation from MessageExchange.
		final var messageExchangeConversation = fetchConversationFromMessageExchange(municipalityId, conversationEntity.getMessageExchangeId());

		// Trigger extra schedule
		messageExchangeScheduler.triggerSyncConversationsAsync();

		return toConversation(messageExchangeConversation, mergeIntoConversationEntity(conversationEntity, messageExchangeConversation));
	}

	public List<Conversation> readConversations(final String municipalityId, final String namespace, final String errandId) {
		return toConversationList(conversationRepository.findByMunicipalityIdAndNamespaceAndErrandId(municipalityId, namespace, errandId));
	}

	public Conversation updateConversationById(final String municipalityId, final String namespace, final String errandId, final String conversationId, final ConversationRequest conversationRequest) {

		// Fetch conversation from DB.
		var conversationEntity = getConversationEntity(municipalityId, namespace, errandId, conversationId);

		// Update in MessageExchange.
		final var messageExchangeResponse = messageExchangeClient
			.updateConversationById(municipalityId, messageExchangeNamespace, conversationEntity.getMessageExchangeId(), toMessageExchangeConversation(municipalityId, messageExchangeNamespace, conversationRequest)).getBody();

		// Save updated conversation in DB.
		conversationEntity = conversationRepository.save(mergeIntoConversationEntity(conversationEntity, messageExchangeResponse));

		return toConversation(messageExchangeResponse, conversationEntity);
	}

	public Page<Message> getMessages(final String municipalityId, final String namespace, final String errandId, final String conversationId, final Pageable pageable) {

		final var conversationEntity = getConversationEntity(municipalityId, namespace, errandId, conversationId);
		final var response = messageExchangeClient.getMessages(municipalityId, messageExchangeNamespace, conversationEntity.getMessageExchangeId(), null, pageable);
		if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to retrieve messages from Message Exchange");
		}
		messageExchangeScheduler.triggerSyncConversationsAsync();
		return toMessagePage(response.getBody());
	}

	public void createMessage(final String municipalityId, final String namespace, final String errandId, final String conversationId, final MessageRequest messageRequest, final List<MultipartFile> attachments) {

		final var conversationEntity = getConversationEntity(municipalityId, namespace, errandId, conversationId);

		messageExchangeClient.createMessage(municipalityId, messageExchangeNamespace, conversationEntity.getMessageExchangeId(), toMessageRequest(messageRequest), attachments);

		Optional.ofNullable(attachments).ifPresent(attachment -> messageExchangeScheduler.triggerSyncConversationsAsync());

		try {
			communicationService.sendMessageNotification(municipalityId, namespace, errandId);
		} catch (final Exception e) {
			LOGGER.error("Failed to send message notification", e);
		}
	}

	private generated.se.sundsvall.messageexchange.Conversation fetchConversationFromMessageExchange(
		final String municipalityId,
		final String messageExchangeConversationId) {

		return messageExchangeClient.getConversationById(municipalityId, messageExchangeNamespace, messageExchangeConversationId).getBody();
	}

	private ConversationEntity getConversationEntity(final String municipalityId, final String namespace, final String errandId, final String conversationId) {
		return conversationRepository.findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, errandId, conversationId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NO_CONVERSATION_FOUND.formatted(conversationId, errandId, municipalityId, namespace)));
	}

	public void getConversationMessageAttachment(
		final String municipalityId, final String namespace, final String errandId,
		final String conversationId, final String messageId, final String attachmentId,
		final HttpServletResponse response) throws IOException {

		final var conversation = getConversationEntity(municipalityId, namespace, errandId, conversationId);
		final var exchangeId = conversation.getMessageExchangeId();

		if (exchangeId == null) {
			throw Problem.valueOf(NOT_FOUND, "Conversation not found in local database");
		}

		final var attachmentResponse = messageExchangeClient.getMessageAttachment(
			municipalityId, messageExchangeNamespace, exchangeId, messageId, attachmentId);

		final var body = attachmentResponse.getBody();
		final var contentType = attachmentResponse.getHeaders().getContentType();

		if (!attachmentResponse.getStatusCode().is2xxSuccessful() || body == null || contentType == null) {
			throw Problem.valueOf(NOT_FOUND, "Attachment not found or invalid in Message Exchange");
		}

		response.setContentType(contentType.toString());

		response.setHeader("Content-Disposition", "attachment; filename=\"" + body.getFilename() + "\"");
		response.setContentLengthLong(attachmentResponse.getHeaders().getContentLength());

		try (final var in = body.getInputStream(); final var out = response.getOutputStream()) {
			in.transferTo(out);
			out.flush();
		}
	}
}
