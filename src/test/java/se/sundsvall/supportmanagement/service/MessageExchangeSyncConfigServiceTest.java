package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.RELATION_ID_KEY;

import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.messageexchange.Conversation;
import generated.se.sundsvall.messageexchange.Identifier;
import generated.se.sundsvall.messageexchange.KeyValues;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;

@ExtendWith(MockitoExtension.class)
class MessageExchangeSyncConfigServiceTest {

	@Mock
	private MessageExchangeClient messageExchangeClientMock;
	@Mock
	private ErrandAttachmentService attachmentServiceMock;
	@Mock
	private ConversationRepository conversationRepositoryMock;
	@Mock
	private ErrandsRepository errandsRepositoryMock;
	@Mock
	private EventService eventServiceMock;
	@Captor
	private ArgumentCaptor<ConversationEntity> conversationEntityCaptor;
	@InjectMocks
	private MessageExchangeSyncService service;

	private static final String MESSAGE_EXCHANGE_NS = "messageExchangeNamespace";

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "messageExchangeNamespace", MESSAGE_EXCHANGE_NS);
	}

	@Test
	void syncConversationAndMessages() {

		// Arrange
		final var id = "id";
		final var messageExchangeId = "messageExchangeId";
		final var errandId = "errandId";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var topic = "topic";
		final var type = "INTERNAL";
		final var relationIds = List.of("relationId");
		final var latestSyncedSequenceNumber = 123L;
		final var targetRelationId = "targetRelationId";
		final var newLatestSyncedSequenceNumber = 456L;
		final var user = "user";
		final var newRelationList = List.of("newRelationId");

		final var entity = ConversationEntity.create()
			.withId(id)
			.withMessageExchangeId(messageExchangeId)
			.withErrandId(errandId)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withTopic(topic)
			.withType(type)
			.withRelationIds(relationIds)
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber)
			.withTargetRelationId(targetRelationId);

		final var conversation = new Conversation()
			.id(id)
			.namespace(namespace)
			.municipalityId(municipalityId)
			.participants(List.of(new Identifier().type("relation").value(relationIds.getFirst())))
			.externalReferences(List.of(new KeyValues().key(RELATION_ID_KEY).values(newRelationList)))
			.metadata(List.of(new KeyValues().key("key").values(List.of("value"))))
			.topic(topic)
			.latestSequenceNumber(newLatestSyncedSequenceNumber);

		final var errandEntity = ErrandEntity.create().withAssignedGroupId(user);

		when(conversationRepositoryMock.save(entity)).thenReturn(entity);
		when(errandsRepositoryMock.getReferenceById(any())).thenReturn(errandEntity);
		when(messageExchangeClientMock.getMessages(eq(municipalityId), eq(MESSAGE_EXCHANGE_NS), any(), any(), any()))
			.thenReturn(ResponseEntity.ok(new PageImpl<>(List.of(new generated.se.sundsvall.messageexchange.Message()))));

		// Act
		service.syncConversation(entity, conversation);

		// Assert
		verify(errandsRepositoryMock).getReferenceById(errandId);
		verify(messageExchangeClientMock).getMessages(municipalityId, MESSAGE_EXCHANGE_NS, messageExchangeId, "sequenceNumber.id >123", Pageable.unpaged());
		verify(eventServiceMock).createErrandEvent(eq(EventType.UPDATE), eq("Ny händelse i konversation topic"), same(errandEntity), eq(null), eq(null), eq(true), eq(NotificationSubType.MESSAGE));
		verify(conversationRepositoryMock).save(conversationEntityCaptor.capture());

		final var savedEntity = conversationEntityCaptor.getValue();
		assertThat(savedEntity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(savedEntity.getId()).isEqualTo(id);
		assertThat(savedEntity.getTopic()).isEqualTo(topic);
		assertThat(savedEntity.getType()).isEqualTo(type);
		assertThat(savedEntity.getRelationIds()).isEqualTo(newRelationList);
		assertThat(savedEntity.getLatestSyncedSequenceNumber()).isEqualTo(newLatestSyncedSequenceNumber);

		verifyNoMoreInteractions(messageExchangeClientMock, attachmentServiceMock);
	}

	@Test
	void syncMessages() {
		// Arrange
		final var errandId = 123L;
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var user = "user";
		final var messageExchangeId = "messageExchangeId";
		final var latestSyncedSequenceNumber = 99L;
		final var conversationEntity = ConversationEntity.create()
			.withErrandId(String.valueOf(errandId))
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withMessageExchangeId(messageExchangeId)
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber);

		when(messageExchangeClientMock.getMessages(eq(municipalityId), eq(MESSAGE_EXCHANGE_NS), any(), any(), any()))
			.thenReturn(ResponseEntity.ok(new PageImpl<>(List.of(new generated.se.sundsvall.messageexchange.Message()))));

		// Act
		var shouldNotify = service.syncMessages(conversationEntity, user);

		// Assert
		assertThat(shouldNotify).isTrue();
		verify(messageExchangeClientMock).getMessages(municipalityId, MESSAGE_EXCHANGE_NS, messageExchangeId, "sequenceNumber.id >99", Pageable.unpaged());
		verifyNoMoreInteractions(messageExchangeClientMock);
		verifyNoInteractions(attachmentServiceMock, conversationRepositoryMock);
	}

	@Test
	void syncMessagesCreatedByAssignedUser() {
		// Arrange
		final var errandId = 123L;
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var user = "user";
		final var messageExchangeId = "messageExchangeId";
		final var latestSyncedSequenceNumber = 99L;
		final var conversationEntity = ConversationEntity.create()
			.withErrandId(String.valueOf(errandId))
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withMessageExchangeId(messageExchangeId)
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber);

		when(messageExchangeClientMock.getMessages(eq(municipalityId), eq(MESSAGE_EXCHANGE_NS), any(), any(), any()))
			.thenReturn(ResponseEntity.ok(new PageImpl<>(List.of(new generated.se.sundsvall.messageexchange.Message().createdBy(new Identifier().value(user))))));

		// Act
		var shouldNotify = service.syncMessages(conversationEntity, user);

		// Assert
		assertThat(shouldNotify).isFalse();
		verify(messageExchangeClientMock).getMessages(municipalityId, MESSAGE_EXCHANGE_NS, messageExchangeId, "sequenceNumber.id >99", Pageable.unpaged());
		verifyNoMoreInteractions(messageExchangeClientMock);
		verifyNoInteractions(attachmentServiceMock, conversationRepositoryMock);
	}

	@Test
	void syncMessagesCreatedByAssignedUserAndOtherUser() {
		// Arrange
		final var errandId = 123L;
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var user = "user";
		final var otherUser = "otherUser";
		final var messageExchangeId = "messageExchangeId";
		final var latestSyncedSequenceNumber = 99L;
		final var conversationEntity = ConversationEntity.create()
			.withErrandId(String.valueOf(errandId))
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withMessageExchangeId(messageExchangeId)
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber);

		when(messageExchangeClientMock.getMessages(eq(municipalityId), eq(MESSAGE_EXCHANGE_NS), any(), any(), any()))
			.thenReturn(ResponseEntity.ok(new PageImpl<>(List.of(
				new generated.se.sundsvall.messageexchange.Message().createdBy(new Identifier().value(user)),
				new generated.se.sundsvall.messageexchange.Message().createdBy(new Identifier().value(otherUser))))));

		// Act
		var shouldNotify = service.syncMessages(conversationEntity, user);

		// Assert
		assertThat(shouldNotify).isTrue();
		verify(messageExchangeClientMock).getMessages(municipalityId, MESSAGE_EXCHANGE_NS, messageExchangeId, "sequenceNumber.id >99", Pageable.unpaged());
		verifyNoMoreInteractions(messageExchangeClientMock);
		verifyNoInteractions(attachmentServiceMock, conversationRepositoryMock);
	}

	@Test
	void syncMessagesNoMessages() {
		// Arrange
		final var errandId = 123L;
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var user = "user";
		final var messageExchangeId = "messageExchangeId";
		final var latestSyncedSequenceNumber = 99L;
		final var conversationEntity = ConversationEntity.create()
			.withErrandId(String.valueOf(errandId))
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withMessageExchangeId(messageExchangeId)
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber);

		when(messageExchangeClientMock.getMessages(eq(municipalityId), eq(MESSAGE_EXCHANGE_NS), any(), any(), any()))
			.thenReturn(ResponseEntity.ok(new PageImpl<>(List.of())));

		// Act
		final var shouldNotify = service.syncMessages(conversationEntity, user);

		// Assert
		assertThat(shouldNotify).isFalse();
		verify(messageExchangeClientMock).getMessages(municipalityId, MESSAGE_EXCHANGE_NS, messageExchangeId, "sequenceNumber.id >99", Pageable.unpaged());
		verifyNoMoreInteractions(messageExchangeClientMock);
		verifyNoInteractions(attachmentServiceMock, conversationRepositoryMock);
	}

	@Test
	void syncMessagesNoResponse() {
		// Arrange
		final var errandId = 123L;
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var user = "user";
		final var messageExchangeId = "messageExchangeId";
		final var latestSyncedSequenceNumber = 99L;
		final var conversationEntity = ConversationEntity.create()
			.withErrandId(String.valueOf(errandId))
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withMessageExchangeId(messageExchangeId)
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber);

		when(messageExchangeClientMock.getMessages(eq(municipalityId), eq(MESSAGE_EXCHANGE_NS), any(), any(), any()))
			.thenReturn(null);

		// Act & Assert
		assertThatThrownBy(() -> service.syncMessages(conversationEntity, user))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Failed to retrieve messages from Message Exchange");

		verify(messageExchangeClientMock).getMessages(municipalityId, MESSAGE_EXCHANGE_NS, messageExchangeId, "sequenceNumber.id >99", Pageable.unpaged());
		verifyNoMoreInteractions(messageExchangeClientMock);
		verifyNoInteractions(attachmentServiceMock, conversationRepositoryMock);
	}

	@Test
	void syncAttachment() {
		// Arrange
		final var errandId = "123L";
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var conversationEntity = ConversationEntity.create()
			.withErrandId(errandId)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace);
		final var message = new generated.se.sundsvall.messageexchange.Message();
		final var attachment = new generated.se.sundsvall.messageexchange.Attachment().id("attachmentId");

		when(messageExchangeClientMock.getMessageAttachment(eq(municipalityId), any(), any(), any(), eq(attachment.getId())))
			.thenReturn(ResponseEntity.ok()
				.header("Content-Type", "application/octet-stream")
				.body(new InputStreamResource(new ByteArrayInputStream(new byte[0]))));

		// Act
		service.syncAttachment(conversationEntity, message, attachment);

		// Assert
		verify(messageExchangeClientMock).getMessageAttachment(eq(municipalityId), any(), any(), any(), eq(attachment.getId()));
		verify(attachmentServiceMock).createErrandAttachment(eq(namespace), eq(municipalityId), eq(errandId), ArgumentMatchers.<ResponseEntity<InputStreamResource>>any());
		verifyNoMoreInteractions(attachmentServiceMock, messageExchangeClientMock);
		verifyNoInteractions(conversationRepositoryMock);
	}

	@Test
	void saveAttachment() {
		// Arrange
		final var errandId = "123L";
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var file = ResponseEntity.ok()
			.header("Content-Type", "application/octet-stream")
			.body(new InputStreamResource(new ByteArrayInputStream(new byte[0])));

		// Act
		service.saveAttachment(errandId, municipalityId, namespace, file);

		// Assert
		verify(attachmentServiceMock).createErrandAttachment(eq(namespace), eq(municipalityId), eq(errandId), ArgumentMatchers.<ResponseEntity<InputStreamResource>>any());
		verifyNoMoreInteractions(attachmentServiceMock);
		verifyNoInteractions(conversationRepositoryMock, messageExchangeClientMock);
	}

	@Test
	void saveAttachmentNullFile() {
		// Arrange
		final var errandId = "123L";
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final ResponseEntity<InputStreamResource> file = ResponseEntity.ok()
			.header("Content-Type", "application/octet-stream")
			.build();

		// Act & Assert
		assertThatThrownBy(() -> service.saveAttachment(errandId, municipalityId, namespace, file))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Failed to retrieve attachment from Message Exchange");

		verifyNoInteractions(attachmentServiceMock, conversationRepositoryMock, messageExchangeClientMock);
	}

	@Test
	void saveAttachmentNoContentType() {
		// Arrange
		final var errandId = "123L";
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final ResponseEntity<InputStreamResource> file = ResponseEntity.ok()
			.body(new InputStreamResource(new ByteArrayInputStream(new byte[0])));

		// Act & Assert
		assertThatThrownBy(() -> service.saveAttachment(errandId, municipalityId, namespace, file))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Failed to retrieve attachment from Message Exchange");

		verifyNoInteractions(conversationRepositoryMock, messageExchangeClientMock);
	}

}
