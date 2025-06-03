package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.mergeIntoConversationEntity;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toConversation;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toConversationEntity;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toConversationList;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.toMessageExchangeConversation;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Conversation;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;

@Service
public class ConversationService {

	private static final String NO_CONVERSATION_ID_RETURNED = "ID of conversation was not returned in location header!";
	private static final String NO_CONVERSATION_FOUND = "No conversation with id: '%s' was found!";

	private MessageExchangeClient messageExchangeClient;
	private ConversationRepository conversationRepository;

	public ConversationService(
		final MessageExchangeClient messageExchangeClient,
		final ConversationRepository conversationRepository) {

		this.messageExchangeClient = messageExchangeClient;
		this.conversationRepository = conversationRepository;
	}

	public Conversation createConversation(final String municipalityId, final String namespace, final String errandId, ConversationRequest conversationRequest) {

		// Create conversation in MessageExchange
		final var createResponse = messageExchangeClient.createConversation(municipalityId, namespace, toMessageExchangeConversation(municipalityId, namespace, conversationRequest));

		// Extract MessageExchange conversation ID.
		var location = Optional.ofNullable(createResponse.getHeaders().getLocation())
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, NO_CONVERSATION_ID_RETURNED))
			.getPath();
		final var messageExchangeConversationid = location.substring(location.lastIndexOf('/') + 1);

		// Fetch conversation from MessageExchange.
		final var messageExchangeConversation = fetchConversationFromMessageExchange(municipalityId, namespace, messageExchangeConversationid);

		// Save conversation in DB.
		final var conversationEntity = conversationRepository.save(toConversationEntity(errandId, conversationRequest.getType(), messageExchangeConversation));

		// Return result
		return toConversation(messageExchangeConversation, conversationEntity);
	}

	public Conversation readConversationById(final String municipalityId, final String namespace, final String errandId, final String conversationId) {

		// Fetch conversation from DB.
		final var conversationEntity = conversationRepository.findById(conversationId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NO_CONVERSATION_FOUND.formatted(conversationId)));

		// Fetch conversation from MessageExchange.
		final var messageExchangeConversation = fetchConversationFromMessageExchange(municipalityId, namespace, conversationEntity.getMessageExchangeId());

		// Return result
		return toConversation(messageExchangeConversation, conversationRepository.save(mergeIntoConversationEntity(conversationEntity, messageExchangeConversation)));
	}

	public List<Conversation> readConversations(final String municipalityId, final String namespace, final String errandId) {

		// Return result
		return toConversationList(conversationRepository.findByMunicipalityIdAndNamespaceAndErrandId(municipalityId, namespace, errandId));
	}

	public Conversation updateConversationById(final String municipalityId, final String namespace, final String errandId, final String conversationId, final ConversationRequest conversationRequest) {

		// Fetch conversation from DB.
		final var conversationEntity = conversationRepository.findById(conversationId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NO_CONVERSATION_FOUND.formatted(conversationId)));

		// Update in MessageExchange.
		final var messageExchangeResponse = messageExchangeClient
			.updateConversationById(municipalityId, namespace, conversationId, toMessageExchangeConversation(municipalityId, namespace, conversationRequest)).getBody();

		return toConversation(conversationRepository.save(mergeIntoConversationEntity(conversationEntity, messageExchangeResponse)));
	}

	private generated.se.sundsvall.messageexchange.Conversation fetchConversationFromMessageExchange(
		final String municipalityId,
		final String namespace,
		final String messageExchangeConversationid) {

		return messageExchangeClient.getConversationById(municipalityId, namespace, messageExchangeConversationid).getBody();
	}
}
