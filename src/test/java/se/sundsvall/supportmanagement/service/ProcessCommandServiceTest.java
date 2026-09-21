package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessSignalRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessSignalEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.model.ProcessCommand;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity.MESSAGE_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SIGNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.PROCESS;

@ExtendWith(MockitoExtension.class)
class ProcessCommandServiceTest {

	private static final String NAMESPACE = "NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "errandId";
	private static final String PROCESS_INSTANCE_ID = "8f1c2b6e";
	private static final String PROCESS_ROW_ID = "rowId";
	private static final String SIGNAL_NAME = "granskning-godkand";
	private static final String SIGNAL_LABEL = "Godkänn granskning";
	private static final String HANDLER = "joe01doe";
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T10:15:30.123456Z"), ZoneId.of("UTC"));

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	@Mock
	private ErrandProcessRepository processRepositoryMock;

	@Mock
	private ErrandProcessSignalRepository signalRepositoryMock;

	@Mock
	private ErrandProcessActivityRepository activityRepositoryMock;

	@Mock
	private EventService eventServiceMock;

	@Captor
	private ArgumentCaptor<ErrandProcessActivityEntity> activityCaptor;

	private ProcessCommandService service;

	private final ErrandEntity errand = ErrandEntity.create().withId(ERRAND_ID).withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID);

	@BeforeEach
	void setUp() {
		service = new ProcessCommandService(accessControlServiceMock, namespaceConfigServiceMock, processRepositoryMock, signalRepositoryMock, activityRepositoryMock, eventServiceMock, CLOCK);

		asHandler(HANDLER);
		lenient().when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of("pw-alkt"));
		lenient().when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW)).thenReturn(errand);
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	@Test
	@DisplayName("Verification that an awaited signal leaves an entry naming the sender and an event carrying the name of the signal, and notifies no one")
	void anAwaitedSignalIsLoggedAndHandedOnToTheProcess() {
		givenInstance(WAITING, awaited(SIGNAL_NAME, SIGNAL_LABEL), awaited("granskning-avvisad", "Skicka tillbaka"));

		service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME);

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue()).satisfies(entry -> {
			assertThat(entry.getErrandProcessId()).isEqualTo(PROCESS_ROW_ID);
			assertThat(entry.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(entry.getActivityType()).isEqualTo("SIGNAL");
			assertThat(entry.getActivityId()).isEqualTo(SIGNAL_NAME);
			assertThat(entry.getActivityName()).isEqualTo(SIGNAL_LABEL);
			assertThat(entry.getSeverity()).isEqualTo(INFO);
			assertThat(entry.getMessage()).isEqualTo("signal 'granskning-godkand' sent by joe01doe");
			assertThat(entry.getExternalTaskId()).isNull();
			assertThat(entry.getErrorCode()).isNull();
			assertThat(entry.getOccurredAt()).isEqualTo(OffsetDateTime.parse("2026-09-21T10:15:30.123Z"));
		});
		verify(eventServiceMock).createProcessCommandEvent(UPDATE, "En signal har skickats till processen i ärendet: Godkänn granskning.", errand, false, SIGNAL, new ProcessCommand(null, SIGNAL_NAME));
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW);
	}

	@Test
	void aSignalWithoutALabelIsNamedByItsNameInTheEventLog() {
		givenInstance(WAITING, awaited(SIGNAL_NAME, null));

		service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME);

		verify(eventServiceMock).createProcessCommandEvent(UPDATE, "En signal har skickats till processen i ärendet: granskning-godkand.", errand, false, SIGNAL, new ProcessCommand(null, SIGNAL_NAME));
	}

	/**
	 * The identity header is set by the caller, and an entry that fails to insert would take the signal down with it.
	 */
	@Test
	void anEntryNamingAnOversizedSenderIsCutToFitItsColumn() {
		asHandler("x".repeat(MESSAGE_LENGTH * 2));
		givenInstance(WAITING, awaited(SIGNAL_NAME, SIGNAL_LABEL));

		service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME);

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue().getMessage()).hasSize(MESSAGE_LENGTH).startsWith("signal 'granskning-godkand' sent by xxx");
	}

	/**
	 * The entry a signal leaves has to say which person stepped the process on. The refusal comes before anything is
	 * looked at, let alone written.
	 */
	@Test
	@DisplayName("Verification that a signal from a caller that is not an ad account is refused with 403 and writes nothing")
	void aSignalFromAMachineIsRefused() {
		Identifier.set(Identifier.create().withType(Identifier.Type.CUSTOM).withTypeString("processEngine").withValue("pw-alkt"));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(403);
				assertThat(problem.getDetail()).contains("ad account");
			});

		verifyNothingWritten();
		verifyNoInteractions(accessControlServiceMock, processRepositoryMock, signalRepositoryMock);
	}

	@Test
	void aSignalWithoutAnyIdentityIsRefused() {
		Identifier.remove();

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(403));

		verifyNothingWritten();
	}

	/**
	 * Publication writes nothing for a namespace running no process, so a signal taken there would be a button that seems
	 * to work and moves nothing.
	 */
	@Test
	void aSignalInANamespaceRunningNoProcessIsRejected() {
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).contains(NAMESPACE);
			});

		verifyNothingWritten();
		verifyNoInteractions(processRepositoryMock, signalRepositoryMock);
	}

	/**
	 * Under repeatable read the snapshot of a transaction is taken at its first plain read. Were the configuration read
	 * ahead of the lock, the signal would be judged against the signals as they stood before the report the lock waited
	 * for - and be taken for a gate that report had just closed.
	 */
	@Test
	@DisplayName("Verification that the errand is locked before anything else is read")
	void theErrandIsLockedBeforeAnythingIsRead() {
		givenInstance(WAITING, awaited(SIGNAL_NAME, SIGNAL_LABEL));

		service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME);

		final var order = inOrder(accessControlServiceMock, namespaceConfigServiceMock, processRepositoryMock, signalRepositoryMock);
		order.verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW);
		order.verify(namespaceConfigServiceMock).getProcessConsumer(NAMESPACE, MUNICIPALITY_ID);
		order.verify(processRepositoryMock).findByProcessInstanceIdAndErrandId(PROCESS_INSTANCE_ID, ERRAND_ID);
		order.verify(signalRepositoryMock).findByErrandProcessIdOrderBySortOrderAsc(PROCESS_ROW_ID);
	}

	@Test
	void aSignalToAnErrandThatDoesNotExistIsNotFound() {
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW)).thenThrow(Problem.valueOf(NOT_FOUND, "not found"));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(404));

		verifyNothingWritten();
		verifyNoInteractions(processRepositoryMock);
	}

	@Test
	void aSignalToAnInstanceTheErrandDoesNotHaveIsNotFound() {
		when(processRepositoryMock.findByProcessInstanceIdAndErrandId(PROCESS_INSTANCE_ID, ERRAND_ID)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(404);
				assertThat(problem.getDetail()).contains(PROCESS_INSTANCE_ID);
			});

		verifyNothingWritten();
		verifyNoInteractions(signalRepositoryMock);
	}

	@ParameterizedTest
	@EnumSource(value = ProcessStatus.class, names = {
		"COMPLETED", "FAILED"
	})
	void aSignalToAProcessThatHasEndedIsAConflict(final ProcessStatus status) {
		givenInstance(status);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains("has ended");
			});

		verifyNothingWritten();
		verifyNoInteractions(signalRepositoryMock);
	}

	@ParameterizedTest
	@EnumSource(value = ProcessStatus.class, names = {
		"RUNNING", "WAITING", "RETRYING"
	})
	void aLiveProcessTakesTheSignalsItWaitsFor(final ProcessStatus status) {
		givenInstance(status, awaited(SIGNAL_NAME, SIGNAL_LABEL));

		service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME);

		verify(eventServiceMock).createProcessCommandEvent(any(), any(), any(), anyBoolean(), any(), any());
	}

	/**
	 * A process that has reported that it moved on waits for something else, and the old button names a signal no longer
	 * among them.
	 */
	@Test
	@DisplayName("Verification that a signal the process does not wait for is a conflict and writes nothing")
	void aSignalTheProcessDoesNotWaitForIsAConflict() {
		givenInstance(WAITING, awaited("beslut-fattat", "Beslut fattat"));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains(SIGNAL_NAME);
			});

		verifyNothingWritten();
	}

	/**
	 * The process engine tells message names apart by case, so a name matched regardless of it would reach the process as
	 * a name no gate listens for, and be lost there without a trace.
	 */
	@Test
	void aSignalIsMatchedExactlyAsTheProcessNamedIt() {
		givenInstance(WAITING, awaited(SIGNAL_NAME, SIGNAL_LABEL));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, "Granskning-Godkand"))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));

		verifyNothingWritten();
	}

	private void givenInstance(final ProcessStatus status, final ErrandProcessSignalEntity... awaited) {
		final var instance = ErrandProcessEntity.create()
			.withId(PROCESS_ROW_ID)
			.withErrandId(ERRAND_ID)
			.withProcessInstanceId(PROCESS_INSTANCE_ID);
		instance.applyStatus(status, CLOCK);

		when(processRepositoryMock.findByProcessInstanceIdAndErrandId(PROCESS_INSTANCE_ID, ERRAND_ID)).thenReturn(Optional.of(instance));
		lenient().when(signalRepositoryMock.findByErrandProcessIdOrderBySortOrderAsc(PROCESS_ROW_ID)).thenReturn(List.of(awaited));
	}

	private static ErrandProcessSignalEntity awaited(final String name, final String label) {
		return ErrandProcessSignalEntity.create().withErrandProcessId(PROCESS_ROW_ID).withName(name).withLabel(label);
	}

	private static void asHandler(final String adAccount) {
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(adAccount));
	}

	private void verifyNothingWritten() {
		verify(activityRepositoryMock, never()).save(any());
		verifyNoInteractions(eventServiceMock);
	}
}
