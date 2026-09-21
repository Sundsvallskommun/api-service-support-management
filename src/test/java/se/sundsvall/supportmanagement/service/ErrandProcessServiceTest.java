package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.api.model.process.ProcessError;
import se.sundsvall.supportmanagement.api.model.process.ProcessSignal;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessSignalRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessSignalEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.WARN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RETRYING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.PROCESS_ACTIVITY;

@ExtendWith(MockitoExtension.class)
class ErrandProcessServiceTest {

	private static final String NAMESPACE = "NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "errandId";
	private static final String PROCESS_INSTANCE_ID = "8f1c2b6e";
	private static final String PROCESS_KEY = "alkt-ansokan";
	private static final String PROCESS_SERVICE = "pw-alkt";
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T10:15:30.000Z"), ZoneId.of("UTC"));

	@Mock
	private ErrandProcessRepository processRepositoryMock;

	@Mock
	private ErrandProcessActivityRepository activityRepositoryMock;

	@Mock
	private ErrandProcessSignalRepository signalRepositoryMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	@Mock
	private PlatformTransactionManager transactionManagerMock;

	@Captor
	private ArgumentCaptor<ErrandProcessEntity> entityCaptor;

	@Captor
	private ArgumentCaptor<List<ErrandProcessActivityEntity>> activitiesCaptor;

	@Captor
	private ArgumentCaptor<ErrandProcessActivityEntity> activityCaptor;

	@Captor
	private ArgumentCaptor<Iterable<ErrandProcessSignalEntity>> signalsCaptor;

	private ErrandProcessService service;

