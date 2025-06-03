package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Identifier;
import se.sundsvall.supportmanagement.api.model.communication.conversation.KeyValues;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;
import se.sundsvall.supportmanagement.service.mapper.ConversationMapper;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

	private static final Long LATEST_SEQUENCE_NUMBER = 123L;
	private static final String MUNICIPALITY_ID = "MUNICIPALITY_ID";
	private static final String NAMESPACE = "NAMESPACE";
	private static final String CONVERSATION_ID = "CONVERSATION_ID";
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

	@Mock
	private MessageExchangeClient messageExchangeClientMock;

	@Mock
	private ConversationRepository conversationRepositoryMock;

	@Captor
	private ArgumentCaptor<ConversationEntity> conversationEntityCaptor;

	@InjectMocks
	private ConversationService conversationService;

	@Test
	void createConversation() {

		// Arrange
		final var conversationRequest = createConversationRequest();
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID);

		when(conversationRepositoryMock.save(any())).thenReturn(conversationEntity);
		when(messageExchangeClientMock.getConversationById(MUNICIPALITY_ID, NAMESPACE, CONVERSATION_ID)).thenReturn(ResponseEntity.ok(createMessageExchangeConversation()));
		when(messageExchangeClientMock.createConversation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(ResponseEntity.created(URI.create("/bla/bla/" + CONVERSATION_ID)).build());

		// Act
		final var response = conversationService.createConversation(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationRequest);

		// Assert
		assertThat(response).isNotNull();

		verify(conversationRepositoryMock).save(conversationEntityCaptor.capture());
		verify(messageExchangeClientMock).createConversation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any());
		verify(messageExchangeClientMock).getConversationById(MUNICIPALITY_ID, NAMESPACE, CONVERSATION_ID);

		assertThat(conversationEntityCaptor.getValue().getMessageExchangeId()).isEqualTo(MESSAGE_EXCHANGE_ID);
		assertThat(conversationEntityCaptor.getValue().getRelationIds()).isEqualTo(RELATION_VALUES_LIST);
		assertThat(conversationEntityCaptor.getValue().getLatestSyncedSequenceNumber()).isEqualTo(LATEST_SEQUENCE_NUMBER);
	}

	@Test
	void updateConversationById() {

		// Arrange
		final var conversationRequest = createConversationRequest();
		final var messageExchangeConversation = createMessageExchangeConversation();
		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(MESSAGE_EXCHANGE_ID).withId(CONVERSATION_ID);

		when(conversationRepositoryMock.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversationEntity));
		when(conversationRepositoryMock.save(any())).thenReturn(conversationEntity);
		when(messageExchangeClientMock.updateConversationById(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(CONVERSATION_ID), any())).thenReturn(ResponseEntity.ok(messageExchangeConversation));

		// Act
		final var response = conversationService.updateConversationById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, conversationRequest);

		// Assert
		assertThat(response).isNotNull();

		verify(conversationRepositoryMock).save(conversationEntityCaptor.capture());
		verify(messageExchangeClientMock).updateConversationById(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(CONVERSATION_ID), any());

		assertThat(conversationEntityCaptor.getValue().getMessageExchangeId()).isEqualTo(MESSAGE_EXCHANGE_ID);
		assertThat(conversationEntityCaptor.getValue().getRelationIds()).isEqualTo(RELATION_VALUES_LIST);
		assertThat(conversationEntityCaptor.getValue().getLatestSyncedSequenceNumber()).isEqualTo(LATEST_SEQUENCE_NUMBER);
	}

	@Test
	void readConversations() {

		// Arrange
		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(ConversationEntity.create()));

		// Act
		final var response = conversationService.readConversations(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		// Assert
		assertThat(response).isNotNull().hasSize(1);

		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readConversationById() {

		// Arrange
		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(MESSAGE_EXCHANGE_ID).withId(CONVERSATION_ID);

		when(conversationRepositoryMock.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversationEntity));
		when(conversationRepositoryMock.save(any())).thenReturn(conversationEntity);
		when(messageExchangeClientMock.getConversationById(MUNICIPALITY_ID, NAMESPACE, MESSAGE_EXCHANGE_ID)).thenReturn(ResponseEntity.ok(createMessageExchangeConversation()));

		// Act
		final var response = conversationService.readConversationById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getTopic()).isEqualTo(TOPIC);

		verify(conversationRepositoryMock).findById(CONVERSATION_ID);
		verify(conversationRepositoryMock).save(conversationEntityCaptor.capture());
		verify(messageExchangeClientMock).getConversationById(MUNICIPALITY_ID, NAMESPACE, MESSAGE_EXCHANGE_ID);

		assertThat(conversationEntityCaptor.getValue().getMessageExchangeId()).isEqualTo(MESSAGE_EXCHANGE_ID);
		assertThat(conversationEntityCaptor.getValue().getRelationIds()).isEqualTo(RELATION_VALUES_LIST);
		assertThat(conversationEntityCaptor.getValue().getLatestSyncedSequenceNumber()).isEqualTo(LATEST_SEQUENCE_NUMBER);
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
}
