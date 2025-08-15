package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Strings;
import org.springframework.data.domain.Page;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Attachment;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Conversation;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Identifier;
import se.sundsvall.supportmanagement.api.model.communication.conversation.KeyValues;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Message;
import se.sundsvall.supportmanagement.api.model.communication.conversation.MessageRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.MessageType;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ReadBy;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;

public final class ConversationMapper {

	public static final String RELATION_ID_KEY = "relationIds";

	private ConversationMapper() {}

	public static generated.se.sundsvall.messageexchange.Conversation toMessageExchangeConversation(String municipalityId, String messageExchangeNamespace, ConversationRequest conversationRequest) {
		return ofNullable(conversationRequest)
			.map(c -> new generated.se.sundsvall.messageexchange.Conversation()
				.externalReferences(ofNullable(c.getRelationIds())
					.map(relationIds -> toMessageExchangeKeyValuesList(List.of(KeyValues.create().withKey(RELATION_ID_KEY).withValues(relationIds))))
					.orElse(null))
				.metadata(toMessageExchangeKeyValuesList(c.getMetadata()))
				.municipalityId(municipalityId)
				.namespace(messageExchangeNamespace)
				.participants(toMessageExchangeIdentifierList(c.getParticipants()))
				.topic(c.getTopic()))
			.orElse(null);
	}

	public static ConversationEntity toConversationEntity(String municipalityId, String namespace, String errandId, ConversationType type, generated.se.sundsvall.messageexchange.Conversation conversation) {
		return ofNullable(conversation)
			.map(c -> ConversationEntity.create()
				.withErrandId(errandId)
				.withLatestSyncedSequenceNumber(c.getLatestSequenceNumber())
				.withMessageExchangeId(c.getId())
				.withMunicipalityId(municipalityId)
				.withNamespace(namespace)
				.withRelationIds(toStringList(toKeyValuesList(c.getExternalReferences()), RELATION_ID_KEY))
				.withTopic(c.getTopic())
				.withType(ofNullable(type).map(ConversationType::name).orElse(null)))
			.orElse(null);
	}

	public static Conversation toConversation(generated.se.sundsvall.messageexchange.Conversation conversation, ConversationEntity conversationEntity) {
		return ofNullable(conversation)
			.map(c -> Conversation.create()
				.withId(conversationEntity.getId())
				.withMetadata(toKeyValuesList(c.getMetadata()))
				.withParticipants(toIdentifierList(c.getParticipants()))
				.withRelationIds(ofNullable(conversationEntity.getRelationIds()).map(ArrayList::new).orElse(null))
				.withTopic(conversation.getTopic())
				.withType(ofNullable(conversationEntity.getType()).map(ConversationType::valueOf).orElse(null)))
			.orElse(null);
	}

	public static List<Conversation> toConversationList(List<ConversationEntity> conversationEntityList) {
		return ofNullable(conversationEntityList).orElse(emptyList()).stream()
			.map(ConversationMapper::toConversation)
			.toList();
	}

	public static Conversation toConversation(ConversationEntity conversationEntity) {
		return ofNullable(conversationEntity)
			.map(c -> Conversation.create()
				.withId(c.getId())
				.withRelationIds(ofNullable(c.getRelationIds()).map(ArrayList::new).orElse(null))
				.withTopic(c.getTopic())
				.withType(ofNullable(c.getType()).map(ConversationType::valueOf).orElse(null)))
			.orElse(null);
	}

	public static ConversationEntity mergeIntoConversationEntity(ConversationEntity conversationEntity, generated.se.sundsvall.messageexchange.Conversation conversation) {
		ofNullable(conversation)
			.ifPresent(c -> conversationEntity
				.withLatestSyncedSequenceNumber(c.getLatestSequenceNumber())
				.withTopic(c.getTopic())
				.withRelationIds(toStringList(toKeyValuesList(c.getExternalReferences()), RELATION_ID_KEY)));

		return conversationEntity;
	}

