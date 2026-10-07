package se.sundsvall.supportmanagement.service.scheduler.teliaace;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;
import se.sundsvall.supportmanagement.integration.teliaace.AddWorkItemRequest;
import se.sundsvall.supportmanagement.integration.teliaace.TeliaAceClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeliaAceWorkItemWorkerTest {

	@Mock
	private TeliaAceWorkItemRepository workItemRepositoryMock;

	@Mock
	private TeliaAceClient teliaAceClientMock;

	@InjectMocks
	private TeliaAceWorkItemWorker worker;

	@Captor
	private ArgumentCaptor<AddWorkItemRequest> requestCaptor;

	@Test
	void fetchProcessable_delegatesToRepository() {
		final var entity = TeliaAceWorkItemEntity.create().withId("id-1");
		when(workItemRepositoryMock.findAllByOrderByCreatedAsc()).thenReturn(List.of(entity));

		final var result = worker.fetchProcessable();

		assertThat(result).containsExactly(entity);
	}

	@Test
	void process_withoutAssignedAgent_sendsRequestWithoutCustomKeysAndDeletes() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withFromAddress("anna.andersson@example.com")
			.withSubject("Nytt ärende i Draken")
			.withContentUrl("https://draken.sundsvall.se/kontaktsundsvall/arende/KS-123456");

		worker.process(workItem);

		verify(teliaAceClientMock).addWorkItem(requestCaptor.capture());
		final var request = requestCaptor.getValue();
		assertThat(request.from()).isEqualTo("anna.andersson@example.com");
		assertThat(request.subject()).isEqualTo("Nytt ärende i Draken");
		assertThat(request.entrance()).isEqualTo("Draken");
		assertThat(request.errand()).isEqualTo("w01");
		assertThat(request.workitem()).isEqualTo("Öppna ärendet i Draken");
		assertThat(request.contentUrl()).isEqualTo("https://draken.sundsvall.se/kontaktsundsvall/arende/KS-123456");
		assertThat(request.customKeys()).isEmpty();
		verify(workItemRepositoryMock).delete(workItem);
	}

	@Test
	void process_withAssignedAgent_sendsPredefinedAgentNameAsCustomKey() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withPredefinedAgentName("jep11jep");

		worker.process(workItem);

		verify(teliaAceClientMock).addWorkItem(requestCaptor.capture());
		assertThat(requestCaptor.getValue().customKeys()).containsExactly(java.util.Map.entry("predefinedAgentName", "jep11jep"));
		verify(workItemRepositoryMock).delete(workItem);
	}

	@Test
	void process_whenClientThrows_doesNotDeleteWorkItem() {
		final var workItem = TeliaAceWorkItemEntity.create().withId("id-1");
		doThrow(new RuntimeException("ACE unavailable")).when(teliaAceClientMock).addWorkItem(any());

		assertThatThrownBy(() -> worker.process(workItem))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("ACE unavailable");

		verify(workItemRepositoryMock, never()).delete(any());
	}

	@Test
	void process_whenWorkItemExceedsMaxAge_dropsWithoutCallingClient() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withCreated(OffsetDateTime.now(ZoneId.systemDefault()).minusDays(10));

		worker.process(workItem);

		verifyNoInteractions(teliaAceClientMock);
		verify(workItemRepositoryMock).delete(workItem);
	}

	@Test
	void process_whenWorkItemIsWithinMaxAge_stillCallsClient() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withCreated(OffsetDateTime.now(ZoneId.systemDefault()).minusHours(1));

		worker.process(workItem);

		verify(teliaAceClientMock).addWorkItem(any());
		verify(workItemRepositoryMock).delete(workItem);
	}
}
