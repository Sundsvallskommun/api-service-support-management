package se.sundsvall.supportmanagement.service.scheduler.messageexchange;

import generated.se.sundsvall.messageexchange.Conversation;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.atIndex;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageExchangeSchedulerTest {

	@Mock
	private MessageExchangeWorker messageExchangeWorkerMock;

	@Mock
	private AsyncTaskExecutor asyncTaskExecutorMock;

	@Mock
	private Page<Conversation> pageMock;

	@Mock
	private Conversation conversationMock;

	@Mock
	private MessageExchangeSyncEntity messageExchangeSyncEntityMock;

	@Mock
	private Pageable pageableMock;

	@Captor
	private ArgumentCaptor<Pageable> pageableArgumentCaptor;

	@InjectMocks
	private MessageExchangeScheduler messageExchangeScheduler;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(messageExchangeScheduler, "self", messageExchangeScheduler);
		ReflectionTestUtils.setField(messageExchangeScheduler, "isSchedulerEnabled", true);
	}

	@Test
	@SuppressWarnings("unchecked")
	void syncConversation() {
		when(messageExchangeWorkerMock.getActiveSyncEntities()).thenReturn(List.of(messageExchangeSyncEntityMock));
		when(messageExchangeWorkerMock.getConversations(any(), any())).thenReturn(pageMock);
		when(pageMock.stream()).thenReturn(Stream.of(conversationMock), Stream.of(conversationMock));
		when(messageExchangeWorkerMock.processConversation(any())).thenReturn(conversationMock);
		when(messageExchangeSyncEntityMock.getLatestSyncedSequenceNumber()).thenReturn(0L);
		when(conversationMock.getLatestSequenceNumber()).thenReturn(2L, 4L);
		when(pageMock.hasNext()).thenReturn(true, false);
		when(pageMock.nextPageable()).thenReturn(pageableMock);

		messageExchangeScheduler.syncConversations();

		verify(messageExchangeWorkerMock).getActiveSyncEntities();
		verify(messageExchangeWorkerMock, times(2)).getConversations(eq(messageExchangeSyncEntityMock), pageableArgumentCaptor.capture());
		verify(messageExchangeWorkerMock, times(2)).processConversation(conversationMock);
		verify(conversationMock, times(2)).getLatestSequenceNumber();
		verify(messageExchangeSyncEntityMock).setLatestSyncedSequenceNumber(2L);
		verify(messageExchangeSyncEntityMock).setLatestSyncedSequenceNumber(4L);
		verify(messageExchangeWorkerMock).saveSyncEntity(messageExchangeSyncEntityMock);

		assertThat(pageableArgumentCaptor.getAllValues())
			.hasSize(2)
			.contains(pageableMock, atIndex(1))
			.first().extracting(Pageable::getPageSize).isEqualTo(100);
	}

	@Test
	@SuppressWarnings("unchecked")
	void triggerSyncConversationAsync() {

		// Mock
		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		when(messageExchangeWorkerMock.getActiveSyncEntities()).thenReturn(List.of(messageExchangeSyncEntityMock));
		when(messageExchangeWorkerMock.getConversations(any(), any())).thenReturn(pageMock);
		when(pageMock.stream()).thenReturn(Stream.of(conversationMock), Stream.of(conversationMock));
		when(messageExchangeWorkerMock.processConversation(any())).thenReturn(conversationMock);
		when(messageExchangeSyncEntityMock.getLatestSyncedSequenceNumber()).thenReturn(0L);
		when(conversationMock.getLatestSequenceNumber()).thenReturn(2L, 4L);
		when(pageMock.hasNext()).thenReturn(true, false);
		when(pageMock.nextPageable()).thenReturn(pageableMock);

		// Act on trigger
		messageExchangeScheduler.triggerSyncConversationsAsync();
		// Verify call to asyncTaskExecutorMock
		verify(asyncTaskExecutorMock).execute(runnableCaptor.capture());

		// Act by starting runnable
		runnableCaptor.getValue().run();
		// Verify that syncConversations() is the runnable being called
		verify(messageExchangeWorkerMock).getActiveSyncEntities();
		verify(messageExchangeWorkerMock, times(2)).getConversations(eq(messageExchangeSyncEntityMock), pageableArgumentCaptor.capture());
		verify(messageExchangeWorkerMock, times(2)).processConversation(conversationMock);
		verify(conversationMock, times(2)).getLatestSequenceNumber();
		verify(messageExchangeSyncEntityMock).setLatestSyncedSequenceNumber(2L);
		verify(messageExchangeSyncEntityMock).setLatestSyncedSequenceNumber(4L);
		verify(messageExchangeWorkerMock).saveSyncEntity(messageExchangeSyncEntityMock);

		assertThat(pageableArgumentCaptor.getAllValues())
			.hasSize(2)
			.contains(pageableMock, atIndex(1))
			.first().extracting(Pageable::getPageSize).isEqualTo(100);
	}
}
