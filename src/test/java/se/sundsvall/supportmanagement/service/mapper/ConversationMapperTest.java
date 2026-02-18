package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.messageexchange.Message;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Identifier;
import se.sundsvall.supportmanagement.api.model.communication.conversation.KeyValues;
import se.sundsvall.supportmanagement.api.model.communication.conversation.MessageType;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.RELATION_ID_KEY;

class ConversationMapperTest {

	private static final Long LATEST_SEQUENCE_NUMBER = 123L;
	private static final String MUNICIPALITY_ID = "MUNICIPALITY_ID";
	private static final String NAMESPACE = "NAMESPACE";
	private static final String ID = "ID";
	private static final String ERRAND_ID = "ERRAND_ID";
	private static final String MESSAGE_EXCHANGE_ID = "MESSAGE_EXCHANGE_ID";
	private static final String TOPIC = "TOPIC";
	private static final String EXTERNAL_REFERENCE_KEY = "EXTERNAL_REFERENCE_KEY";
	private static final String METADATA_KEY = "METADATA_KEY";
	private static final String IDENTIFIER_TYPE = "IDENTIFIER_TYPE";
	private static final String IDENTIFIER_VALUE = "IDENTIFIER_VALUE";
	private static final ConversationType CONVERSATION_TYPE = ConversationType.EXTERNAL;
	private static final List<String> VALUES_LIST = List.of("value1", "value2");
	private static final List<String> RELATION_VALUES_LIST = List.of("rel1", "rel2");

	private static final String ATTACHMENT_ID = "ATTACHMENT_ID";
	private static final String ATTACHMENT_FILENAME = "filename.txt";
	private static final String ATTACHMENT_MIMETYPE = "text/plain";
	private static final Integer ATTACHMENT_FILESIZE = 666;
	private static final OffsetDateTime ATTACHMENT_CREATED = now();

	private static final String MESSAGE_ID = "MESSAGE_ID";
	private static final String MESSAGE_IN_REPLY_TO_ID = "MESSAGE_IN_REPLY_TO_ID";
	private static final String MESSAGE_CONTENT = "content";
	private static final Long MESSAGE_SEQUENCE_NUMBER = 10L;
	private static final OffsetDateTime MESSAGE_CREATED = now();
	private static final OffsetDateTime MESSAGE_READ_AT = now();
	private static final MessageType MESSAGE_TYPE = MessageType.USER_CREATED;

	@Test
	void toConversationEntity() {

		// Act
		final var result = ConversationMapper.toConversationEntity(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_TYPE, createMessageExchangeConversation());

		// Assert
		assertThat(result.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(result.getType()).isEqualTo(CONVERSATION_TYPE.name());
		assertThat(result.getId()).isNull();
		assertThat(result.getLatestSyncedSequenceNumber()).isEqualTo(LATEST_SEQUENCE_NUMBER);
		assertThat(result.getMessageExchangeId()).isEqualTo(MESSAGE_EXCHANGE_ID);
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(result.getRelationIds()).isEqualTo(RELATION_VALUES_LIST);
		assertThat(result.getTopic()).isEqualTo(TOPIC);
	}

	@Test
	void toConversationEntityWhenMessageExchangeConversationIsNull() {

		// Act
		final var result = ConversationMapper.toConversationEntity(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_TYPE, null);

		// Assert
		assertThat(result).isNull();
	}

	@Test
	void toMessageExchangeConversation() {

		// Act
		final var result = ConversationMapper.toMessageExchangeConversation(MUNICIPALITY_ID, NAMESPACE, createConversationRequest());

		// Assert
		assertThat(result.getExternalReferences()).isEqualTo(List.of(new generated.se.sundsvall.messageexchange.KeyValues().key(ConversationMapper.RELATION_ID_KEY).values(RELATION_VALUES_LIST)));
		assertThat(result.getId()).isNull();
		assertThat(result.getLatestSequenceNumber()).isNull();
		assertThat(result.getMetadata()).isEqualTo(List.of(new generated.se.sundsvall.messageexchange.KeyValues().key(METADATA_KEY).values(VALUES_LIST)));
		assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getParticipants()).isEqualTo((List.of(new generated.se.sundsvall.messageexchange.Identifier().type(IDENTIFIER_TYPE).value(IDENTIFIER_VALUE))));
		assertThat(result.getTopic()).isEqualTo(TOPIC);
	}

	@Test
	void toMessageExchangeConversationWhenConversationIsNull() {

		// Act
		final var result = ConversationMapper.toMessageExchangeConversation(MUNICIPALITY_ID, NAMESPACE, null);

		// Assert
		assertThat(result).isNull();
	}

