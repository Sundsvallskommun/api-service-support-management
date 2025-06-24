package se.sundsvall.supportmanagement.service.scheduler.messageexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.service.mapper.ConversationMapper.RELATION_ID_KEY;

import generated.se.sundsvall.messageexchange.Conversation;
import generated.se.sundsvall.messageexchange.KeyValues;
import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MessageExchangeSyncRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.ConversationService;

@ExtendWith(MockitoExtension.class)
class MessageExchangeWorkerTest {

	@Mock
	private MessageExchangeClient messageExchangeClientMock;
	@Mock
	private MessageExchangeSyncRepository messageExchangeSyncRepositoryMock;
	@Mock
	private ConversationRepository conversationRepositoryMock;
	@Mock
	private ConversationService conversationServiceMock;
	@Mock
	private RelationClient relationClientMock;
	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Captor
	private ArgumentCaptor<ConversationEntity> conversationEntityArgumentCaptor;

	@InjectMocks
	private MessageExchangeWorker messageExchangeWorker;

	@Test
	void getActiveSyncEntities() {
		var entity = MessageExchangeSyncEntity.create();
		when(messageExchangeSyncRepositoryMock.findByActive(any())).thenReturn(List.of(entity));

		var list = messageExchangeWorker.getActiveSyncEntities();

		verify(messageExchangeSyncRepositoryMock).findByActive(true);
		assertThat(list).hasSize(1).first().isSameAs(entity);
	}

	@Test
	void saveSyncEntity() {
		var entity = MessageExchangeSyncEntity.create();

		messageExchangeWorker.saveSyncEntity(entity);

		verify(messageExchangeSyncRepositoryMock).save(same(entity));
	}

	@Test
	void getConversation() {
		var entity = MessageExchangeSyncEntity.create()
			.withMunicipalityId("municipalityId")
			.withNamespace("namespace")
			.withLatestSyncedSequenceNumber(33L);
		var pageableMock = Mockito.mock(Pageable.class);
		var conversationPage = new PageImpl<>(List.of(new Conversation()));
		when(messageExchangeClientMock.getConversations(any(), any(), any(), any(), any())).thenReturn(ResponseEntity.ok(conversationPage));

		var result = messageExchangeWorker.getConversations(entity, pageableMock);

		verify(messageExchangeClientMock).getConversations(isNull(), eq("municipalityId"), eq("namespace"), eq("messages.sequenceNumber.id > 33"), same(pageableMock));
		assertThat(result).isSameAs(conversationPage);
	}

	@Test
	void processConversation() {
		var conversation = new Conversation();
		conversation.setExternalReferences(List.of(new KeyValues().key(RELATION_ID_KEY).addValuesItem("1").addValuesItem("2")));
		conversation.setMunicipalityId("municipalityId");
		conversation.setId("conversationId");
		var conversationEntities = new ArrayList<ConversationEntity>();
		conversationEntities.add(ConversationEntity.create()
			.withMunicipalityId("municipalityId-existing")
			.withNamespace("support-management-namespace-existing")
			.withId("existingConversationEntityId")
			.withMessageExchangeId("existingMessageExchangeId")
			.withRelationIds(List.of("1")));

		when(conversationRepositoryMock.findByMessageExchangeId(any())).thenReturn(conversationEntities);
		when(relationClientMock.getRelation(any(), any())).thenReturn(
			ResponseEntity.ok(new Relation(null, new ResourceIdentifier().resourceId("other-id"), new ResourceIdentifier().resourceId("123").service("support-management"))), // relation 1
			ResponseEntity.ok(new Relation(null, new ResourceIdentifier().resourceId("existingConversationEntityId"), new ResourceIdentifier().resourceId("other-id")))); // relation 2
		when(errandsRepositoryMock.findById("123")).thenReturn(Optional.of(ErrandEntity.create().withMunicipalityId("municipalityId").withNamespace("support-management-namespace").withId("123")));

		messageExchangeWorker.processConversation(conversation);

		verify(conversationRepositoryMock).findByMessageExchangeId("conversationId");
		verify(relationClientMock).getRelation("municipalityId", "1");
		verify(relationClientMock).getRelation("municipalityId", "2");
		verify(errandsRepositoryMock, times(2)).findById("123");
		verify(conversationServiceMock, times(2)).syncConversation(conversationEntityArgumentCaptor.capture(), same(conversation));
		assertThat(conversationEntityArgumentCaptor.getAllValues()).hasSize(2)
			.extracting(ConversationEntity::getMunicipalityId, ConversationEntity::getNamespace, ConversationEntity::getId, ConversationEntity::getMessageExchangeId)
			.containsExactly(
				tuple("municipalityId-existing", "support-management-namespace-existing", "existingConversationEntityId", "existingMessageExchangeId"),
				tuple("municipalityId", "support-management-namespace", null, "conversationId"));

		verifyNoMoreInteractions(conversationRepositoryMock, relationClientMock, errandsRepositoryMock, conversationServiceMock, messageExchangeClientMock, messageExchangeSyncRepositoryMock);
	}
}