	public static Page<Message> toMessagePage(final Page<generated.se.sundsvall.messageexchange.Message> message) {
		return message.map(m -> Message.create()
			.withId(m.getId())
			.withInReplyToMessageId(m.getInReplyToMessageId())
			.withCreated(m.getCreated())
			.withCreatedBy(ofNullable(m.getCreatedBy()).map(ConversationMapper::toIdentifier).orElse(null))
			.withContent(m.getContent())
			.withReadBy(toReadBy(m.getReadBy()))
			.withAttachments(toAttachments(m.getAttachments()))
			.withType(MessageType.valueOf(m.getType().toString())));
	}

	public static List<ReadBy> toReadBy(final List<generated.se.sundsvall.messageexchange.ReadBy> readByList) {
		return ofNullable(readByList).orElse(emptyList()).stream()
			.map(readBy -> ReadBy.create()
				.withIdentifier(toIdentifier(readBy.getIdentifier()))
				.withReadAt(readBy.getReadAt()))
			.toList();
	}

	private static List<Attachment> toAttachments(final List<generated.se.sundsvall.messageexchange.Attachment> attachmentList) {
		return ofNullable(attachmentList).orElse(emptyList()).stream()
			.map(attachment -> Attachment.create()
				.withCreated(attachment.getCreated())
				.withId(attachment.getId())
				.withFileName(attachment.getFileName())
				.withMimeType(attachment.getMimeType())
				.withFileSize(ofNullable(attachment.getFileSize()).orElse(0)))
			.toList();
	}

	public static generated.se.sundsvall.messageexchange.Message toMessageRequest(final MessageRequest messageRequest) {
		return new generated.se.sundsvall.messageexchange.Message()
			.inReplyToMessageId(messageRequest.getInReplyToMessageId())
			.content(messageRequest.getContent());
	}

	private static List<KeyValues> toKeyValuesList(List<generated.se.sundsvall.messageexchange.KeyValues> keyValueList) {
		return ofNullable(keyValueList).orElse(emptyList()).stream()
			.map(kv -> KeyValues.create()
				.withKey(kv.getKey())
				.withValues(new ArrayList<>(kv.getValues())))
			.toList();
	}

	private static List<generated.se.sundsvall.messageexchange.KeyValues> toMessageExchangeKeyValuesList(List<KeyValues> keyValueList) {
		return ofNullable(keyValueList).orElse(emptyList()).stream()
			.map(kv -> new generated.se.sundsvall.messageexchange.KeyValues()
				.key(kv.getKey())
				.values(new ArrayList<>(kv.getValues())))
			.toList();
	}

	private static List<generated.se.sundsvall.messageexchange.Identifier> toMessageExchangeIdentifierList(List<Identifier> identifierList) {
		return ofNullable(identifierList).orElse(emptyList()).stream()
			.map(ConversationMapper::toMessageExchangeIdentifier)
			.toList();
	}

	private static generated.se.sundsvall.messageexchange.Identifier toMessageExchangeIdentifier(Identifier identifier) {
		return ofNullable(identifier)
			.map(i -> new generated.se.sundsvall.messageexchange.Identifier()
				.type(i.getType())
				.value(i.getValue()))
			.orElse(null);
	}

	private static List<Identifier> toIdentifierList(List<generated.se.sundsvall.messageexchange.Identifier> identifierList) {
		return ofNullable(identifierList).orElse(emptyList()).stream()
			.map(ConversationMapper::toIdentifier)
			.toList();
	}

	private static Identifier toIdentifier(generated.se.sundsvall.messageexchange.Identifier identifier) {
		return ofNullable(identifier)
			.map(i -> Identifier.create()
				.withType(i.getType())
				.withValue(i.getValue()))
			.orElse(null);
	}

	private static List<String> toStringList(List<KeyValues> keyValueList, String key) {
		return new ArrayList<>(ofNullable(keyValueList).orElse(emptyList()).stream()
			.filter(keyValues -> Strings.CI.equals(keyValues.getKey(), key))
			.flatMap(keyValues -> keyValues.getValues().stream())
			.toList());
	}
}
