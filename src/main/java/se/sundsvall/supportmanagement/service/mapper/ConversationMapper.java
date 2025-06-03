package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Conversation;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Identifier;
import se.sundsvall.supportmanagement.api.model.communication.conversation.KeyValues;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;

public final class ConversationMapper {

	public static final String RELATION_ID_KEY = "relationIds";

	private ConversationMapper() {}

	public static generated.se.sundsvall.messageexchange.Conversation toMessageExchangeConversation(String municipalityId, String namespace, ConversationRequest conversationRequest) {
		return Optional.ofNullable(conversationRequest)
			.map(c -> new generated.se.sundsvall.messageexchange.Conversation()
				.externalReferences(toMessageExchangeKeyValuesList(List.of(KeyValues.create().withKey(RELATION_ID_KEY).withValues(c.getRelationIds()))))
				.metadata(toMessageExchangeKeyValuesList(c.getMetadata()))
				.municipalityId(municipalityId)
				.namespace(namespace)
				.participants(toMessageExchangeIdentifierList(c.getParticipants()))
				.topic(c.getTopic()))
			.orElse(null);
	}

	public static ConversationEntity toConversationEntity(String errandId, ConversationType type, generated.se.sundsvall.messageexchange.Conversation conversation) {
		return Optional.ofNullable(conversation)
			.map(c -> ConversationEntity.create()
				.withErrandId(errandId)
				.withLatestSyncedSequenceNumber(c.getLatestSequenceNumber())
				.withMessageExchangeId(c.getId())
				.withMunicipalityId(c.getMunicipalityId())
				.withNamespace(c.getNamespace())
				.withRelationIds(toStringList(toKeyValuesList(c.getExternalReferences()), RELATION_ID_KEY))
				.withTopic(c.getTopic())
				.withType(Optional.ofNullable(type).map(ConversationType::name).orElse(null)))
			.orElse(null);
	}

	public static Conversation toConversation(generated.se.sundsvall.messageexchange.Conversation conversation, ConversationEntity conversationEntity) {
		return Optional.ofNullable(conversation)
			.map(c -> Conversation.create()
				.withId(conversationEntity.getId())
				.withMetadata(toKeyValuesList(c.getMetadata()))
				.withParticipants(toIdentifierList(c.getParticipants()))
				.withRelationIds(Optional.ofNullable(conversationEntity.getRelationIds()).map(ArrayList::new).orElse(null))
				.withTopic(conversation.getTopic())
				.withType(Optional.ofNullable(conversationEntity.getType()).map(ConversationType::valueOf).orElse(null)))
			.orElse(null);
	}

	public static List<Conversation> toConversationList(List<ConversationEntity> conversationEntityList) {
		return Optional.ofNullable(conversationEntityList).orElse(emptyList()).stream()
			.map(ConversationMapper::toConversation)
			.toList();
	}

	public static Conversation toConversation(ConversationEntity conversationEntity) {
		return Optional.ofNullable(conversationEntity)
			.map(c -> Conversation.create()
				.withId(c.getId())
				.withRelationIds(Optional.ofNullable(c.getRelationIds()).map(ArrayList::new).orElse(null))
				.withTopic(c.getTopic())
				.withType(Optional.ofNullable(c.getType()).map(ConversationType::valueOf).orElse(null)))
			.orElse(null);
	}

	public static ConversationEntity mergeIntoConversationEntity(ConversationEntity conversationEntity, generated.se.sundsvall.messageexchange.Conversation conversation) {
		Optional.ofNullable(conversation)
			.ifPresent(c -> conversationEntity
				.withLatestSyncedSequenceNumber(c.getLatestSequenceNumber())
				.withTopic(c.getTopic())
				.withRelationIds(toStringList(toKeyValuesList(c.getExternalReferences()), RELATION_ID_KEY)));

		return conversationEntity;
	}

	private static List<KeyValues> toKeyValuesList(List<generated.se.sundsvall.messageexchange.KeyValues> keyValueList) {
		return Optional.ofNullable(keyValueList).orElse(Collections.emptyList()).stream()
			.map(kv -> KeyValues.create()
				.withKey(kv.getKey())
				.withValues(new ArrayList<>(kv.getValues())))
			.toList();
	}

	private static List<generated.se.sundsvall.messageexchange.KeyValues> toMessageExchangeKeyValuesList(List<KeyValues> keyValueList) {
		return Optional.ofNullable(keyValueList).orElse(Collections.emptyList()).stream()
			.map(kv -> new generated.se.sundsvall.messageexchange.KeyValues()
				.key(kv.getKey())
				.values(new ArrayList<>(kv.getValues())))
			.toList();
	}

	private static List<generated.se.sundsvall.messageexchange.Identifier> toMessageExchangeIdentifierList(List<Identifier> identifierList) {
		return Optional.ofNullable(identifierList).orElse(Collections.emptyList()).stream()
			.map(ConversationMapper::toMessageExchangeIdentifier)
			.toList();
	}

	private static generated.se.sundsvall.messageexchange.Identifier toMessageExchangeIdentifier(Identifier identifier) {
		return Optional.ofNullable(identifier)
			.map(i -> new generated.se.sundsvall.messageexchange.Identifier()
				.type(i.getType())
				.value(i.getValue()))
			.orElse(null);
	}

	private static List<Identifier> toIdentifierList(List<generated.se.sundsvall.messageexchange.Identifier> identifierList) {
		return Optional.ofNullable(identifierList).orElse(Collections.emptyList()).stream()
			.map(ConversationMapper::todentifier)
			.toList();
	}

	private static Identifier todentifier(generated.se.sundsvall.messageexchange.Identifier identifier) {
		return Optional.ofNullable(identifier)
			.map(i -> Identifier.create()
				.withType(i.getType())
				.withValue(i.getValue()))
			.orElse(null);
	}

	private static List<String> toStringList(List<KeyValues> keyValueList, String key) {
		return new ArrayList<>(Optional.ofNullable(keyValueList).orElse(Collections.emptyList()).stream()
			.filter(keyValues -> equalsIgnoreCase(keyValues.getKey(), key))
			.flatMap(keyValues -> keyValues.getValues().stream())
			.toList());
	}
}