	/**
	 * Every write path checks its sender against the process configuration of the namespace, so the happy path needs both
	 * an identifier and a configured consumer. Stubbed leniently because the read paths ask for neither.
	 */
	@BeforeEach
	void setUp() {
		service = new ErrandProcessService(processRepositoryMock, activityRepositoryMock, signalRepositoryMock, accessControlServiceMock, namespaceConfigServiceMock, transactionManagerMock, CLOCK);

		Identifier.set(Identifier.create().withType(Identifier.Type.CUSTOM).withTypeString("processEngine").withValue(PROCESS_SERVICE));
		lenient().when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(PROCESS_SERVICE));
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// The sender of a report, checked against the configuration of the namespace
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * Answered as validation rather than as authorization, which is the whole reason these are 400 and not 403: nothing
	 * behind {@code X-Sent-By} is verified, so refusing with 403 would claim a check the service never made.
	 */
	@Test
	void aReportFromAServiceThatIsNotTheConsumerOfTheNamespaceIsRejected() {
		final var report = report(RUNNING).withProcessService("pw-someone-else");

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).contains("pw-someone-else").contains(PROCESS_SERVICE);
			});

		verifyNoInteractions(processRepositoryMock, accessControlServiceMock);
	}

	@Test
	void aRegistrationFromAServiceThatIsNotTheConsumerOfTheNamespaceIsRejected() {
		final var report = report(RUNNING).withProcessInstanceId(PROCESS_INSTANCE_ID).withProcessService("pw-someone-else");

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.registerProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, report))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(400));

		verifyNoInteractions(processRepositoryMock, accessControlServiceMock);
	}

	/**
	 * The rule that lets the reading side skip its lookup entirely for a namespace running no process: with no rows
	 * possible, there is nothing to read.
	 */
	@Test
	void aReportToANamespaceWithNoProcessConsumerIsRejected() {
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).contains(NAMESPACE);
			});

		verifyNoInteractions(processRepositoryMock, accessControlServiceMock);
	}

	/**
	 * Without an identifier the notification of the errand and the activity log stand without an author, which is the
	 * thing the fallback in EventService was added to prevent.
	 */
	@Test
	void aReportWithoutAnIdentifierIsRejected() {
		Identifier.remove();

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(400);
				assertThat(problem.getDetail()).contains("X-Sent-By");
			});

		verifyNoInteractions(processRepositoryMock, accessControlServiceMock, namespaceConfigServiceMock);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Reporting on an instance
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void aReportAboutAnUnknownInstanceCreatesTheRow() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING));

		assertThat(result.created()).isTrue();
		assertThat(result.process().getProcessStatus()).isEqualTo(RUNNING.name());
		assertThat(result.process().getProcessInstanceId()).isEqualTo(PROCESS_INSTANCE_ID);

		verify(processRepositoryMock).saveAndFlush(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getActiveMarker()).isTrue();
		assertThat(entityCaptor.getValue().getNamespace()).isEqualTo(NAMESPACE);
	}

	@Test
	void aReportAboutAKnownInstanceUpdatesIt() {
		final var existing = entity(PROCESS_INSTANCE_ID, RUNNING);
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(COMPLETED));

		assertThat(result.created()).isFalse();
		assertThat(result.process().getProcessStatus()).isEqualTo(COMPLETED.name());
		assertThat(existing.getActiveMarker()).isNull();
	}

	@Test
	void aReportGuardsTheErrandWithAWriteLockOnTheProcessResource() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING)));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING));

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW);
	}

	@Test
	void anInstanceIdInTheBodyDifferingFromThePathIsRejected() {
		final var report = report(RUNNING).withProcessInstanceId("someone-else");

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(400));

		verifyNoInteractions(processRepositoryMock, accessControlServiceMock);
	}

	/**
	 * The errand the instance belongs to can sit in another namespace or municipality, so the refusal does not name it.
	 */
	@Test
	void anInstanceRegisteredOnAnotherErrandIsRefusedWithoutNamingThatErrand() {
		final var existing = entity(PROCESS_INSTANCE_ID, RUNNING).withErrandId("anotherErrand");
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains(PROCESS_INSTANCE_ID, ERRAND_ID).doesNotContain("anotherErrand");
			});
	}

	@Test
	void aReportClaimingAnotherProcessThanTheRowRunsIsRefused() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING)));
		final var report = report(RUNNING).withProcessKey("alkt-tillsyn");

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));

		verify(processRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void anErrandAlreadyRunningAnotherProcessRefusesANewInstance() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(entity("failed-instance", FAILED).withProcessKey("alkt-tillsyn")));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));

		verify(processRepositoryMock, never()).saveAndFlush(any());
	}

	@Test
	void aSecondLiveInstanceIsRefused() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(entity("another-instance", RUNNING)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains("another-instance");
			});
	}

	/**
	 * The race the start permission leaves open: an instance started on an errand whose process has already run its
	 * course, whose first work step reports before the start is registered. Were the rule asked only when a start is
	 * registered, the report would create the row and the registration would find it and answer 200 - and the process
	 * engine would never learn that it has to abort the instance.
	 */
	@Test
	void aReportCreatingAnInstanceOnAnErrandWhoseProcessHasRunItsCourseIsRefused() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(entity("completed-instance", COMPLETED)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));

		verify(processRepositoryMock, never()).saveAndFlush(any());
	}

	/**
	 * A failed instance reporting itself alive again asks for the place it gave up when it ended, and the answer has to
	 * name the instance holding it rather than leaving the unique key to say only that something collided.
	 */
	@Test
	void anInstanceComingBackToLifeIsToldWhichInstanceTookItsPlace() {
		final var existing = entity(PROCESS_INSTANCE_ID, FAILED);
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(entity("took-its-place", RUNNING), existing));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains("took-its-place");
			});

		verify(processRepositoryMock, never()).saveAndFlush(any());
	}

	/**
	 * A terminal row leaves the slot of the unique key empty, so it can never take one that is occupied. Asking the
	 * question of it anyway would refuse a start that failed while an older instance is still alive.
	 */
	@Test
	void aTerminalRowIsWrittenEvenWhileAnotherInstanceLives() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(entity("still-alive", RUNNING)));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(FAILED));

		assertThat(result.created()).isTrue();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Registering a start
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void registeringAStartOfAnInstanceAlreadyReportedOnTouchesNothing() {
		final var existing = entity(PROCESS_INSTANCE_ID, WAITING).withId("rowId").withCurrentActivityId("granska");
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));
		when(signalRepositoryMock.findByErrandProcessIdOrderBySortOrderAsc("rowId")).thenReturn(List.of(signal("rowId", "granskning-godkand", "Godkänn granskning", 0)));

		final var result = service.registerProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, report(RUNNING).withProcessInstanceId(PROCESS_INSTANCE_ID)
			.withAwaitingSignals(List.of(ProcessSignal.create().withName("something-else"))));

		assertThat(result.created()).isFalse();
		assertThat(result.process().getProcessStatus()).isEqualTo(WAITING.name());
		assertThat(result.process().getCurrentActivityId()).isEqualTo("granska");
		assertThat(result.process().getAwaitingSignals()).extracting(ProcessSignal::getName).containsExactly("granskning-godkand");
		verify(processRepositoryMock, never()).saveAndFlush(any());
		verify(signalRepositoryMock, never()).saveAll(any());
		verify(signalRepositoryMock, never()).deleteAll(any());
		verifyNoInteractions(activityRepositoryMock);
	}

	@Test
	void registeringAStartOnAnErrandWithACompletedProcessIsRefused() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(entity("completed-instance", COMPLETED)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.registerProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, report(RUNNING).withProcessInstanceId(PROCESS_INSTANCE_ID)))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));
	}

	@Test
	void registeringAStartOnAnErrandWhoseOnlyProcessFailedCreatesANewOne() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(entity(null, FAILED)));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.registerProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, report(RUNNING).withProcessInstanceId(PROCESS_INSTANCE_ID));

		assertThat(result.created()).isTrue();
	}

	@Test
	void aStartThatFailedIsRegisteredWithoutAnInstance() {
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.registerProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, report(FAILED)
			.withError(ProcessError.create().withCode("START_FAILED").withMessage("boom")));

		assertThat(result.created()).isTrue();
		assertThat(result.process().getProcessInstanceId()).isNull();
		assertThat(result.process().getError().getCode()).isEqualTo("START_FAILED");
		verify(processRepositoryMock, never()).findByProcessInstanceId(anyString());
	}

	@Test
	void aStartWithoutAnInstanceThatDoesNotSayItFailedIsRejected() {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.registerProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, report(RUNNING)))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(400));

		verifyNoInteractions(processRepositoryMock, accessControlServiceMock);
	}

	@Test
	void registeringAStartGuardsTheErrandWithAWriteLockOnTheProcessResource() {
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.registerProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, report(FAILED));

		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Activities
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void aReplayedReportAddsNoActivityItAlreadyWrote() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId")));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(activityRepositoryMock.findByErrandProcessIdAndExternalTaskId("rowId", "task-1"))
			.thenReturn(List.of(ErrandProcessActivityEntity.create().withActivityId("review_phase")));

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)
			.withExternalTaskId("task-1")
			.withActivities(List.of(activity("review_phase"), activity("decision_phase"))));

		verify(activityRepositoryMock).saveAll(activitiesCaptor.capture());
		assertThat(activitiesCaptor.getValue())
			.extracting(ErrandProcessActivityEntity::getActivityId)
			.containsExactly("decision_phase");
	}

	@Test
	void twoIdenticalActivitiesInOneReportAreStoredOnce() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId")));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(activityRepositoryMock.findByErrandProcessIdAndExternalTaskId("rowId", "task-1")).thenReturn(List.of());

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)
			.withExternalTaskId("task-1")
			.withActivities(List.of(activity("review_phase"), activity("review_phase"))));

		verify(activityRepositoryMock).saveAll(activitiesCaptor.capture());
		assertThat(activitiesCaptor.getValue()).hasSize(1);
	}

	/**
	 * Null is distinct in a unique index, so the database would let both of these through. The service says the same
	 * thing, rather than inventing a stricter rule the constraint does not hold.
	 */
	@Test
	void activitiesWithoutAnExternalTaskAreAllStored() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId")));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)
			.withActivities(List.of(activity("review_phase"), activity("review_phase"))));

		verify(activityRepositoryMock).saveAll(activitiesCaptor.capture());
		assertThat(activitiesCaptor.getValue()).hasSize(2);
		verify(activityRepositoryMock, never()).findByErrandProcessIdAndExternalTaskId(anyString(), anyString());
	}

	@Test
	void aReportWithoutActivitiesAsksTheLogNothing() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING)));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING));

		verifyNoInteractions(activityRepositoryMock);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// What the process waits for from a handler
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * A signal still awaited keeps its row, since a flush inserts before it deletes and a name deleted and inserted anew
	 * in the same flush would collide with itself in the unique key.
	 */
	@Test
	void aReportReplacesWhatTheInstanceWaitsFor() {
		final var approve = signal("rowId", "granskning-godkand", "Godkänn granskning", 0);
		final var reject = signal("rowId", "granskning-avvisad", "Skicka tillbaka", 1);
		givenKnownInstance(entity(PROCESS_INSTANCE_ID, WAITING).withId("rowId"), approve, reject);

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(WAITING).withAwaitingSignals(List.of(
			ProcessSignal.create().withName("granskning-avvisad").withLabel("Skicka tillbaka för komplettering"),
			ProcessSignal.create().withName("beslut-fattat").withLabel("Beslut fattat"))));

		verify(signalRepositoryMock).deleteAll(signalsCaptor.capture());
		assertThat(signalsCaptor.getValue()).containsExactly(approve);
		verify(signalRepositoryMock).saveAll(signalsCaptor.capture());
		assertThat(signalsCaptor.getValue())
			.extracting(ErrandProcessSignalEntity::getErrandProcessId, ErrandProcessSignalEntity::getName, ErrandProcessSignalEntity::getLabel, ErrandProcessSignalEntity::getSortOrder)
			.containsExactly(
				tuple("rowId", "granskning-avvisad", "Skicka tillbaka för komplettering", 0),
				tuple("rowId", "beslut-fattat", "Beslut fattat", 1));
		assertThat(signalsCaptor.getValue()).first().isSameAs(reject);
		assertThat(result.process().getAwaitingSignals())
			.extracting(ProcessSignal::getName)
			.containsExactly("granskning-avvisad", "beslut-fattat");
	}

	/**
	 * A report describes the whole state of the process, so leaving the signals out says the same as sending none.
	 */
	@ParameterizedTest
	@NullAndEmptySource
	void aReportWithoutSignalsSaysTheProcessWaitsForNoOne(final List<ProcessSignal> awaitingSignals) {
		final var approve = signal("rowId", "granskning-godkand", "Godkänn granskning", 0);
		givenKnownInstance(entity(PROCESS_INSTANCE_ID, WAITING).withId("rowId"), approve);

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING).withAwaitingSignals(awaitingSignals));

		verify(signalRepositoryMock).deleteAll(signalsCaptor.capture());
		assertThat(signalsCaptor.getValue()).containsExactly(approve);
		verify(signalRepositoryMock).saveAll(signalsCaptor.capture());
		assertThat(signalsCaptor.getValue()).isEmpty();
		assertThat(result.process().getAwaitingSignals()).isEmpty();
	}

	/**
	 * Names are compared exactly, as the process engine compares them: a name reported twice is one signal, while names
	 * differing in case or in a trailing space are two.
	 */
	@Test
	void theSameNameReportedTwiceIsStoredOnceTheFirstKept() {
		givenKnownInstance(entity(PROCESS_INSTANCE_ID, WAITING).withId("rowId"));

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(WAITING).withAwaitingSignals(List.of(
			ProcessSignal.create().withName("granskning-godkand").withLabel("First"),
			ProcessSignal.create().withName("granskning-godkand").withLabel("Second"),
			ProcessSignal.create().withName("Granskning-Godkand"),
			ProcessSignal.create().withName("granskning-godkand "))));

		verify(signalRepositoryMock).saveAll(signalsCaptor.capture());
		assertThat(signalsCaptor.getValue())
			.extracting(ErrandProcessSignalEntity::getName, ErrandProcessSignalEntity::getLabel, ErrandProcessSignalEntity::getSortOrder)
			.containsExactly(
				tuple("granskning-godkand", "First", 0),
				tuple("Granskning-Godkand", null, 1),
				tuple("granskning-godkand ", null, 2));
	}

	@Test
	void theFirstReportAboutAnInstanceStoresWhatItWaitsForWithoutAskingWhatItWaitedForBefore() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> ((ErrandProcessEntity) invocation.getArgument(0)).withId("rowId"));
		when(signalRepositoryMock.saveAll(any())).thenAnswer(invocation -> copyOf(invocation.getArgument(0)));

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(WAITING)
			.withAwaitingSignals(List.of(ProcessSignal.create().withName("granskning-godkand").withLabel("Godkänn granskning"))));

		verify(signalRepositoryMock).saveAll(signalsCaptor.capture());
		assertThat(signalsCaptor.getValue())
			.extracting(ErrandProcessSignalEntity::getErrandProcessId, ErrandProcessSignalEntity::getName)
			.containsExactly(tuple("rowId", "granskning-godkand"));
		assertThat(result.process().getAwaitingSignals()).containsExactly(ProcessSignal.create().withName("granskning-godkand").withLabel("Godkänn granskning"));
		verify(signalRepositoryMock, never()).findByErrandProcessIdOrderBySortOrderAsc(any());
	}

	// ---------------------------------------------------------------------------------------------------------------
	// The errand moving under a work step
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * The 412 a work step that only reads the errand gets in place of the If-Match it has no way to send, and the whole
	 * reason the field exists. Neither the state nor the activities of the report are written.
	 */
	@Test
	void aReportReadAtAVersionTheErrandHasLeftBehindIsRefused() {
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW)).thenReturn(errand(8L));

		final var report = report(RUNNING)
			.withErrandVersion(7L)
			.withExternalTaskId("task-1")
			.withActivities(List.of(activity("review_phase")));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(412));

		verify(processRepositoryMock, never()).saveAndFlush(any());
		verifyNoInteractions(activityRepositoryMock);
	}

	@Test
	void aReportReadAtTheVersionTheErrandStillHasIsTaken() {
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW)).thenReturn(errand(7L));
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING)));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(WAITING).withErrandVersion(7L));

		assertThat(result.process().getProcessStatus()).isEqualTo(WAITING.name());
	}

	/**
	 * A step that neither reads nor writes the errand sends no version, and then nothing is checked at all - not even the
	 * errand it would have been checked against, which is what leaving it unstubbed here says.
	 */
	@Test
	void aReportWithoutAVersionIsHeldAgainstNothing() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING)));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING));

		assertThat(result.process().getProcessStatus()).isEqualTo(RUNNING.name());
	}

	@Test
	void aRegistrationReadAtAVersionTheErrandHasLeftBehindIsRefused() {
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, PROCESS, RW)).thenReturn(errand(8L));

		final var report = report(RUNNING).withProcessInstanceId(PROCESS_INSTANCE_ID).withErrandVersion(7L);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.registerProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, report))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(412));

		verify(processRepositoryMock, never()).saveAndFlush(any());
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Two work steps at once
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * Two branches of one instance working at the same time, which the process models are not allowed to have. The report
	 * is taken anyway: refusing it would silence the very entry that reveals the model is breaking the rule.
	 */
	@Test
	void aSecondTaskAnnouncingItselfIsLoggedAndItsReportIsStillTaken() {
		final var existing = entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId").withOutstandingExternalTaskId("task-1");
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING).withExternalTaskId("task-2"));

		assertThat(result.process().getProcessStatus()).isEqualTo(RUNNING.name());
		verify(activityRepositoryMock).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue())
			.extracting(
				ErrandProcessActivityEntity::getSeverity,
				ErrandProcessActivityEntity::getExternalTaskId,
				ErrandProcessActivityEntity::getActivityId,
				ErrandProcessActivityEntity::getErrorCode,
				ErrandProcessActivityEntity::getOccurredAt)
			.containsExactly(WARN, "task-2", null, "CONCURRENT_EXTERNAL_TASKS", OffsetDateTime.now(CLOCK));

		// Both tasks named, and what to do about them. Whoever reads the entry is looking at an errand, while the fix
		// is in a BPMN file they cannot reach from there.
		assertThat(activityCaptor.getValue().getMessage())
			.startsWith("concurrent external tasks detected")
			.contains("task 'task-2' reported RUNNING while task 'task-1' was still working")
			.contains("take the parallel gateway out of the model");
		assertThat(existing.getOutstandingExternalTaskId()).isEqualTo("task-2");
	}

	/**
	 * Logged once per instance. The two branches keep passing each other for as long as the model has the gateway,
	 * and the log this would otherwise fill is the one a handler reads on the errand.
	 */
	@Test
	void aFurtherCollisionOnTheSameInstanceIsNotLoggedAgain() {
		final var existing = entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId").withOutstandingExternalTaskId("task-1");
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(activityRepositoryMock.existsByErrandProcessIdAndActivityType("rowId", "CONCURRENCY")).thenReturn(true);

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING).withExternalTaskId("task-2"));

		verify(activityRepositoryMock, never()).save(any());
	}

	/**
	 * Steps running one after another never meet, and that is what keeps the warning from firing on every process there
	 * is: the report a step hands in when it is done empties the place before the next task announces itself.
	 */
	@Test
	void tasksTakingTurnsRaiseNoWarning() {
		final var existing = entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId");
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING).withExternalTaskId("task-1"));
		assertThat(existing.getOutstandingExternalTaskId()).isEqualTo("task-1");

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING).withExternalTaskId("task-1"));
		assertThat(existing.getOutstandingExternalTaskId()).isNull();

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING).withExternalTaskId("task-2"));
		assertThat(existing.getOutstandingExternalTaskId()).isEqualTo("task-2");

		verify(activityRepositoryMock, never()).save(any());
	}

	/**
	 * The report a step hands in when it is done empties the place even when it does not say RUNNING, which is what a
	 * step that leaves the process waiting reports. Without that the next task to announce itself would be taken for a
	 * parallel branch, and every process with a wait state would raise the warning.
	 */
	@Test
	void aClosingReportFromTheWorkingTaskEmptiesThePlace() {
		final var existing = entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId").withOutstandingExternalTaskId("task-1");
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(WAITING).withExternalTaskId("task-1"));
		assertThat(existing.getOutstandingExternalTaskId()).isNull();

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING).withExternalTaskId("task-2"));

		assertThat(existing.getOutstandingExternalTaskId()).isEqualTo("task-2");
		verify(activityRepositoryMock, never()).save(any());
	}

	/**
	 * Only the task standing in the place empties it. A closing report from another task says nothing about the one still
	 * working, and emptying the place on it would hide the overlap with the next task to announce itself.
	 */
	@Test
	void aClosingReportFromAnotherTaskLeavesTheWorkingOneWhereItIs() {
		final var existing = entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId").withOutstandingExternalTaskId("task-1");
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RETRYING).withExternalTaskId("task-3"));

		assertThat(existing.getOutstandingExternalTaskId()).isEqualTo("task-1");
		verify(activityRepositoryMock, never()).save(any());
	}

	/**
	 * A report naming no task - the registration of a start, among others - says nothing about the task standing on the
	 * row, and must not be read as that task having finished.
	 */
	@Test
	void aReportNamingNoTaskLeavesTheOutstandingOneWhereItIs() {
		final var existing = entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId").withOutstandingExternalTaskId("task-1");
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING));

		assertThat(existing.getOutstandingExternalTaskId()).isEqualTo("task-1");
	}

	@Test
	void theFirstReportAboutAnInstanceRecordsTheTaskThatMadeIt() {
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING).withExternalTaskId("task-1"));

		verify(processRepositoryMock).saveAndFlush(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getOutstandingExternalTaskId()).isEqualTo("task-1");
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Losing the race
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void aUniqueKeyCollisionIsResolvedByReadingTheRowTheWinnerWrote() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID))
			.thenReturn(Optional.empty())
			.thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING)));
		when(processRepositoryMock.saveAndFlush(any()))
			.thenThrow(new DataIntegrityViolationException("uq_ep_process_instance_id"))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(COMPLETED));

		assertThat(result.created()).isFalse();
		assertThat(result.process().getProcessStatus()).isEqualTo(COMPLETED.name());
		verify(processRepositoryMock, times(2)).findByProcessInstanceId(PROCESS_INSTANCE_ID);
	}

	/**
	 * A violation that survives the second attempt is not contention: contention would have been seen by the checks the
	 * attempt begins with and refused as the conflict it is, named. Dressing it up as 409 would tell a process engine
	 * that this errand already had its process, and it would abort the one it just started.
	 */
	@Test
	void aViolationThatSurvivesTheSecondAttemptIsRaisedAsItIs() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Column 'activity_type' cannot be null"));

		final var report = report(RUNNING);

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report));
	}

	/**
	 * The report of a work step on a row that already exists is the case a lookup after the fact cannot judge: the row
	 * and the live slot of the errand were both taken by this very report before it wrote anything, so asking the
	 * database whether they are occupied answers yes whatever the true cause was. Only the second attempt can tell the
	 * two apart, and this is what keeps a broken activity from being reported as a race that never happened.
	 */
	@Test
	void aViolationOnTheUpdatePathIsNotMistakenForContention() {
		final var existing = entity(PROCESS_INSTANCE_ID, RUNNING);
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(activityRepositoryMock.saveAll(any())).thenThrow(new DataIntegrityViolationException("uq_epa_idempotency"));

		final var report = report(RUNNING).withActivities(List.of(activity("granska")));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report));
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Reading
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void readingTheProcessesAnswersWithTheEnvelope() {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID))
			.thenReturn(List.of(entity("newest", WAITING).withId("row-newest"), entity("oldest", COMPLETED).withId("row-oldest")));
		when(signalRepositoryMock.findByErrandProcessIdInOrderBySortOrderAsc(List.of("row-newest", "row-oldest")))
			.thenReturn(List.of(signal("row-newest", "granskning-godkand", "Godkänn granskning", 0)));

		final var processes = service.readProcesses(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		assertThat(processes.getProcesses())
			.extracting(ErrandProcess::getProcessInstanceId, process -> process.getAwaitingSignals().stream().map(ProcessSignal::getName).toList())
			.containsExactly(
				tuple("newest", List.of("granskning-godkand")),
				tuple("oldest", List.of()));
		assertThat(processes.getStartable()).isNull();
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS, R);
	}

	@Test
	void readingTheProcessesOfAnErrandThatNeverHadOneAsksForNoSignals() {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of());

		assertThat(service.readProcesses(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID).getProcesses()).isEmpty();
		verifyNoInteractions(signalRepositoryMock);
	}

	@Test
	void readingTheLogWithoutAFilterReturnsEntriesWithAndWithoutAnInstance() {
		final var pageable = PageRequest.of(0, 50);
		final var withInstance = ErrandProcessActivityEntity.create().withId("a").withErrandProcessId("rowId").withOccurredAt(now(systemDefault()));
		final var withoutInstance = ErrandProcessActivityEntity.create().withId("b").withOccurredAt(now(systemDefault()));
		when(activityRepositoryMock.findByErrandId(ERRAND_ID, pageable)).thenReturn(new PageImpl<>(List.of(withInstance, withoutInstance), pageable, 2));
		when(processRepositoryMock.findAllById(any())).thenReturn(List.of(entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId")));

		final var page = service.readProcessActivities(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, pageable);

		assertThat(page.getContent())
			.extracting(ProcessActivity::getId, ProcessActivity::getProcessInstanceId)
			.containsExactly(
				tuple("a", PROCESS_INSTANCE_ID),
				tuple("b", null));
		assertThat(page.getTotalElements()).isEqualTo(2);
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_ACTIVITY, R);
	}

	@Test
	void narrowingTheLogToAnInstanceLeavesTheInstancelessEntriesOut() {
		final var pageable = PageRequest.of(0, 50);
		when(processRepositoryMock.findByProcessInstanceIdAndErrandId(PROCESS_INSTANCE_ID, ERRAND_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId")));
		when(activityRepositoryMock.findByErrandIdAndErrandProcessId(ERRAND_ID, "rowId", pageable))
			.thenReturn(new PageImpl<>(List.of(ErrandProcessActivityEntity.create().withId("a").withErrandProcessId("rowId").withOccurredAt(now(systemDefault()))), pageable, 1));

		final var page = service.readProcessActivities(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, pageable);

		assertThat(page.getContent()).extracting(ProcessActivity::getProcessInstanceId).containsExactly(PROCESS_INSTANCE_ID);
		verify(activityRepositoryMock, never()).findByErrandId(anyString(), any());
	}

	@Test
	void narrowingTheLogToAnInstanceTheErrandNeverHadReturnsNothing() {
		final var pageable = PageRequest.of(0, 50);
		when(processRepositoryMock.findByProcessInstanceIdAndErrandId("unknown", ERRAND_ID)).thenReturn(Optional.empty());

		final var page = service.readProcessActivities(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "unknown", pageable);

		assertThat(page).isEmpty();
		verifyNoInteractions(activityRepositoryMock);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Enrichment of the errand
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * The signals are asked for once for the page, and only for the processes the errands show - the older rows of an
	 * errand are thrown away before the question is put.
	 */
	@Test
	void theLatestProcessPerErrandAndWhatItWaitsForAreReadInOneQueryEach() {
		when(processRepositoryMock.findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(List.of("errand-1", "errand-2"), MUNICIPALITY_ID, NAMESPACE)).thenReturn(List.of(
			entity("newest-1", FAILED).withId("row-newest-1").withErrandId("errand-1"),
			entity("older-1", COMPLETED).withId("row-older-1").withErrandId("errand-1"),
			entity("newest-2", WAITING).withId("row-newest-2").withErrandId("errand-2")));
		when(signalRepositoryMock.findByErrandProcessIdInOrderBySortOrderAsc(List.of("row-newest-1", "row-newest-2"))).thenReturn(List.of(
			signal("row-newest-2", "granskning-godkand", "Godkänn granskning", 0),
			signal("row-newest-2", "granskning-avvisad", "Skicka tillbaka", 1)));

		final var processes = service.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, List.of("errand-1", "errand-2"));

		assertThat(processes).hasSize(2);
		assertThat(processes.get("errand-1").getProcessInstanceId()).isEqualTo("newest-1");
		assertThat(processes.get("errand-1").getAwaitingSignals()).isEmpty();
		assertThat(processes.get("errand-2").getProcessInstanceId()).isEqualTo("newest-2");
		assertThat(processes.get("errand-2").getAwaitingSignals()).extracting(ProcessSignal::getName).containsExactly("granskning-godkand", "granskning-avvisad");
		verify(processRepositoryMock, times(1)).findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(any(), anyString(), anyString());
		verify(signalRepositoryMock, times(1)).findByErrandProcessIdInOrderBySortOrderAsc(any());
		verifyNoMoreInteractions(signalRepositoryMock);
	}

	@Test
	void aPageWithoutASingleProcessAsksForNoSignals() {
		when(processRepositoryMock.findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(List.of("errand-1"), MUNICIPALITY_ID, NAMESPACE)).thenReturn(List.of());

		assertThat(service.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, List.of("errand-1"))).isEmpty();
		verifyNoInteractions(signalRepositoryMock);
	}

	@Test
	void noErrandsAsksTheDatabaseNothing() {
		assertThat(service.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, List.of())).isEmpty();
		assertThat(service.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, null)).isEmpty();
		verifyNoInteractions(processRepositoryMock, signalRepositoryMock);
	}

	// ---------------------------------------------------------------------------------------------------------------

	private static ErrandProcess report(final ProcessStatus status) {
		return ErrandProcess.create()
			.withProcessService(PROCESS_SERVICE)
			.withProcessKey(PROCESS_KEY)
			.withProcessStatus(status);
	}

	private static ErrandEntity errand(final long version) {
		return ErrandEntity.create().withId(ERRAND_ID).withVersion(version);
	}

	private static ProcessActivity activity(final String activityId) {
		return ProcessActivity.create()
			.withActivityType("PHASE")
			.withActivityId(activityId)
			.withOccurredAt(now(systemDefault()));
	}

	/**
	 * An instance the service already knows, waiting for what is sent in. What is saved is handed back as it was sent,
	 * which is what the repository does for rows that are new or already managed.
	 */
	private void givenKnownInstance(final ErrandProcessEntity instance, final ErrandProcessSignalEntity... stored) {
		when(processRepositoryMock.findByProcessInstanceId(instance.getProcessInstanceId())).thenReturn(Optional.of(instance));
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(signalRepositoryMock.findByErrandProcessIdOrderBySortOrderAsc(instance.getId())).thenReturn(List.of(stored));
		when(signalRepositoryMock.saveAll(any())).thenAnswer(invocation -> copyOf(invocation.getArgument(0)));
	}

	private static List<ErrandProcessSignalEntity> copyOf(final Iterable<ErrandProcessSignalEntity> signals) {
		return StreamSupport.stream(signals.spliterator(), false).toList();
	}

	private static ErrandProcessSignalEntity signal(final String errandProcessId, final String name, final String label, final int sortOrder) {
		return ErrandProcessSignalEntity.create()
			.withId(errandProcessId + "-" + name)
			.withErrandProcessId(errandProcessId)
			.withName(name)
			.withLabel(label)
			.withSortOrder(sortOrder);
	}

	private static ErrandProcessEntity entity(final String processInstanceId, final ProcessStatus status) {
		final var entity = ErrandProcessEntity.create()
			.withErrandId(ERRAND_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withProcessService(PROCESS_SERVICE)
			.withProcessKey(PROCESS_KEY)
			.withProcessInstanceId(processInstanceId);
		entity.applyStatus(status, CLOCK);
		return entity;
	}
}
