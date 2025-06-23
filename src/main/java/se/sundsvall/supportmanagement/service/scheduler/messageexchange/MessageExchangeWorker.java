package se.sundsvall.supportmanagement.service.scheduler.messageexchange;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.RELATION_ID_KEY;

import generated.se.sundsvall.messageexchange.Conversation;
import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MessageExchangeSyncRepository;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.ConversationService;

@Component
public class MessageExchangeWorker {

	private final MessageExchangeClient messageExchangeClient;
	private final MessageExchangeSyncRepository messageExchangeSyncRepository;
	private final ConversationRepository conversationRepository;
	private final ConversationService conversationService;
	private final RelationClient relationClient;
	private final ErrandsRepository errandsRepository;

	public MessageExchangeWorker(final MessageExchangeClient messageExchangeClient, final MessageExchangeSyncRepository messageExchangeSyncRepository,
		final ConversationRepository conversationRepository, final ConversationService conversationService,
		final RelationClient relationClient, final ErrandsRepository errandsRepository) {

		this.messageExchangeClient = messageExchangeClient;
		this.messageExchangeSyncRepository = messageExchangeSyncRepository;
		this.conversationRepository = conversationRepository;
		this.conversationService = conversationService;
		this.relationClient = relationClient;
		this.errandsRepository = errandsRepository;
	}

	public List<MessageExchangeSyncEntity> getActiveSyncEntities() {
		return messageExchangeSyncRepository.findByActive(true);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveSyncEntity(MessageExchangeSyncEntity syncEntity) {
		messageExchangeSyncRepository.save(syncEntity);
	}

	public Page<Conversation> getConversations(MessageExchangeSyncEntity syncEntity, Pageable pageable) {
		return messageExchangeClient.getConversations(null, syncEntity.getMunicipalityId(), syncEntity.getNamespace(), "messages.sequenceNumber.id > ".concat(syncEntity.getLatestSyncedSequenceNumber().toString()), pageable).getBody();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Conversation processConversation(Conversation conversation) {
		addNewUnsyncedConversationsToList(conversation, conversationRepository.findByMessageExchangeId(conversation.getId()))
			.forEach(conversationEntity -> conversationService.syncConversation(conversationEntity, conversation));
		return conversation;
	}

	/**
	 * Appends conversation to conversationEntities for conversation where target relation points to support-management
	 * errand and is not created.
	 *
	 * @param  conversation         Conversation that is processed
	 * @param  conversationEntities List of existing conversations
	 * @return                      conversationEntities with possible added conversations
	 */
	private List<ConversationEntity> addNewUnsyncedConversationsToList(Conversation conversation, List<ConversationEntity> conversationEntities) {
		conversation.getExternalReferences().stream()
			.filter(keyValues -> keyValues.getKey() != null && keyValues.getKey().equals(RELATION_ID_KEY))
			.flatMap(keyValues -> keyValues.getValues().stream())
			.filter(isNotPresentInConversationRelations(conversationEntities))
			.map(relationId -> relationClient.getRelation(conversation.getMunicipalityId(), relationId))
			.filter(response -> response.getStatusCode().is2xxSuccessful())
			.map(HttpEntity::getBody)
			.filter(relationTargetConnectedToSupportManagementErrand())
			.map(createConversation(conversation))
			.forEach(conversationEntities::add);

		return conversationEntities;
	}

	private Predicate<String> isNotPresentInConversationRelations(List<ConversationEntity> conversationEntities) {
		// if targetRelationId matches existing conversationEntity it means the conversation is already added
		return relationId -> conversationEntities.stream().noneMatch(conversationEntity -> relationId.equals(conversationEntity.getTargetRelationId()));
	}

	private Predicate<Relation> relationTargetConnectedToSupportManagementErrand() {
		// Only match target, since source will be created through support-management service and already added
		return relation -> resourceIdentifierMatchesErrand(relation.getTarget());
	}

	private boolean resourceIdentifierMatchesErrand(ResourceIdentifier resourceIdentifier) {
		return "support-management".equalsIgnoreCase(resourceIdentifier.getService()) && errandsRepository.findById(resourceIdentifier.getResourceId()).isPresent();
	}

	private Function<Relation, ConversationEntity> createConversation(Conversation conversation) {
		return relation -> {
			var errand = errandsRepository.findById(relation.getTarget().getResourceId())
				.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "Bug in relation filter"));
			return ConversationEntity.create().withErrandId(errand.getId())
				.withMessageExchangeId(conversation.getId())
				.withNamespace(errand.getNamespace())
				.withMunicipalityId(errand.getMunicipalityId())
				.withTargetRelationId(relation.getId())
				.withType("INTERNAL");
		};
	}
}
