package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.messageexchange.Conversation;
import generated.se.sundsvall.messageexchange.Message;
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
import org.mockito.Mockito;
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
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.mapper.ConversationMapper;
import se.sundsvall.supportmanagement.service.scheduler.messageexchange.MessageExchangeScheduler;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType.EXTERNAL;
import static se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType.INTERNAL;
import static se.sundsvall.supportmanagement.service.ConversationService.CONVERSATION_DEPARTMENT_NAME;

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
	private static final ConversationType CONVERSATION_TYPE = EXTERNAL;
	private static final List<String> VALUES_LIST = List.of("value1", "value2");
	private static final List<String> RELATION_VALUES_LIST = List.of("rel1", "rel2");

	@Mock
	private MessageExchangeClient messageExchangeClientMock;

	@Mock
	private ConversationRepository conversationRepositoryMock;

	@Mock
	private MessageExchangeScheduler messageExchangeSchedulerMock;

	@Mock
	private CommunicationService communicationServiceMock;

	@Mock
	private RelationClient relationClientMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

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

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, RW);
		verify(conversationRepositoryMock).save(conversationEntityCaptor.capture());
		verify(messageExchangeClientMock).createConversation(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), any());
		verify(messageExchangeClientMock).getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, CONVERSATION_ID);

		assertThat(conversationEntityCaptor.getValue().getMessageExchangeId()).isEqualTo(MESSAGE_EXCHANGE_ID);
		assertThat(conversationEntityCaptor.getValue().getRelationIds()).isEqualTo(RELATION_VALUES_LIST);
		assertThat(conversationEntityCaptor.getValue().getLatestSyncedSequenceNumber()).isEqualTo(LATEST_SEQUENCE_NUMBER);

		verifyNoInteractions(messageExchangeSchedulerMock, communicationServiceMock);
		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock);
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
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, RW);
		verify(messageExchangeClientMock).createConversation(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), any());
		verify(conversationRepositoryMock, never()).save(any());
		verify(messageExchangeClientMock, never()).getConversationById(any(), any(), any());

		verifyNoInteractions(messageExchangeSchedulerMock, communicationServiceMock, conversationRepositoryMock);
		verifyNoMoreInteractions(messageExchangeClientMock);
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

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(conversationRepositoryMock).save(conversationEntityCaptor.capture());
		verify(messageExchangeClientMock).updateConversationById(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any());

		assertThat(conversationEntityCaptor.getValue().getMessageExchangeId()).isEqualTo(MESSAGE_EXCHANGE_ID);
		assertThat(conversationEntityCaptor.getValue().getRelationIds()).isEqualTo(RELATION_VALUES_LIST);
		assertThat(conversationEntityCaptor.getValue().getLatestSyncedSequenceNumber()).isEqualTo(LATEST_SEQUENCE_NUMBER);

		verifyNoInteractions(messageExchangeSchedulerMock, communicationServiceMock);
		verifyNoMoreInteractions(messageExchangeClientMock, conversationRepositoryMock);
	}

	@Test
	void readConversations() {
		// Arrange
		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(ConversationEntity.create()));

		// Act
		final var response = conversationService.readConversations(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		// Assert
		assertThat(response).isNotNull().hasSize(1);

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verifyNoInteractions(messageExchangeSchedulerMock, communicationServiceMock, messageExchangeClientMock);
		verifyNoMoreInteractions(conversationRepositoryMock);
	}

	@Test
	void readConversationById() {
		// Arrange
		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(MESSAGE_EXCHANGE_ID).withId(CONVERSATION_ID);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID)).thenReturn(Optional.of(conversationEntity));
		when(messageExchangeClientMock.getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID)).thenReturn(ResponseEntity.ok(createMessageExchangeConversation()));

		// Act
		final var response = conversationService.readConversationById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getTopic()).isEqualTo(TOPIC);

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(messageExchangeSchedulerMock).triggerSyncConversationsAsync();
		verify(messageExchangeClientMock).getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID);

		verifyNoInteractions(communicationServiceMock);
		verifyNoMoreInteractions(messageExchangeClientMock, conversationRepositoryMock, messageExchangeSchedulerMock);
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
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(conversationRepositoryMock, never()).save(any());

		verifyNoInteractions(messageExchangeClientMock, messageExchangeSchedulerMock, communicationServiceMock);
		verifyNoMoreInteractions(conversationRepositoryMock);
	}

	@Test
	void createExternalMessage() {
		// Arrange
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID).withType(EXTERNAL.name()).withMessageExchangeId(MESSAGE_EXCHANGE_ID).withErrandId(ERRAND_ID);
		final var messageRequest = MessageRequest.create();

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID))
			.thenReturn(Optional.ofNullable(conversationEntity));

		when(messageExchangeClientMock.createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), eq(null)))
			.thenReturn(ResponseEntity.ok().build());

		// Act
		conversationService.createMessage(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, messageRequest, null);

		// Assert
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(messageExchangeClientMock).createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), eq(null));
		verify(communicationServiceMock).sendMessageNotification(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_DEPARTMENT_NAME);

		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, communicationServiceMock);
		verifyNoInteractions(messageExchangeSchedulerMock);
	}

	@Test
	void createExternalMessageWithAttachment() {
		// Arrange
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID).withType(EXTERNAL.name()).withMessageExchangeId(MESSAGE_EXCHANGE_ID).withErrandId(ERRAND_ID);
		final var messageRequest = MessageRequest.create();
		final var multipartFile = new MockMultipartFile("attachments", "attachment.txt".getBytes());

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID))
			.thenReturn(Optional.ofNullable(conversationEntity));

		when(messageExchangeClientMock.createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), any()))
			.thenReturn(ResponseEntity.ok().build());

		// Act
		conversationService.createMessage(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, messageRequest, List.of(multipartFile));

		// Assert
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(messageExchangeClientMock).createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), any());
		verify(messageExchangeSchedulerMock).triggerSyncConversationsAsync();
		verify(communicationServiceMock).sendMessageNotification(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_DEPARTMENT_NAME);

		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, messageExchangeSchedulerMock, communicationServiceMock);
	}

	@Test
	void createInternalMessageWhenNotifyReporterIsActivated() {
		// Arrange
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID).withType(INTERNAL.name()).withMessageExchangeId(MESSAGE_EXCHANGE_ID).withErrandId(ERRAND_ID);
		final var messageRequest = MessageRequest.create();
		final var namespaceConfigMock = Mockito.mock(NamespaceConfig.class);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID)).thenReturn(Optional.ofNullable(conversationEntity));
		when(messageExchangeClientMock.createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), eq(null))).thenReturn(ResponseEntity.ok().build());
		when(namespaceConfigServiceMock.get(NAMESPACE, MUNICIPALITY_ID)).thenReturn(namespaceConfigMock);
		when(namespaceConfigMock.isNotifyReporter()).thenReturn(true);

		// Act
		conversationService.createMessage(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, messageRequest, null);

		// Assert
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(messageExchangeClientMock).createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), eq(null));
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(namespaceConfigMock).isNotifyReporter();
		verify(communicationServiceMock).sendEmailNotificationToReporter(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_DEPARTMENT_NAME);

		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, communicationServiceMock, namespaceConfigServiceMock, namespaceConfigMock);
		verifyNoInteractions(messageExchangeSchedulerMock);
	}

	@Test
	void createInternalMessageWhenNotifyReporterIsNotActivated() {
		// Arrange
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID).withType(INTERNAL.name()).withMessageExchangeId(MESSAGE_EXCHANGE_ID).withErrandId(ERRAND_ID);
		final var messageRequest = MessageRequest.create();
		final var namespaceConfigMock = Mockito.mock(NamespaceConfig.class);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID)).thenReturn(Optional.ofNullable(conversationEntity));
		when(messageExchangeClientMock.createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), eq(null))).thenReturn(ResponseEntity.ok().build());
		when(namespaceConfigServiceMock.get(NAMESPACE, MUNICIPALITY_ID)).thenReturn(namespaceConfigMock);

		// Act
		conversationService.createMessage(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, messageRequest, null);

		// Assert
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(messageExchangeClientMock).createMessage(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), eq(null));
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(namespaceConfigMock).isNotifyReporter();

		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, namespaceConfigServiceMock, namespaceConfigMock);
		verifyNoInteractions(communicationServiceMock, messageExchangeSchedulerMock);
	}

	@Test
	void getMessages() {
		// Arrange
		final var pageable = PageRequest.of(0, 10);
		final var conversationEntity = ConversationEntity.create().withId(CONVERSATION_ID).withMessageExchangeId(MESSAGE_EXCHANGE_ID).withErrandId(ERRAND_ID);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID))
			.thenReturn(Optional.ofNullable(conversationEntity));

		when(messageExchangeClientMock.getMessages(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any(), any()))
			.thenReturn(ResponseEntity.ok(new PageImpl<>(List.of(new generated.se.sundsvall.messageexchange.Message().type(Message.TypeEnum.USER_CREATED)))));

		// Act
		final var result = conversationService.getMessages(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID, pageable);

		// Assert
		assertThat(result).isNotNull().hasSize(1);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CONVERSATION_ID);
		verify(messageExchangeClientMock).getMessages(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID, null, pageable);
		verify(messageExchangeSchedulerMock).triggerSyncConversationsAsync();

		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, messageExchangeSchedulerMock);
		verifyNoInteractions(communicationServiceMock);
	}

	private Conversation createMessageExchangeConversation() {
		return new Conversation()
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

		when(messageExchangeClientMock.getMessageAttachment(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId))
			.thenReturn(ResponseEntity.ok()
				.contentType(contentType)
				.contentLength(content.length)
				.body(inputStreamResource));

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId))
			.thenReturn(Optional.of(conversationEntity));

		final var response = new MockHttpServletResponse();

		// Act
		conversationService.getConversationMessageAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId, messageId, attachmentId, response);

		// Assert
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentType()).isEqualTo(contentType.toString());
		assertThat(response.getHeader("Content-Disposition")).contains(filename);
		assertThat(response.getContentAsByteArray()).isEqualTo(content);

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId);
		verify(messageExchangeClientMock).getMessageAttachment(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId);

		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, messageExchangeSchedulerMock);
		verifyNoInteractions(communicationServiceMock);
	}

	@Test
	void getConversationMessageAttachmentMissingExchangeId() {
		// Arrange
		final var conversationId = "convId";
		final var messageId = "msgId";
		final var attachmentId = "attId";

		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(null);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId))
			.thenReturn(Optional.of(conversationEntity));

		final var response = new MockHttpServletResponse();

		// Act & Assert
		assertThatThrownBy(() -> conversationService.getConversationMessageAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId, messageId, attachmentId, response))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Conversation not found in local database");

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId);

		verifyNoMoreInteractions(conversationRepositoryMock);
		verifyNoInteractions(communicationServiceMock, messageExchangeClientMock, messageExchangeSchedulerMock);
	}

	@Test
	void getConversationMessageAttachmentNon2xxResponse() {
		// Arrange
		final var conversationId = "convId";
		final var messageId = "msgId";
		final var attachmentId = "attId";
		final var messageExchangeId = "exchangeId";

		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(messageExchangeId);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId))
			.thenReturn(Optional.of(conversationEntity));
		when(messageExchangeClientMock.getMessageAttachment(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId))
			.thenReturn(ResponseEntity.status(404).build());

		final var response = new MockHttpServletResponse();

		// Act & Assert
		assertThatThrownBy(() -> conversationService.getConversationMessageAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId, messageId, attachmentId, response))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Attachment not found or invalid in Message Exchange");

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId);
		verify(messageExchangeClientMock).getMessageAttachment(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId);

		verifyNoInteractions(communicationServiceMock);
		verifyNoMoreInteractions(messageExchangeClientMock, messageExchangeSchedulerMock, conversationRepositoryMock);

	}

	@Test
	void getConversationMessageAttachmentNullBody() {
		// Arrange
		final var conversationId = "convId";
		final var messageId = "msgId";
		final var attachmentId = "attId";
		final var messageExchangeId = "exchangeId";

		final var conversationEntity = ConversationEntity.create().withMessageExchangeId(messageExchangeId);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId))
			.thenReturn(Optional.of(conversationEntity));
		when(messageExchangeClientMock.getMessageAttachment(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId))
			.thenReturn(ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(null));

		final var response = new MockHttpServletResponse();

		// Act & Assert
		assertThatThrownBy(() -> conversationService.getConversationMessageAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId, messageId, attachmentId, response))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Not Found: Attachment not found or invalid in Message Exchange");

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId);
		verify(messageExchangeClientMock).getMessageAttachment(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId);

		verifyNoInteractions(communicationServiceMock, messageExchangeSchedulerMock);
		verifyNoMoreInteractions(messageExchangeClientMock, conversationRepositoryMock);
	}

	@Test
	void getConversationMessageAttachmentNullContentType() {
		// Arrange
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

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId))
			.thenReturn(Optional.of(conversationEntity));
		when(messageExchangeClientMock.getMessageAttachment(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId))
			.thenReturn(ResponseEntity.ok().body(inputStreamResource));

		final var response = new MockHttpServletResponse();

		// Act & Assert
		assertThatThrownBy(() -> conversationService.getConversationMessageAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId, messageId, attachmentId, response))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Not Found: Attachment not found or invalid in Message Exchange");

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, R, RW);
		verify(conversationRepositoryMock).findByMunicipalityIdAndNamespaceAndErrandIdAndId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, conversationId);
		verify(messageExchangeClientMock).getMessageAttachment(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, messageExchangeId, messageId, attachmentId);

		verifyNoInteractions(communicationServiceMock, messageExchangeSchedulerMock);
		verifyNoMoreInteractions(messageExchangeClientMock, conversationRepositoryMock);
	}

	@Test
	void deleteByErrandIdRemoveOnlyRelationExternalReferenceWhenOthersExist() {
		// Arrange
		final var errandEntity = ErrandEntity.create()
			.withId(ERRAND_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID);
		final var relationIds = List.of("r1");
		final var conversation = ConversationEntity.create()
			.withId(CONVERSATION_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandId(ERRAND_ID)
			.withMessageExchangeId(MESSAGE_EXCHANGE_ID)
			.withRelationIds(relationIds);

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(conversation));

		final var meConversation = new Conversation()
			.id(MESSAGE_EXCHANGE_ID)
			.municipalityId(MUNICIPALITY_ID)
			.namespace(MESSAGE_EXCHANGE_NAMESPACE)
			.externalReferences(List.of(
				new generated.se.sundsvall.messageexchange.KeyValues().key("someOtherKey").values(List.of("foo")),
				new generated.se.sundsvall.messageexchange.KeyValues().key("relationIds").values(List.of("r1", "r2"))));

		when(messageExchangeClientMock.getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID))
			.thenReturn(ResponseEntity.ok(meConversation));
		when(messageExchangeClientMock.updateConversationById(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any()))
			.thenReturn(ResponseEntity.ok(new Conversation()));

		// Act
		conversationService.deleteByErrandId(errandEntity);

		// Assert
		verify(relationClientMock).deleteRelation(MUNICIPALITY_ID, "r1");
		verify(messageExchangeClientMock, never()).deleteConversation(any(), any(), any());
		verify(messageExchangeClientMock).updateConversationById(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any());
		verify(conversationRepositoryMock).deleteAll(List.of(conversation));

		verifyNoInteractions(messageExchangeSchedulerMock, communicationServiceMock);
		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, relationClientMock);
	}

	@Test
	void deleteByErrandIdDeleteMessageExchangeConversationWhenRelationIsOnlyExternalReference() {
		// Arrange
		final var errandEntity = ErrandEntity.create()
			.withId(ERRAND_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID);
		final var conversation = ConversationEntity.create()
			.withId(CONVERSATION_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandId(ERRAND_ID)
			.withMessageExchangeId(MESSAGE_EXCHANGE_ID)
			.withRelationIds(List.of("r1"));

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(conversation));

		final var meConversation = new Conversation()
			.id(MESSAGE_EXCHANGE_ID)
			.municipalityId(MUNICIPALITY_ID)
			.namespace(MESSAGE_EXCHANGE_NAMESPACE)
			.externalReferences(List.of(
				new generated.se.sundsvall.messageexchange.KeyValues().key("relationIds").values(List.of("r1"))));

		when(messageExchangeClientMock.getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID))
			.thenReturn(ResponseEntity.ok(meConversation));

		// Act
		conversationService.deleteByErrandId(errandEntity);

		// Assert
		verify(relationClientMock).deleteRelation(MUNICIPALITY_ID, "r1");
		verify(messageExchangeClientMock).deleteConversation(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID);
		verify(messageExchangeClientMock, never()).updateConversationById(any(), any(), any(), any());
		verify(conversationRepositoryMock).deleteAll(List.of(conversation));

		verifyNoInteractions(messageExchangeSchedulerMock, communicationServiceMock);
		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, relationClientMock);
	}

	@Test
	void deleteByErrandIdNoUpdateWhenNoExternalReferenceMatchesRelationIds() {
		// Arrange
		final var errandEntity = ErrandEntity.create()
			.withId(ERRAND_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID);
		final var conversation = ConversationEntity.create()
			.withId(CONVERSATION_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandId(ERRAND_ID)
			.withMessageExchangeId(MESSAGE_EXCHANGE_ID)
			.withRelationIds(List.of("rX", "rY"));

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(conversation));

		final var meConversation = new Conversation()
			.id(MESSAGE_EXCHANGE_ID)
			.municipalityId(MUNICIPALITY_ID)
			.namespace(MESSAGE_EXCHANGE_NAMESPACE)
			.externalReferences(List.of(
				new generated.se.sundsvall.messageexchange.KeyValues().key("relationIds").values(List.of("r1", "r2")),
				new generated.se.sundsvall.messageexchange.KeyValues().key("other").values(List.of("a"))));

		when(messageExchangeClientMock.getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID))
			.thenReturn(ResponseEntity.ok(meConversation));
		when(messageExchangeClientMock.updateConversationById(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any()))
			.thenReturn(ResponseEntity.ok(new Conversation()));

		// Act
		conversationService.deleteByErrandId(errandEntity);

		// Assert
		verify(relationClientMock).deleteRelation(MUNICIPALITY_ID, "rX");
		verify(relationClientMock).deleteRelation(MUNICIPALITY_ID, "rY");
		verify(messageExchangeClientMock, never()).deleteConversation(any(), any(), any());
		verify(messageExchangeClientMock).updateConversationById(eq(MUNICIPALITY_ID), eq(MESSAGE_EXCHANGE_NAMESPACE), eq(MESSAGE_EXCHANGE_ID), any());
		verify(conversationRepositoryMock).deleteAll(List.of(conversation));

		verifyNoInteractions(messageExchangeSchedulerMock, communicationServiceMock);
		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, relationClientMock);
	}

	@Test
	void deleteByErrandIdWhenNullReferences() {
		// Arrange
		final var errandEntity = ErrandEntity.create()
			.withId(ERRAND_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID);
		final var conversation = ConversationEntity.create()
			.withId(CONVERSATION_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandId(ERRAND_ID)
			.withMessageExchangeId(MESSAGE_EXCHANGE_ID)
			.withRelationIds(List.of("r1"));

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(conversation));

		final var meConversation = new Conversation()
			.id(MESSAGE_EXCHANGE_ID)
			.municipalityId(MUNICIPALITY_ID)
			.namespace(MESSAGE_EXCHANGE_NAMESPACE)
			.externalReferences(null);

		when(messageExchangeClientMock.getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID))
			.thenReturn(ResponseEntity.ok(meConversation));

		// Act
		conversationService.deleteByErrandId(errandEntity);

		// Assert
		verify(relationClientMock).deleteRelation(MUNICIPALITY_ID, "r1");
		verify(messageExchangeClientMock).deleteConversation(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID);
		verify(messageExchangeClientMock, never()).updateConversationById(any(), any(), any(), any());
		verify(conversationRepositoryMock).deleteAll(List.of(conversation));

		verifyNoInteractions(messageExchangeSchedulerMock, communicationServiceMock);
		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, relationClientMock);
	}

	@Test
	void deleteByErrandIdExceptionThrown() {
		// Arrange
		final var errandEntity = ErrandEntity.create()
			.withId(ERRAND_ID)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID);
		final var conversation = ConversationEntity.create()
			.withId(CONVERSATION_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandId(ERRAND_ID)
			.withMessageExchangeId(MESSAGE_EXCHANGE_ID)
			.withRelationIds(List.of("r1"));

		when(conversationRepositoryMock.findByMunicipalityIdAndNamespaceAndErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(conversation));

		doThrow(new RuntimeException("boom")).when(relationClientMock).deleteRelation(MUNICIPALITY_ID, "r1");

		final var meConversation = new Conversation()
			.id(MESSAGE_EXCHANGE_ID)
			.municipalityId(MUNICIPALITY_ID)
			.namespace(MESSAGE_EXCHANGE_NAMESPACE)
			.externalReferences(List.of(new generated.se.sundsvall.messageexchange.KeyValues().key("relationIds").values(List.of("r1"))));

		when(messageExchangeClientMock.getConversationById(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID))
			.thenReturn(ResponseEntity.ok(meConversation));
		doThrow(new RuntimeException("me-boom")).when(messageExchangeClientMock).deleteConversation(MUNICIPALITY_ID, MESSAGE_EXCHANGE_NAMESPACE, MESSAGE_EXCHANGE_ID);

		// Act (should not throw)
		conversationService.deleteByErrandId(errandEntity);

		// Assert
		verify(conversationRepositoryMock).deleteAll(List.of(conversation));
		verifyNoInteractions(messageExchangeSchedulerMock, communicationServiceMock);
		verifyNoMoreInteractions(conversationRepositoryMock, messageExchangeClientMock, relationClientMock);
	}
}
