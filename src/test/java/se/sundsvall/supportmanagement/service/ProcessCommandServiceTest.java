package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.Duration;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.DirectRun;
import se.sundsvall.supportmanagement.config.ProcessEngineProperties.LoopGuard;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessSignalRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessSignalEntity;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.model.ProcessCommand;
import se.sundsvall.supportmanagement.service.model.ProcessKeySelection;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.createErrandProcessEntity;
import static se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity.MESSAGE_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity.PROCESS_KEY_LENGTH;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandLifecycle.DRAFT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SIGNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStartMode.MANUAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
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
	private static final String APPLICATION = "alkt-ansokan";
	private static final String SUPERVISION = "alkt-tillsyn";
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
	private ProcessEventOutboxRepository outboxRepositoryMock;

	@Mock
	private ProcessKeySelector processKeySelectorMock;

	@Mock
	private EventService eventServiceMock;

	@Captor
	private ArgumentCaptor<ErrandProcessActivityEntity> activityCaptor;

	private ProcessCommandService service;

	private final ErrandEntity errand = ErrandEntity.create().withId(ERRAND_ID).withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID);

	@BeforeEach
	void setUp() {
		service = new ProcessCommandService(accessControlServiceMock, namespaceConfigServiceMock, processRepositoryMock, signalRepositoryMock, outboxRepositoryMock, processKeySelectorMock,
			new ProcessActivityLog(activityRepositoryMock, new ProcessEngineProperties(new LoopGuard(20, Duration.ofMinutes(10)), new DirectRun(false, 2, 4, 500)), CLOCK), eventServiceMock);

		asHandler(HANDLER);
		lenient().when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of("pw-alkt"));
		lenient().when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW)).thenReturn(errand);
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Starting by hand
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	@DisplayName("Verification that a start leaves an entry naming the sender without a process instance, and an event carrying the chosen key, and notifies no one")
	void aStartIsLoggedAndHandedOnWithItsKey() {
		givenLabels(selection(SUPERVISION, MANUAL));

		service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null);

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue()).satisfies(entry -> {
			assertThat(entry.getErrandProcessId()).isNull();
			assertThat(entry.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(entry.getActivityType()).isEqualTo("START");
			assertThat(entry.getActivityId()).isEqualTo(SUPERVISION);
			assertThat(entry.getActivityName()).isNull();
			assertThat(entry.getSeverity()).isEqualTo(INFO);
			assertThat(entry.getMessage()).isEqualTo("start of process 'alkt-tillsyn' requested by joe01doe");
			assertThat(entry.getExternalTaskId()).isNull();
			assertThat(entry.getErrorCode()).isNull();
			assertThat(entry.getOccurredAt()).isEqualTo(OffsetDateTime.parse("2026-09-21T10:15:30.123Z"));
		});
		verify(eventServiceMock).createProcessCommandEvent(UPDATE, "En start av processen har begärts i ärendet: alkt-tillsyn.", errand, EventSubType.PROCESS,
			new ProcessCommand(SUPERVISION, null));
	}

	@ParameterizedTest
	@EnumSource(ProcessStartMode.class)
	@DisplayName("Verification that a start does not read the start mode of the labels")
	void aStartIsTakenWhateverTheStartMode(final ProcessStartMode startMode) {
		givenLabels(selection(APPLICATION, startMode));

		service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, APPLICATION);

		verify(eventServiceMock).createProcessCommandEvent(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("Verification that a start after a start that failed is taken, as the recovery it is")
	void aStartAfterAFailedStartIsTaken() {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(instance(FAILED, APPLICATION)));
		givenLabels(selection(APPLICATION, AUTOMATIC));

		service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null);

		verify(eventServiceMock).createProcessCommandEvent(UPDATE, "En start av processen har begärts i ärendet: alkt-ansokan.", errand, EventSubType.PROCESS,
			new ProcessCommand(APPLICATION, null));
	}

	@Test
	void aStartEntryNamingAnOversizedSenderIsCutToFitItsColumn() {
		asHandler("x".repeat(MESSAGE_LENGTH * 2));
		givenLabels(selection(APPLICATION, MANUAL));

		service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null);

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue().getMessage()).hasSize(MESSAGE_LENGTH).startsWith("start of process 'alkt-ansokan' requested by xxx");
	}

	@Test
	@DisplayName("Verification that a start from a caller that is not an ad account is refused with 403 before anything is read or written")
	void aStartFromAMachineIsRefused() {
		Identifier.set(Identifier.create().withType(Identifier.Type.CUSTOM).withTypeString("processEngine").withValue("pw-alkt"));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, APPLICATION))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(403);
				assertThat(problem.getDetail()).contains("ad account");
			});

		verifyNothingWritten();
		verifyNoInteractions(accessControlServiceMock, processRepositoryMock, processKeySelectorMock, outboxRepositoryMock);
	}

	@Test
	void aStartWithoutAnyIdentityIsRefused() {
		Identifier.remove();

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, APPLICATION))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(403));

		verifyNothingWritten();
	}

	@Test
	void aStartOfAnErrandThatDoesNotExistIsNotFound() {
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW)).thenThrow(Problem.valueOf(NOT_FOUND, "not found"));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, APPLICATION))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(404));

		verifyNothingWritten();
		verifyNoInteractions(processRepositoryMock);
	}

	@Test
	void aStartOfADraftIsAConflict() {
		errand.setLifecycle(DRAFT);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, APPLICATION))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).isEqualTo("The errand 'errandId' is a draft, and a process is started only for an active errand. Make the errand active first");
			});

		verifyNothingWritten();
		verifyNoInteractions(processKeySelectorMock);
	}

	@Test
	void aStartInANamespaceRunningNoProcessIsRejected() {
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, APPLICATION))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).contains(NAMESPACE);
			});

		verifyNothingWritten();
	}

	@ParameterizedTest
	@EnumSource(value = ProcessStatus.class, names = {
		"RUNNING", "WAITING", "RETRYING"
	})
	void aStartOfAnErrandWithALiveProcessIsAConflict(final ProcessStatus status) {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(instance(status, APPLICATION)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, APPLICATION))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains("live process");
			});

		verifyNothingWritten();
	}

	@Test
	void aStartOfAnErrandWhoseProcessRanToItsEndIsAConflict() {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(instance(COMPLETED, APPLICATION)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains("ran to its end");
			});

		verifyNothingWritten();
	}

	@Test
	void aStartOfAnErrandWithoutAProcessKeyIsRejected() {
		givenLabels(ProcessKeySelection.NONE);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).contains("No label of the errand");
			});

		verifyNothingWritten();
	}

	@Test
	void aStartOfAnErrandWhoseLabelsNoLongerNameItsProcessIsRejectedNamingThatProcess() {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(instance(FAILED, APPLICATION)));
		givenLabels(selection(SUPERVISION, AUTOMATIC));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, SUPERVISION))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).contains(APPLICATION);
			});

		verifyNothingWritten();
	}

	@Test
	@DisplayName("Verification that a start of an errand whose labels point in two directions has to name the key, and names both when it does not")
	void aStartOfAnAmbiguousErrandHasToChoose() {
		givenLabels(ambiguous(APPLICATION, SUPERVISION));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).contains(APPLICATION, SUPERVISION);
			});

		verifyNothingWritten();
	}

	@Test
	@DisplayName("Verification that the key chosen among those of an ambiguous errand is the one handed on, rather than resolved again from the labels")
	void theChosenKeyOfAnAmbiguousErrandIsHandedOn() {
		givenLabels(ambiguous(APPLICATION, SUPERVISION));

		service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, SUPERVISION);

		verify(eventServiceMock).createProcessCommandEvent(UPDATE, "En start av processen har begärts i ärendet: alkt-tillsyn.", errand, EventSubType.PROCESS,
			new ProcessCommand(SUPERVISION, null));
	}

	@Test
	void aStartNamingAKeyTheLabelsDoNotOfferIsRejected() {
		givenLabels(selection(APPLICATION, MANUAL));

		for (final var requested : List.of(SUPERVISION, "Alkt-Ansokan", " alkt-ansokan")) {
			assertThatExceptionOfType(ThrowableProblem.class)
				.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requested))
				.satisfies(problem -> {
					assertThat(problem.getStatus().value()).isEqualTo(400);
					assertThat(problem.getDetail()).contains("is not one the errand");
				});
		}

		verifyNothingWritten();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"", " ", "\t"
	})
	@DisplayName("Verification that a blank key is no key named, so the one process the labels offer is started")
	void aBlankKeyStartsTheOnlyProcessOffered(final String blank) {
		givenLabels(selection(APPLICATION, MANUAL));

		service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, blank);

		verify(eventServiceMock).createProcessCommandEvent(UPDATE, "En start av processen har begärts i ärendet: alkt-ansokan.", errand, EventSubType.PROCESS,
			new ProcessCommand(APPLICATION, null));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"", " "
	})
	@DisplayName("Verification that a blank key on an errand whose labels offer several processes chooses none of them")
	void aBlankKeyOnAnAmbiguousErrandChoosesNothing(final String blank) {
		givenLabels(ambiguous(APPLICATION, SUPERVISION));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, blank))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).contains("has to name the one to start");
			});

		verifyNothingWritten();
	}

	@Test
	@DisplayName("Verification that a label carrying a key longer than a process key may be offers nothing to start, and the start is rejected without repeating the key")
	void aStartWhoseLabelCarriesAnOversizedKeyIsRejected() {
		final var oversized = "k".repeat(PROCESS_KEY_LENGTH + 1);
		givenLabels(selection(oversized, MANUAL));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).contains("carries a process key it can be started with").doesNotContain(oversized);
			});

		verifyNothingWritten();
	}

	/**
	 * The start on its way may be an earlier press or, in automatic mode, an errand event not yet delivered. Either way the
	 * press is recorded, and the process is handed no second start.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"PROCESS", "ERRAND"
	})
	@DisplayName("Verification that a press while a start with the same key is on its way is recorded with its entry and its event, but not published")
	void aPressWhileAStartWithTheSameKeyIsOnItsWayIsRecordedButNotPublished(final String subTypeOnItsWay) {
		givenLabels(selection(APPLICATION, AUTOMATIC));
		when(outboxRepositoryMock.findByErrandIdAndStartAllowedIsTrueAndDeliveredAtIsNull(ERRAND_ID))
			.thenReturn(List.of(waitingStart(APPLICATION).withEventSubType(subTypeOnItsWay)));

		service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null);

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue()).satisfies(entry -> {
			assertThat(entry.getActivityType()).isEqualTo("START");
			assertThat(entry.getActivityId()).isEqualTo(APPLICATION);
			assertThat(entry.getMessage()).isEqualTo("start of process 'alkt-ansokan' requested by joe01doe");
		});
		verify(eventServiceMock).createProcessCommandEvent(UPDATE, "En start av processen har begärts i ärendet: alkt-ansokan.", errand, EventSubType.PROCESS, null);
		verifyNoMoreInteractions(eventServiceMock);
	}

	@Test
	@DisplayName("Verification that a start while one of another process is on its way is a conflict that writes nothing")
	void aPressWithAnotherKeyWhileAStartIsOnItsWayIsAConflict() {
		givenLabels(ambiguous(APPLICATION, SUPERVISION));
		when(outboxRepositoryMock.findByErrandIdAndStartAllowedIsTrueAndDeliveredAtIsNull(ERRAND_ID)).thenReturn(List.of(waitingStart(APPLICATION)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, SUPERVISION))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains(APPLICATION, SUPERVISION);
			});

		verifyNothingWritten();
	}

	@Test
	@DisplayName("Verification that the errand is locked before anything else is read for a start")
	void theErrandIsLockedBeforeAStartReadsAnything() {
		givenLabels(selection(APPLICATION, MANUAL));

		service.startProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null);

		final var order = inOrder(accessControlServiceMock, namespaceConfigServiceMock, processRepositoryMock, processKeySelectorMock, outboxRepositoryMock);
		order.verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW);
		order.verify(namespaceConfigServiceMock).getProcessConsumer(NAMESPACE, MUNICIPALITY_ID);
		order.verify(processRepositoryMock).findByErrandIdOrderByCreatedDesc(ERRAND_ID);
		order.verify(processKeySelectorMock).select(errand);
		order.verify(outboxRepositoryMock).findByErrandIdAndStartAllowedIsTrueAndDeliveredAtIsNull(ERRAND_ID);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Signals
	// ---------------------------------------------------------------------------------------------------------------

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
		verify(eventServiceMock).createProcessCommandEvent(UPDATE, "En signal har skickats till processen i ärendet: Godkänn granskning.", errand, SIGNAL, new ProcessCommand(null, SIGNAL_NAME));
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW);
	}

	@Test
	void aSignalWithoutALabelIsNamedByItsNameInTheEventLog() {
		givenInstance(WAITING, awaited(SIGNAL_NAME, null));

		service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME);

		verify(eventServiceMock).createProcessCommandEvent(UPDATE, "En signal har skickats till processen i ärendet: granskning-godkand.", errand, SIGNAL, new ProcessCommand(null, SIGNAL_NAME));
	}

	@Test
	void anEntryNamingAnOversizedSenderIsCutToFitItsColumn() {
		asHandler("x".repeat(MESSAGE_LENGTH * 2));
		givenInstance(WAITING, awaited(SIGNAL_NAME, SIGNAL_LABEL));

		service.signalProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, SIGNAL_NAME);

		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue().getMessage()).hasSize(MESSAGE_LENGTH).startsWith("signal 'granskning-godkand' sent by xxx");
	}

	/**
	 * The refusal comes before anything is looked at, let alone written.
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

		verify(eventServiceMock).createProcessCommandEvent(any(), any(), any(), any(), any());
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
	 * A signal that differs from an awaited one only in case is refused with 409 and writes nothing.
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

	private void givenLabels(final ProcessKeySelection selection) {
		when(processKeySelectorMock.select(errand)).thenReturn(selection);
	}

	private static ErrandProcessEntity instance(final ProcessStatus status, final String processKey) {
		return createErrandProcessEntity(status, CLOCK, process -> process
			.withId(PROCESS_ROW_ID)
			.withErrandId(ERRAND_ID)
			.withProcessKey(processKey)
			.withProcessInstanceId(PROCESS_INSTANCE_ID));
	}

	private static ProcessKeySelection selection(final String processKey, final ProcessStartMode startMode) {
		return new ProcessKeySelection(processKey, startMode, List.of(processKey));
	}

	private static ProcessKeySelection ambiguous(final String... processKeys) {
		return new ProcessKeySelection(null, null, List.of(processKeys));
	}

	private static ProcessEventOutboxEntity waitingStart(final String processKey) {
		return ProcessEventOutboxEntity.create().withErrandId(ERRAND_ID).withProcessKey(processKey).withStartAllowed(true);
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
