package se.sundsvall.supportmanagement.service;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Identifier;
import se.sundsvall.supportmanagement.api.model.communication.conversation.KeyValues;
import se.sundsvall.supportmanagement.api.model.communication.conversation.MessageRequest;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
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
	private static final String MESSAGE_EXCHANGE_NAMESPACE = "draken";
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

	@Mock
	private ErrandsRepository errandRepositoryMock;

	@Mock
	private ErrandAttachmentService errandAttachmentServiceMock;

	@Mock
	private CommunicationService communicationServiceMock;

	@Captor
	private ArgumentCaptor<ConversationEntity> conversationEntityCaptor;

	@InjectMocks
	private ConversationService conversationService;

	@BeforeEach
	void beforeEach() {
		// Set Spring managed value.
		setField(conversationService, "messageExchangeNamespace", "draken");
	}

	@Test
	void createConversation() {

		// Arrange
		final var conversationRequest = createConversationRequest();
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID);

		when(conversationRepositoryMock.save(any())).thenReturn(conversationEntity);
		when(messageExchangeClientMock.getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, CONVERSATION_ID)).thenReturn(ResponseEntity.ok(createMessageExchangeConversation()));
		when(messageExchangeClientMock.createConversation(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), any())).thenReturn(ResponseEntity.created(URI.create("/bla/bla/" + CONVERSATION_ID)).build());

		// Act
		final var response = conversationService.createConversation(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationRequest);

		// Assert
		assertThat(response).isNotNull();

		verify(conversationRepositoryMock).save(conversationEntityCaptor.capture());
		verify(messageExchangeClientMock).createConversation(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), any());
		verify(messageExchangeClientMock).getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, CONVERSATION_ID);

		assertThat(conversationEntityCaptor.getValue().getMessageExchangeId()).isEqualTo(MESSAGE_EXCHANGE_ID);
		assertThat(conversationEntityCaptor.getValue().getRelationIds()).isEqualTo(RELATION_VALUES_LIST);
		assertThat(conversationEntityCaptor.getValue().getLatestSyncedSequenceNumber()).isEqualTo(LATEST_SEQUENCE_NUMBER);
	}

	@Test
	void createConversationNoConversationIdReturnedFromMessageExchange() {

		// Arrange
		final var conversationRequest = createConversationRequest();

		when(messageExchangeClientMock.createConversation(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), any())).thenReturn(ResponseEntity.notFound().build());

		// Act
		assertThatThrownBy(() -> conversationService.createConversation(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationRequest))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Internal Server Error: ID of conversation was not returned in location header!");

		// Assert
		verify(messageExchangeClientMock).createConversation(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), any());
		verify(conversationRepositoryMock, never()).save(any());
		verify(messageExchangeClientMock, never()).getConversationById(any(), any(), any());
	}

	@Test
	void updateConversationById() {

		// Arrange
		final var conversationRequest = createConversationRequest();
		final var messageExchangeConversation = createMessageExchangeConversation();
		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(MESSAGE_EXCHANGE_ID).withId(CONVERSATION_ID);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID)).thenReturn(Optional.of(conversationEntity));
		when(conversationRepositoryMock.save(any())).thenReturn(conversationEntity);
		when(messageExchangeClientMock.updateConversationById(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any())).thenReturn(ResponseEntity.ok(messageExchangeConversation));

		// Act
		final var response = conversationService.updateConversationById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, conversationRequest);

		// Assert
		assertThat(response).isNotNull();

		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(conversationRepositoryMock).save(conversationEntityCaptor.capture());
		verify(messageExchangeClientMock).updateConversationById(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any());

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

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID)).thenReturn(Optional.of(conversationEntity));
		when(conversationRepositoryMock.save(any())).thenReturn(conversationEntity);
		when(messageExchangeClientMock.getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID)).thenReturn(ResponseEntity.ok(createMessageExchangeConversation()));

		// Act
		final var response = conversationService.readConversationById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getTopic()).isEqualTo(TOPIC);

		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(conversationRepositoryMock).save(conversationEntityCaptor.capture());
		verify(messageExchangeClientMock).getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID);

		assertThat(conversationEntityCaptor.getValue().getMessageExchangeId()).isEqualTo(MESSAGE_EXCHANGE_ID);
		assertThat(conversationEntityCaptor.getValue().getRelationIds()).isEqualTo(RELATION_VALUES_LIST);
		assertThat(conversationEntityCaptor.getValue().getLatestSyncedSequenceNumber()).isEqualTo(LATEST_SEQUENCE_NUMBER);
	}

	@Test
	void readConversationByIdConversationNotFoundInDB() {

		// Arrange
		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID)).thenReturn(empty());

		// Act
		assertThatThrownBy(() -> conversationService.readConversationById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Not Found: No conversation with ID:'CONVERSATION_ID', errandId:'ERRAND_ID', municipalityId:'MUNICIPALITY_ID' and namespace:'NAMESPACE' was found!");

		// Assert
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(conversationRepositoryMock, never()).save(any());
		verify(messageExchangeClientMock, never()).getConversationById(any(), any(), any());
	}

	@Test
	void createMessage() {

		// Arrange
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID).withMessageExchangeId(MESSAGE_EXCHANGE_ID).withErrandId(ERRAND_ID);
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);
		final var messageRequest = MessageRequest.create();

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID))
			.thenReturn(Optional.ofNullable(conversationEntity));

		when(errandRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.ofNullable(errandEntity));

		when(messageExchangeClientMock.createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), eq(null)))
			.thenReturn(ResponseEntity.ok().build());

		// Act
		conversationService.createMessage(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, messageRequest, null);

		// Assert
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(errandRepositoryMock).findById(ERRAND_ID);
		verify(messageExchangeClientMock).createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), eq(null));
		verify(errandAttachmentServiceMock, never()).createErrandAttachment(any(), any(), any(), any());
		verify(communicationServiceMock).sendMessageNotification(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, errandAttachmentServiceMock, communicationServiceMock);
	}

	@Test
	void createMessageErrandNotFoundInDB() {

		// Arrange
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID).withMessageExchangeId(MESSAGE_EXCHANGE_ID).withErrandId(ERRAND_ID);
		final var messageRequest = MessageRequest.create();

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID))
			.thenReturn(Optional.ofNullable(conversationEntity));

		when(errandRepositoryMock.findById(ERRAND_ID)).thenReturn(empty());

		// Act
		assertThatThrownBy(() -> conversationService.createMessage(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, messageRequest, null))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Not Found: No errand with ID: 'ERRAND_ID' was found!");

		// Assert
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(errandRepositoryMock).findById(ERRAND_ID);
		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, errandAttachmentServiceMock);
	}

	@Test
	void createMessageWithAttachment() {

		// Arrange
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID).withMessageExchangeId(MESSAGE_EXCHANGE_ID).withErrandId(ERRAND_ID);
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);
		final var messageRequest = MessageRequest.create();
		final var multipartFile = new MockMultipartFile("attachments", "attachment.txt".getBytes());

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID))
			.thenReturn(Optional.ofNullable(conversationEntity));

		when(errandRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.ofNullable(errandEntity));

		when(messageExchangeClientMock.createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), any()))
			.thenReturn(ResponseEntity.ok().build());

		// Act
		conversationService.createMessage(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, messageRequest, List.of(multipartFile));

		// Assert
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(errandRepositoryMock).findById(ERRAND_ID);
		verify(messageExchangeClientMock).createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), any());
		verify(errandAttachmentServiceMock).createErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, multipartFile);

		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, errandAttachmentServiceMock);
	}

	@Test
	void getMessages() {

		// Arrange
		final var pageable = PageRequest.of(0, 10);
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID).withMessageExchangeId(MESSAGE_EXCHANGE_ID).withErrandId(ERRAND_ID);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID))
			.thenReturn(Optional.ofNullable(conversationEntity));

		when(messageExchangeClientMock.getMessages(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any()))
			.thenReturn(ResponseEntity.ok(new PageImpl<>(List.of(new generated.se.sundsvall.messageexchange.Message()))));

		// Act
		final var result = conversationService.getMessages(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, pageable);

		// Assert
		assertThat(result).isNotNull().hasSize(1);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(messageExchangeClientMock).getMessages(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID, pageable);
		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock);
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

	@Test
	void getConversationMessageAttachment() throws IOException {
		// Arrange
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var conversationId = "convId";
		final var messageId = "msgId";
		final var attachmentId = "attId";
		final var messageExchangeId = "exchangeId";
		final var filename = "file.txt";
		final var contentType = org.springframework.http.MediaType.TEXT_PLAIN;
		final var content = "test content".getBytes();

		final var conversationEntity = ConversationEntity.create()
			.withMessageExchangeId(messageExchangeId);

		final var inputStream = new java.io.ByteArrayInputStream(content);
		final var inputStreamResource = new InputStreamResource(inputStream) {
			@Override
			public String getFilename() {
				return filename;
			}
		};

		when(messageExchangeClientMock.getMessageAttachment(municipalityId, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId))
			.thenReturn(ResponseEntity.ok()
				.contentType(contentType)
				.contentLength(content.length)
				.body(inputStreamResource));

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, ERRAND_ID, conversationId))
			.thenReturn(Optional.of(conversationEntity));

		final var response = new MockHttpServletResponse();

		// Act
		conversationService.getConversationMessageAttachment(municipalityId, namespace, ERRAND_ID, conversationId, messageId, attachmentId, response);

		// Assert
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentType()).isEqualTo(contentType.toString());
		assertThat(response.getHeader("Content-Disposition")).contains(filename);
		assertThat(response.getContentAsByteArray()).isEqualTo(content);

		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, ERRAND_ID, conversationId);
		verify(messageExchangeClientMock).getMessageAttachment(municipalityId, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId);
	}

	@Test
	void getConversationMessageAttachmentMissingExchangeId() {
		// Arrange
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var conversationId = "convId";
		final var messageId = "msgId";
		final var attachmentId = "attId";

		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(null);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, ERRAND_ID, conversationId))
			.thenReturn(Optional.of(conversationEntity));

		final var response = new MockHttpServletResponse();

		// Act & Assert
		assertThatThrownBy(() -> conversationService.getConversationMessageAttachment(municipalityId, namespace, ERRAND_ID, conversationId, messageId, attachmentId, response))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Conversation not found in local database");

		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, ERRAND_ID, conversationId);
		verifyNoInteractions(messageExchangeClientMock);
	}

	@Test
	void getConversationMessageAttachmentNon2xxResponse() {
		// Arrange
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var conversationId = "convId";
		final var messageId = "msgId";
		final var attachmentId = "attId";
		final var messageExchangeId = "exchangeId";

		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(messageExchangeId);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, ERRAND_ID, conversationId))
			.thenReturn(Optional.of(conversationEntity));
		when(messageExchangeClientMock.getMessageAttachment(municipalityId, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId))
			.thenReturn(ResponseEntity.status(404).build());

		final var response = new MockHttpServletResponse();

		// Act & Assert
		assertThatThrownBy(() -> conversationService.getConversationMessageAttachment(municipalityId, namespace, ERRAND_ID, conversationId, messageId, attachmentId, response))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Attachment not found or invalid in Message Exchange");

		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, ERRAND_ID, conversationId);
		verify(messageExchangeClientMock).getMessageAttachment(municipalityId, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId);
	}

	@Test
	void getConversationMessageAttachmentNullBody() {
		// Arrange
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var conversationId = "convId";
		final var messageId = "msgId";
		final var attachmentId = "attId";
		final var messageExchangeId = "exchangeId";

		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(messageExchangeId);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, ERRAND_ID, conversationId))
			.thenReturn(Optional.of(conversationEntity));
		when(messageExchangeClientMock.getMessageAttachment(municipalityId, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId))
			.thenReturn(ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(null));

		final var response = new MockHttpServletResponse();

		// Act & Assert
		assertThatThrownBy(() -> conversationService.getConversationMessageAttachment(municipalityId, namespace, ERRAND_ID, conversationId, messageId, attachmentId, response))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Not Found: Attachment not found or invalid in Message Exchange");

		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, ERRAND_ID, conversationId);
		verify(messageExchangeClientMock).getMessageAttachment(municipalityId, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId);
	}

	@Test
	void getConversationMessageAttachmentNullContentType() {
		// Arrange
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var conversationId = "convId";
		final var messageId = "msgId";
		final var attachmentId = "attId";
		final var messageExchangeId = "exchangeId";
		final var filename = "file.txt";
		final var content = "test content".getBytes();
		final var inputStream = new java.io.ByteArrayInputStream(content);
		final var inputStreamResource = new InputStreamResource(inputStream) {
			@Override
			public String getFilename() {
				return filename;
			}
		};

		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(messageExchangeId);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, ERRAND_ID, conversationId))
			.thenReturn(Optional.of(conversationEntity));
		when(messageExchangeClientMock.getMessageAttachment(municipalityId, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId))
			.thenReturn(ResponseEntity.ok().body(inputStreamResource));

		final var response = new MockHttpServletResponse();

		// Act & Assert
		assertThatThrownBy(() -> conversationService.getConversationMessageAttachment(municipalityId, namespace, ERRAND_ID, conversationId, messageId, attachmentId, response))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Not Found: Attachment not found or invalid in Message Exchange");

		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(municipalityId, namespace, ERRAND_ID, conversationId);
		verify(messageExchangeClientMock).getMessageAttachment(municipalityId, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId);
	}
}
