package se.sundsvall.supportmanagement.service.scheduler.teliaace;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import se.sundsvall.dept44.exception.ClientProblem;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.TeliaAceContactRepository;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceContactEntity;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;
import se.sundsvall.supportmanagement.integration.teliaace.AddWorkItemRequest;
import se.sundsvall.supportmanagement.integration.teliaace.AddWorkItemResponse;
import se.sundsvall.supportmanagement.integration.teliaace.TeliaAceClient;
import se.sundsvall.supportmanagement.integration.teliaace.UpdateContactDataRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeliaAceWorkItemWorkerTest {

	private static final String ERRAND_ID = "errand-1";
	private static final String EMAIL_UID = "42.CallGuideWorkItem@computer";

	@Mock
	private TeliaAceWorkItemRepository workItemRepositoryMock;

	@Mock
	private TeliaAceContactRepository contactRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private TeliaAceClient teliaAceClientMock;

	@InjectMocks
	private TeliaAceWorkItemWorker worker;

	@Captor
	private ArgumentCaptor<AddWorkItemRequest> addRequestCaptor;

	@Captor
	private ArgumentCaptor<UpdateContactDataRequest> updateRequestCaptor;

	@Captor
	private ArgumentCaptor<TeliaAceContactEntity> contactCaptor;

	private static TeliaAceContactEntity contact() {
		return TeliaAceContactEntity.create().withErrandId(ERRAND_ID).withEmailUid(EMAIL_UID);
	}

	private void errandExists() {
		when(errandsRepositoryMock.existsById(ERRAND_ID)).thenReturn(true);
	}

	@Test
	void fetchProcessable_delegatesToRepository() {
		final var entity = TeliaAceWorkItemEntity.create().withId("id-1");
		when(workItemRepositoryMock.findTop6ByOrderByCreatedAsc()).thenReturn(List.of(entity));

		final var result = worker.fetchProcessable();

		assertThat(result).containsExactly(entity);
	}

	@Test
	void process_whenErrandNoLongerExists_dropsWorkItemWithoutCallingClient() {
		final var workItem = TeliaAceWorkItemEntity.create().withId("id-1").withErrandId(ERRAND_ID);
		when(errandsRepositoryMock.existsById(ERRAND_ID)).thenReturn(false);

		worker.process(workItem);

		verifyNoInteractions(teliaAceClientMock, contactRepositoryMock);
		verify(workItemRepositoryMock).delete(workItem);
	}

	@Test
	void process_withNoContactYet_addsWorkItemAndSavesTheReturnedEmailUid() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withErrandId(ERRAND_ID)
			.withFromAddress("anna.andersson@example.com")
			.withSubject("Nytt ärende i Draken")
			.withContentUrl("https://draken.sundsvall.se/kontaktsundsvall/arende/KS-123456");
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());
		when(teliaAceClientMock.addWorkItem(any())).thenReturn(new AddWorkItemResponse("request-1", "42", EMAIL_UID));

		worker.process(workItem);

		verify(teliaAceClientMock).addWorkItem(addRequestCaptor.capture());
		final var request = addRequestCaptor.getValue();
		assertThat(request.from()).isEqualTo("anna.andersson@example.com");
		assertThat(request.subject()).isEqualTo("Nytt ärende i Draken");
		assertThat(request.entrance()).isEqualTo("wiDraken");
		assertThat(request.errand()).isEqualTo("w01");
		assertThat(request.workitem()).isEqualTo("Öppna ärendet i Draken");
		assertThat(request.workitemSource()).isEqualTo("draken");
		assertThat(request.contentUrl()).isEqualTo("https://draken.sundsvall.se/kontaktsundsvall/arende/KS-123456");
		assertThat(request.customKeys()).isNull();

		verify(contactRepositoryMock).save(contactCaptor.capture());
		assertThat(contactCaptor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(contactCaptor.getValue().getEmailUid()).isEqualTo(EMAIL_UID);

		verify(workItemRepositoryMock).delete(workItem);
	}

	@Test
	void process_withNoContactYetAndAssignedAgent_sendsPredefinedAgentNameAsCustomKey() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withErrandId(ERRAND_ID)
			.withFromAddress("bertil.bengtsson@example.com")
			.withPredefinedAgentName("jep11jep");
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());
		when(teliaAceClientMock.addWorkItem(any())).thenReturn(new AddWorkItemResponse("request-1", "42", EMAIL_UID));

		worker.process(workItem);

		verify(teliaAceClientMock).addWorkItem(addRequestCaptor.capture());
		assertThat(addRequestCaptor.getValue().customKeys()).isEqualTo(Map.of("predefTargetAgentName", "jep11jep"));
	}

	@Test
	void process_whenAddWorkItemReturnsBlankEmailUid_dropsItemWithoutSavingAnything() {
		final var workItem = TeliaAceWorkItemEntity.create().withId("id-1").withErrandId(ERRAND_ID);
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());
		when(teliaAceClientMock.addWorkItem(any())).thenReturn(new AddWorkItemResponse("request-1", "42", " "));

		worker.process(workItem);

		verify(contactRepositoryMock, never()).save(any());
		verify(workItemRepositoryMock).delete(workItem);
	}

	@Test
	void process_whenAddWorkItemReturnsNullResponse_dropsItemWithoutSavingAnything() {
		final var workItem = TeliaAceWorkItemEntity.create().withId("id-1").withErrandId(ERRAND_ID);
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());
		when(teliaAceClientMock.addWorkItem(any())).thenReturn(null);

		worker.process(workItem);

		verify(contactRepositoryMock, never()).save(any());
		verify(workItemRepositoryMock).delete(workItem);
	}

	@Test
	void process_whenContactAlreadyExists_updatesContactDataInsteadOfAddingANewWorkItem() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withErrandId(ERRAND_ID)
			.withFromAddress("anna.andersson@example.com");
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(contact()));

		worker.process(workItem);

		verify(teliaAceClientMock).updateContactData(same(EMAIL_UID), updateRequestCaptor.capture());
		assertThat(updateRequestCaptor.getValue().customKeys())
			.isEqualTo(Map.of("workItem", "Nytt meddelande mottaget från anna.andersson@example.com. Öppna ärendet i Draken"));
		verify(teliaAceClientMock, never()).addWorkItem(any());
		verify(contactRepositoryMock, never()).save(any());
		verify(workItemRepositoryMock).delete(workItem);
	}

	@Test
	void process_whenContactAlreadyExistsAndNoFromAddress_usesUnknownSenderNote() {
		final var workItem = TeliaAceWorkItemEntity.create().withId("id-1").withErrandId(ERRAND_ID);
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(contact()));

		worker.process(workItem);

		verify(teliaAceClientMock).updateContactData(any(), updateRequestCaptor.capture());
		assertThat(updateRequestCaptor.getValue().customKeys())
			.isEqualTo(Map.of("workItem", "Nytt meddelande mottaget från okänd avsändare. Öppna ärendet i Draken"));
	}

	@Test
	void process_whenContactAlreadyExistsAndAssignedAgent_keepsPredefinedAgentNameAlongsideTheNote() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withErrandId(ERRAND_ID)
			.withPredefinedAgentName("jep11jep");
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(contact()));

		worker.process(workItem);

		verify(teliaAceClientMock).updateContactData(any(), updateRequestCaptor.capture());
		assertThat(updateRequestCaptor.getValue().customKeys()).containsEntry("predefTargetAgentName", "jep11jep");
	}

	@Test
	void process_whenUpdateContactDataRejectedAsClientProblem_clearsContactAndAddsAFreshWorkItem() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withErrandId(ERRAND_ID)
			.withFromAddress("anna.andersson@example.com");
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(contact()));
		doThrow(new ClientProblem(HttpStatus.NOT_FOUND, "unknown emailUid")).when(teliaAceClientMock).updateContactData(any(), any());
		when(teliaAceClientMock.addWorkItem(any())).thenReturn(new AddWorkItemResponse("request-2", "43", "43.CallGuideWorkItem@computer"));

		worker.process(workItem);

		verify(contactRepositoryMock).delete(contact());
		verify(teliaAceClientMock).addWorkItem(any());
		verify(contactRepositoryMock).save(contactCaptor.capture());
		assertThat(contactCaptor.getValue().getEmailUid()).isEqualTo("43.CallGuideWorkItem@computer");
		verify(workItemRepositoryMock).delete(workItem);
	}

	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {
		"UNAUTHORIZED", "FORBIDDEN", "TOO_MANY_REQUESTS"
	})
	void process_whenUpdateContactDataRejectedWithAuthOrRateLimitStatus_propagatesWithoutClearingContact(final HttpStatus status) {
		final var workItem = TeliaAceWorkItemEntity.create().withId("id-1").withErrandId(ERRAND_ID);
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(contact()));
		doThrow(new ClientProblem(status, "rejected")).when(teliaAceClientMock).updateContactData(any(), any());

		assertThatThrownBy(() -> worker.process(workItem)).isInstanceOf(ClientProblem.class);

		verify(contactRepositoryMock, never()).delete(any());
		verify(teliaAceClientMock, never()).addWorkItem(any());
		verify(workItemRepositoryMock, never()).delete(any());
	}

	@Test
	void process_whenUpdateContactDataThrowsOtherException_keepsContactAndWorkItem() {
		final var workItem = TeliaAceWorkItemEntity.create().withId("id-1").withErrandId(ERRAND_ID);
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(contact()));
		doThrow(new ServerProblem(HttpStatus.BAD_GATEWAY, "ACE unavailable")).when(teliaAceClientMock).updateContactData(any(), any());

		assertThatThrownBy(() -> worker.process(workItem)).isInstanceOf(ServerProblem.class);

		verify(contactRepositoryMock, never()).delete(any());
		verify(teliaAceClientMock, never()).addWorkItem(any());
		verify(workItemRepositoryMock, never()).delete(any());
	}

	@Test
	void process_whenAddWorkItemThrowsClientProblem_dropsWorkItemWithoutRethrowing() {
		final var workItem = TeliaAceWorkItemEntity.create().withId("id-1").withErrandId(ERRAND_ID);
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());
		doThrow(new ClientProblem(HttpStatus.BAD_REQUEST, "bad request")).when(teliaAceClientMock).addWorkItem(any());

		worker.process(workItem);

		verify(contactRepositoryMock, never()).save(any());
		verify(workItemRepositoryMock).delete(workItem);
	}

	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {
		"UNAUTHORIZED", "FORBIDDEN", "TOO_MANY_REQUESTS"
	})
	void process_whenAddWorkItemRejectedWithAuthOrRateLimitStatus_propagatesWithoutDroppingWorkItem(final HttpStatus status) {
		final var workItem = TeliaAceWorkItemEntity.create().withId("id-1").withErrandId(ERRAND_ID);
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());
		doThrow(new ClientProblem(status, "rejected")).when(teliaAceClientMock).addWorkItem(any());

		assertThatThrownBy(() -> worker.process(workItem)).isInstanceOf(ClientProblem.class);

		verify(workItemRepositoryMock, never()).delete(any());
		verify(contactRepositoryMock, never()).save(any());
	}

	@Test
	void process_whenAddWorkItemThrowsOtherException_doesNotDeleteWorkItem() {
		final var workItem = TeliaAceWorkItemEntity.create().withId("id-1").withErrandId(ERRAND_ID);
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());
		doThrow(new RuntimeException("ACE unavailable")).when(teliaAceClientMock).addWorkItem(any());

		assertThatThrownBy(() -> worker.process(workItem))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("ACE unavailable");

		verify(workItemRepositoryMock, never()).delete(any());
	}

	@Test
	void process_whenWorkItemExceedsMaxAge_dropsWithoutCallingClientOrRepositories() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withCreated(OffsetDateTime.now(ZoneId.systemDefault()).minusDays(10));

		worker.process(workItem);

		verifyNoInteractions(teliaAceClientMock, errandsRepositoryMock, contactRepositoryMock);
		verify(workItemRepositoryMock).delete(workItem);
	}

	@Test
	void process_whenWorkItemIsWithinMaxAge_stillCallsClient() {
		final var workItem = TeliaAceWorkItemEntity.create()
			.withId("id-1")
			.withErrandId(ERRAND_ID)
			.withCreated(OffsetDateTime.now(ZoneId.systemDefault()).minusHours(1));
		errandExists();
		when(contactRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());
		when(teliaAceClientMock.addWorkItem(any())).thenReturn(new AddWorkItemResponse("request-1", "42", EMAIL_UID));

		worker.process(workItem);

		verify(teliaAceClientMock).addWorkItem(any());
		verify(workItemRepositoryMock).delete(workItem);
	}
}
