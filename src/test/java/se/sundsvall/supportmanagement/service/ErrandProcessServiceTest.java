package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.api.model.process.ProcessError;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
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
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T10:15:30.000Z"), ZoneId.of("UTC"));

	@Mock
	private ErrandProcessRepository processRepositoryMock;

	@Mock
	private ErrandProcessActivityRepository activityRepositoryMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private PlatformTransactionManager transactionManagerMock;

	@Captor
	private ArgumentCaptor<ErrandProcessEntity> entityCaptor;

	@Captor
	private ArgumentCaptor<List<ErrandProcessActivityEntity>> activitiesCaptor;

	private ErrandProcessService service;

	@BeforeEach
	void setUp() {
		service = new ErrandProcessService(processRepositoryMock, activityRepositoryMock, accessControlServiceMock, transactionManagerMock, CLOCK);
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
		assertThat(result.process().getProcessStatus()).isEqualTo(RUNNING);
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
		assertThat(result.process().getProcessStatus()).isEqualTo(COMPLETED);
		assertThat(existing.getActiveMarker()).isNull();
		verify(processRepositoryMock, never()).existsByErrandIdAndProcessKeyNot(anyString(), anyString());
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

	@Test
	void anInstanceRegisteredOnAnotherErrandIsRefused() {
		final var existing = entity(PROCESS_INSTANCE_ID, RUNNING).withErrandId("anotherErrand");
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));
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
		when(processRepositoryMock.existsByErrandIdAndProcessKeyNot(ERRAND_ID, PROCESS_KEY)).thenReturn(true);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));
	}

	@Test
	void aSecondLiveInstanceIsRefused() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.findByErrandIdAndActiveMarkerIsNotNull(ERRAND_ID)).thenReturn(Optional.of(entity("another-instance", RUNNING)));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains("another-instance");
			});
	}

	/**
	 * A failed instance reporting itself alive again asks for the place it gave up when it ended, and the answer has to
	 * name the instance holding it rather than leaving the unique key to say only that something collided.
	 */
	@Test
	void anInstanceComingBackToLifeIsToldWhichInstanceTookItsPlace() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, FAILED)));
		when(processRepositoryMock.findByErrandIdAndActiveMarkerIsNotNull(ERRAND_ID)).thenReturn(Optional.of(entity("took-its-place", RUNNING)));

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
		when(processRepositoryMock.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(FAILED));

		assertThat(result.created()).isTrue();
		verify(processRepositoryMock, never()).findByErrandIdAndActiveMarkerIsNotNull(anyString());
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Registering a start
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void registeringAStartOfAnInstanceAlreadyReportedOnTouchesNothing() {
		final var existing = entity(PROCESS_INSTANCE_ID, WAITING).withCurrentActivityId("granska");
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(existing));

		final var result = service.registerProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, report(RUNNING).withProcessInstanceId(PROCESS_INSTANCE_ID));

		assertThat(result.created()).isFalse();
		assertThat(result.process().getProcessStatus()).isEqualTo(WAITING);
		assertThat(result.process().getCurrentActivityId()).isEqualTo("granska");
		verify(processRepositoryMock, never()).saveAndFlush(any());
		verifyNoInteractions(activityRepositoryMock);
	}

	@Test
	void registeringAStartOnAnErrandWithACompletedProcessIsRefused() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.existsByErrandIdAndProcessStatus(ERRAND_ID, COMPLETED)).thenReturn(true);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.registerProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, report(RUNNING).withProcessInstanceId(PROCESS_INSTANCE_ID)))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));
	}

	@Test
	void registeringAStartOnAnErrandWhoseOnlyProcessFailedCreatesANewOne() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.existsByErrandIdAndProcessStatus(ERRAND_ID, COMPLETED)).thenReturn(false);
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
		assertThat(result.process().getProcessStatus()).isEqualTo(COMPLETED);
		verify(processRepositoryMock, times(2)).findByProcessInstanceId(PROCESS_INSTANCE_ID);
	}

	/**
	 * The live slot of the errand taken by an instance of another process is a race the second attempt can still lose,
	 * and only then is contention the honest answer.
	 */
	@Test
	void aCollisionThatSurvivesTheSecondAttemptIsAnsweredAsAConflict() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.findByErrandIdAndActiveMarkerIsNotNull(ERRAND_ID))
			.thenReturn(Optional.empty())
			.thenReturn(Optional.empty())
			.thenReturn(Optional.of(entity("took-the-slot", RUNNING)));
		when(processRepositoryMock.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_ep_one_active_per_errand"));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)))
			.satisfies(problem -> assertThat(problem.getStatus().value()).isEqualTo(409));
	}

	/**
	 * A violation no other row explains is not contention, and must not be dressed up as it: a process engine reads 409
	 * as "this errand already had its process" and aborts the one it just started.
	 */
	@Test
	void aViolationNoConcurrentRowExplainsIsRaisedAsItIs() {
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.findByErrandIdAndActiveMarkerIsNotNull(ERRAND_ID)).thenReturn(Optional.empty());
		when(processRepositoryMock.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Column 'activity_type' cannot be null"));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> service.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, report(RUNNING)));
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Reading
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void readingTheProcessesAnswersWithTheEnvelope() {
		when(processRepositoryMock.findByErrandIdAndMunicipalityIdAndNamespaceOrderByCreatedDesc(ERRAND_ID, MUNICIPALITY_ID, NAMESPACE, Pageable.unpaged()))
			.thenReturn(List.of(entity("newest", RUNNING), entity("oldest", COMPLETED)));

		final var processes = service.readProcesses(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		assertThat(processes.getProcesses())
			.extracting(ErrandProcess::getProcessInstanceId)
			.containsExactly("newest", "oldest");
		assertThat(processes.getStartable()).isNull();
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS, R);
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
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_ACTIVITY, R);
	}

	@Test
	void narrowingTheLogToAnInstanceLeavesTheInstancelessEntriesOut() {
		final var pageable = PageRequest.of(0, 50);
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING).withId("rowId")));
		when(activityRepositoryMock.findByErrandIdAndErrandProcessId(ERRAND_ID, "rowId", pageable))
			.thenReturn(new PageImpl<>(List.of(ErrandProcessActivityEntity.create().withId("a").withErrandProcessId("rowId").withOccurredAt(now(systemDefault()))), pageable, 1));

		final var page = service.readProcessActivities(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, pageable);

		assertThat(page.getContent()).extracting(ProcessActivity::getProcessInstanceId).containsExactly(PROCESS_INSTANCE_ID);
		verify(activityRepositoryMock, never()).findByErrandId(anyString(), any());
	}

	@Test
	void narrowingTheLogToAnInstanceTheErrandNeverHadReturnsNothing() {
		final var pageable = PageRequest.of(0, 50);
		when(processRepositoryMock.findByProcessInstanceId("unknown")).thenReturn(Optional.empty());

		final var page = service.readProcessActivities(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "unknown", pageable);

		assertThat(page).isEmpty();
		verifyNoInteractions(activityRepositoryMock);
	}

	@Test
	void narrowingTheLogToAnInstanceOfAnotherErrandReturnsNothing() {
		final var pageable = PageRequest.of(0, 50);
		when(processRepositoryMock.findByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(Optional.of(entity(PROCESS_INSTANCE_ID, RUNNING).withErrandId("anotherErrand")));

		assertThat(service.readProcessActivities(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PROCESS_INSTANCE_ID, pageable)).isEmpty();
		verifyNoInteractions(activityRepositoryMock);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Enrichment of the errand
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void theLatestProcessPerErrandIsReadInOneQuery() {
		when(processRepositoryMock.findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(List.of("errand-1", "errand-2"), MUNICIPALITY_ID, NAMESPACE)).thenReturn(List.of(
			entity("newest-1", FAILED).withErrandId("errand-1"),
			entity("older-1", COMPLETED).withErrandId("errand-1"),
			entity("newest-2", RUNNING).withErrandId("errand-2")));

		final var processes = service.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, List.of("errand-1", "errand-2"));

		assertThat(processes).hasSize(2);
		assertThat(processes.get("errand-1").getProcessInstanceId()).isEqualTo("newest-1");
		assertThat(processes.get("errand-2").getProcessInstanceId()).isEqualTo("newest-2");
		verify(processRepositoryMock, times(1)).findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(any(), anyString(), anyString());
	}

	@Test
	void noErrandsAsksTheDatabaseNothing() {
		assertThat(service.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, List.of())).isEmpty();
		assertThat(service.findLatestProcesses(NAMESPACE, MUNICIPALITY_ID, null)).isEmpty();
		verifyNoInteractions(processRepositoryMock);
	}

	// ---------------------------------------------------------------------------------------------------------------

	private static ErrandProcess report(final ProcessStatus status) {
		return ErrandProcess.create()
			.withProcessService("pw-alkt")
			.withProcessKey(PROCESS_KEY)
			.withProcessStatus(status);
	}

	private static ProcessActivity activity(final String activityId) {
		return ProcessActivity.create()
			.withActivityType("PHASE")
			.withActivityId(activityId)
			.withOccurredAt(now(systemDefault()));
	}

	private static ErrandProcessEntity entity(final String processInstanceId, final ProcessStatus status) {
		final var entity = ErrandProcessEntity.create()
			.withErrandId(ERRAND_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withProcessService("pw-alkt")
			.withProcessKey(PROCESS_KEY)
			.withProcessInstanceId(processInstanceId);
		entity.applyStatus(status, CLOCK);
		return entity;
	}
}