	@Test
	void toConversation() {

		// Act
		final var result = ConversationMapper.toConversation(createMessageExchangeConversation(), createConversationEntity());

		// Assert
		assertThat(result.getId()).isEqualTo(ID);
		assertThat(result.getMetadata()).isEqualTo(List.of(KeyValues.create().withKey(METADATA_KEY).withValues(VALUES_LIST)));
		assertThat(result.getParticipants()).isEqualTo((List.of(Identifier.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE))));
		assertThat(result.getTopic()).isEqualTo(TOPIC);
		assertThat(result.getType()).isEqualTo(CONVERSATION_TYPE);
	}

	@Test
	void toConversationWhenMessageExchangeConversationIsNull() {

		// Act
		final var result = ConversationMapper.toConversation(null, createConversationEntity());

		// Assert
		assertThat(result).isNull();
	}

	@Test
	void toConversationFromConversationEntity() {

		// Act
		final var result = ConversationMapper.toConversation(createConversationEntity());

		// Assert
		assertThat(result.getMetadata()).isNull();
		assertThat(result.getParticipants()).isNull();
		assertThat(result.getRelationIds()).isEqualTo(RELATION_VALUES_LIST);
		assertThat(result.getId()).isEqualTo(ID);
		assertThat(result.getTopic()).isEqualTo(TOPIC);
		assertThat(result.getType()).isEqualTo(CONVERSATION_TYPE);
	}

	@Test
	void toConversationFromConversationEntityWhenConversationEntityIsNull() {

		// Act
		final var result = ConversationMapper.toConversation(null);

		// Assert
		assertThat(result).isNull();
	}

	@Test
	void toConversationListFromConversationEntityList() {

		// Act
		final var result = ConversationMapper.toConversationList(List.of(createConversationEntity()));

		// Assert
		assertThat(result).hasSize(1);

		assertThat(result.getFirst().getMetadata()).isNull();
		assertThat(result.getFirst().getParticipants()).isNull();
		assertThat(result.getFirst().getRelationIds()).isEqualTo(RELATION_VALUES_LIST);
		assertThat(result.getFirst().getId()).isEqualTo(ID);
		assertThat(result.getFirst().getTopic()).isEqualTo(TOPIC);
		assertThat(result.getFirst().getType()).isEqualTo(CONVERSATION_TYPE);
	}

	@Test
	void mergeIntoConversationEntity() {

		// Arrange
		final var newLatestSequenceNumber = 666L;
		final var newTopic = "NEW-TOPIC";
		final var newRelationIds = List.of("newRel-1", "newRel-2");
		final var conversationEntity = createConversationEntity();
		final var messageExchangeConversation = createMessageExchangeConversation();

		messageExchangeConversation
			.latestSequenceNumber(newLatestSequenceNumber)
			.topic(newTopic)
			.externalReferences(List.of(new generated.se.sundsvall.messageexchange.KeyValues().key(RELATION_ID_KEY).values(newRelationIds)));

		// Act
		final var result = ConversationMapper.mergeIntoConversationEntity(conversationEntity, messageExchangeConversation);

		// Assert
		assertThat(result.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(result.getType()).isEqualTo(CONVERSATION_TYPE.name());
		assertThat(result.getId()).isEqualTo(ID);
		assertThat(result.getLatestSyncedSequenceNumber()).isEqualTo(newLatestSequenceNumber);
		assertThat(result.getMessageExchangeId()).isEqualTo(MESSAGE_EXCHANGE_ID);
		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(result.getRelationIds()).isEqualTo(newRelationIds);
		assertThat(result.getTopic()).isEqualTo(newTopic);
	}

	@Test
	void toMessagePage() {

		// Arrange
		final var message = createMessageExchangeMessage();
		final var page = new org.springframework.data.domain.PageImpl<>(List.of(message));

		// Act
		final var result = ConversationMapper.toMessagePage(page);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(1);

		final var extractedMessage = result.getContent().getFirst();

		assertThat(extractedMessage.getId()).isEqualTo(MESSAGE_ID);
		assertThat(extractedMessage.getContent()).isEqualTo(MESSAGE_CONTENT);
		assertThat(extractedMessage.getInReplyToMessageId()).isEqualTo(MESSAGE_IN_REPLY_TO_ID);
		assertThat(extractedMessage.getCreated()).isEqualTo(MESSAGE_CREATED);
		assertThat(extractedMessage.getCreatedBy()).isNotNull();
		assertThat(extractedMessage.getCreatedBy().getType()).isEqualTo(IDENTIFIER_TYPE);
		assertThat(extractedMessage.getCreatedBy().getValue()).isEqualTo(IDENTIFIER_VALUE);
		assertThat(extractedMessage.getReadBy()).isNotNull();
		assertThat(extractedMessage.getReadBy()).hasSize(1);
		assertThat(extractedMessage.getReadBy().getFirst().getIdentifier().getType()).isEqualTo(IDENTIFIER_TYPE);
		assertThat(extractedMessage.getReadBy().getFirst().getIdentifier().getValue()).isEqualTo(IDENTIFIER_VALUE);
		assertThat(extractedMessage.getReadBy().getFirst().getReadAt()).isEqualTo(MESSAGE_READ_AT);
		assertThat(extractedMessage.getAttachments()).isNotNull();
		assertThat(extractedMessage.getAttachments()).hasSize(1);
		assertThat(extractedMessage.getAttachments().getFirst().getCreated()).isEqualTo(ATTACHMENT_CREATED);
		assertThat(extractedMessage.getAttachments().getFirst().getFileName()).isEqualTo(ATTACHMENT_FILENAME);
		assertThat(extractedMessage.getAttachments().getFirst().getFileSize()).isEqualTo(ATTACHMENT_FILESIZE);
		assertThat(extractedMessage.getAttachments().getFirst().getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(extractedMessage.getAttachments().getFirst().getMimeType()).isEqualTo(ATTACHMENT_MIMETYPE);
		assertThat(extractedMessage.getAttachments().getFirst().getCreated()).isEqualTo(ATTACHMENT_CREATED);
		assertThat(extractedMessage.getType()).isEqualTo(MESSAGE_TYPE);
	}

	@Test
	void toReadBy() {

		// Arrange
		final var type = "adAccount";
		final var value = "joe01doe";
		final var readAt = OffsetDateTime.now().minusDays(7);
		final var identifier = new generated.se.sundsvall.messageexchange.Identifier()
			.type(type)
			.value(value);
		final var readByList = List.of(new generated.se.sundsvall.messageexchange.ReadBy()
			.identifier(identifier)
			.readAt(readAt));

		// Act
		final var result = ConversationMapper.toReadBy(readByList);

		// Assert
		assertThat(result).isNotNull().hasSize(1);
		assertThat(result.getFirst().getIdentifier().getType()).isEqualTo(type);
		assertThat(result.getFirst().getIdentifier().getValue()).isEqualTo(value);
		assertThat(result.getFirst().getReadAt()).isCloseTo(readAt, within(5, SECONDS));
	}

	private generated.se.sundsvall.messageexchange.Message createMessageExchangeMessage() {
		return new generated.se.sundsvall.messageexchange.Message()
			.content(MESSAGE_CONTENT)
			.created(MESSAGE_CREATED)
			.createdBy(new generated.se.sundsvall.messageexchange.Identifier().type(IDENTIFIER_TYPE).value(IDENTIFIER_VALUE))
			.id(MESSAGE_ID)
			.inReplyToMessageId(MESSAGE_IN_REPLY_TO_ID)
			.readBy(List.of(new generated.se.sundsvall.messageexchange.ReadBy()
				.identifier(new generated.se.sundsvall.messageexchange.Identifier().type(IDENTIFIER_TYPE).value(IDENTIFIER_VALUE))
				.readAt(MESSAGE_READ_AT)))
			.sequenceNumber(MESSAGE_SEQUENCE_NUMBER)
			.attachments(List.of(
				new generated.se.sundsvall.messageexchange.Attachment()
					.fileName(ATTACHMENT_FILENAME)
					.fileSize(ATTACHMENT_FILESIZE)
					.id(ATTACHMENT_ID)
					.mimeType(ATTACHMENT_MIMETYPE)
					.created(ATTACHMENT_CREATED)))
			.type(Message.TypeEnum.valueOf(MESSAGE_TYPE.name()));
	}

	private generated.se.sundsvall.messageexchange.Conversation createMessageExchangeConversation() {
		return new generated.se.sundsvall.messageexchange.Conversation()
			.externalReferences(List.of(
				new generated.se.sundsvall.messageexchange.KeyValues().key(EXTERNAL_REFERENCE_KEY).values(VALUES_LIST),
				new generated.se.sundsvall.messageexchange.KeyValues().key(ConversationMapper.RELATION_ID_KEY).values(RELATION_VALUES_LIST)))
			.id(MESSAGE_EXCHANGE_ID)
			.latestSequenceNumber(LATEST_SEQUENCE_NUMBER)
			.metadata(List.of(new generated.se.sundsvall.messageexchange.KeyValues().key(METADATA_KEY).values(VALUES_LIST)))
			.municipalityId(MUNICIPALITY_ID)
			.namespace(NAMESPACE)
			.participants(List.of(new generated.se.sundsvall.messageexchange.Identifier().type(IDENTIFIER_TYPE).value(IDENTIFIER_VALUE)))
			.topic(TOPIC);
	}

	private ConversationRequest createConversationRequest() {
		return ConversationRequest.create()
			.withMetadata(List.of(
				KeyValues.create().withKey(METADATA_KEY).withValues(VALUES_LIST)))
			.withParticipants(List.of(Identifier.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE)))
			.withRelationIds(RELATION_VALUES_LIST)
			.withTopic(TOPIC)
			.withType(CONVERSATION_TYPE);
	}

	private ConversationEntity createConversationEntity() {
		return ConversationEntity.create()
			.withErrandId(ERRAND_ID)
			.withId(ID)
			.withLatestSyncedSequenceNumber(LATEST_SEQUENCE_NUMBER)
			.withMessageExchangeId(MESSAGE_EXCHANGE_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withRelationIds(RELATION_VALUES_LIST)
			.withTopic(TOPIC)
			.withType(CONVERSATION_TYPE.name());
	}
}
